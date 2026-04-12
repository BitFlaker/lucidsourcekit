package com.bitflaker.lucidsourcekit.utils.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.graphics.withSave
import androidx.core.view.children
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bitflaker.lucidsourcekit.utils.pdf.layout.DocumentLayout
import com.bitflaker.lucidsourcekit.utils.pdf.layout.PdfPageDimensions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import kotlin.math.max
import kotlin.math.roundToInt

private const val MM_TO_INCH_PDF = 1 / 25.4 * 72

/**
 * @param width The width of the page in mm
 * @param height The height of the page in mm
 */
class PdfGenerator(val width: Int, val height: Int) {
    companion object {
        private val variablePattern = Regex("\\{[A-Z0-9_]+\\}")

        val A0: Builder get() = Builder(PdfPageDimensions.A0.width, PdfPageDimensions.A0.height)
        val A1: Builder get() = Builder(PdfPageDimensions.A1.width, PdfPageDimensions.A1.height)
        val A2: Builder get() = Builder(PdfPageDimensions.A2.width, PdfPageDimensions.A2.height)
        val A3: Builder get() = Builder(PdfPageDimensions.A3.width, PdfPageDimensions.A3.height)
        val A4: Builder get() = Builder(PdfPageDimensions.A4.width, PdfPageDimensions.A4.height)
        val A5: Builder get() = Builder(PdfPageDimensions.A5.width, PdfPageDimensions.A5.height)
        val A6: Builder get() = Builder(PdfPageDimensions.A6.width, PdfPageDimensions.A6.height)
        val A7: Builder get() = Builder(PdfPageDimensions.A7.width, PdfPageDimensions.A7.height)
        val A8: Builder get() = Builder(PdfPageDimensions.A8.width, PdfPageDimensions.A8.height)
        val A9: Builder get() = Builder(PdfPageDimensions.A9.width, PdfPageDimensions.A9.height)
        val A10: Builder get() = Builder(PdfPageDimensions.A10.width, PdfPageDimensions.A10.height)

    }

    val overlays = Overlays()
    var contents: MutableList<ViewGroup> = mutableListOf()
    val pageWidth = (width * MM_TO_INCH_PDF).roundToInt()
    val pageHeight = (height * MM_TO_INCH_PDF).roundToInt()
    var layout = DocumentLayout(pageWidth, pageHeight)

    private val expandedTemplates = HashMap<Int, String>()
    private val dateFormatter: DateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)
    private val timeFormatter: DateFormat = DateFormat.getTimeInstance(DateFormat.SHORT)
    private var printDate: Date = Date()

    /**
     * The views provided in the `contents` and `overlays` are expected to have the dimensions of this generator
     * (e.g. for A4 a width of `595px` and a height of `842px`). The contents will be fully drawn and page breaks will be
     * inserted automatically.
     *
     * @param file The destination file to store the generated PDF at
     */
    suspend fun generate(file: File) {
        val pdfDocument = generateDocument()

        // Save the file
        file.outputStream().use {
            pdfDocument.writeTo(it)
        }

        pdfDocument.close()
    }

    /**
     * The views provided in the `contents` and `overlays` are expected to have the dimensions of this generator
     * (e.g. for A4 a width of `595px` and a height of `842px`). The contents will be fully drawn and page breaks will be
     * inserted automatically.
     *
     * @param stream The output stream to write the PDF data to. The stream will be closed automatically
     */
    suspend fun generate(stream: OutputStream) {
        val pdfDocument = generateDocument()

        // Save the file
        stream.use {
            pdfDocument.writeTo(it)
        }

        pdfDocument.close()
    }

    suspend fun generatePreview(
        context: Context,
        pageNumber: Int,
        scale: Float = 1f
    ): Bitmap = withContext(Dispatchers.IO) {
        val pdfDocument = generateDocument()

        val tempFile = File.createTempFile("preview_", ".pdf", context.cacheDir)
        tempFile.outputStream().use(pdfDocument::writeTo)

        val fileDescriptor = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(fileDescriptor)

        val page = renderer.openPage(pageNumber - 1)
        val bitmap = createBitmap(page.width, page.height)

        bitmap.eraseColor(Color.WHITE)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

        if (scale != 1f) {
            val scaledWidth = (page.width * scale).roundToInt()
            val scaledHeight = (page.height * scale).roundToInt()
            bitmap.scale(scaledWidth, scaledHeight)
        }

        page.close()
        renderer.close()
        fileDescriptor.close()
        pdfDocument.close()

        tempFile.delete()

        bitmap
    }

    suspend fun generateDocument(): PdfDocument {
        if (contents.isEmpty()) throw IllegalArgumentException("The content array has to have at least 1 item")
        if (contents.any { !layout.isVerticalSplittableViewGroup(it) }) throw IllegalArgumentException("The content array must only contain vertical wrappable ViewGroups")

        printDate = Calendar.getInstance().time

        // Measure and layout the content layers
        withContext(Dispatchers.Main) {
            for (content in contents) {
                layout.measureLayout(content)
            }
        }

        // Clear previously cached expanded views
        expandedTemplates.clear()

        // Calculate the draw sizes and page count for drawing the full content
        var pageCount = 0
        val pages = mutableListOf<PageDefinition>()
        val contentPageCount = hashMapOf<Int, Int>()
        for (i in contents.indices) {
            val content = contents[i]
            val maxHeight = pageHeight - (content.paddingTop + content.paddingBottom)
            val totalHeight = content.measuredHeight

            // Try to split the fully inflated content view at suitable heights to best
            // fit it on PDF pages and create Page definitions for them
            val offset = ChildOffset()
            var heightOffset = content.paddingTop
            while (heightOffset < totalHeight) {
                val layoutPageNumber = (contentPageCount[i] ?: 0) + 1
                val splitHeight = getSize(offset, content, maxHeight) ?: (totalHeight - heightOffset)

                // Create and add page definition
                pages.add(
                    PageDefinition(
                        i,
                        heightOffset,
                        content.paddingTop,
                        splitHeight,
                        layoutPageNumber
                    )
                )

                contentPageCount[i] = layoutPageNumber
                heightOffset += splitHeight
                pageCount++
            }
        }

        // Create the PDF document
        val pdfDocument = PdfDocument()
        for (i in pages.indices) {
            val pageDef = pages[i]
            val content = contents[pageDef.layoutIndex]
            val pageContext = PageContext(
                i + 1,
                pageDef.layoutIndex,
                pageDef.layoutPageNumber
            )

            // Create a new page
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageContext.pageNumber).create()
            val page = pdfDocument.startPage(pageInfo)

            // Draw the current page content
            page.canvas.withSave {
                clipRect(0, pageDef.pageOffset, pageInfo.pageWidth, pageDef.height + pageDef.pageOffset)
                translate(0f, (pageDef.pageOffset - pageDef.offset).toFloat())
                content.draw(page.canvas)
            }

            // Expand all variables in all visible overlays and draw them to the canvas
            for (overlay in overlays.filter {
                it.displayCondition(pageContext)
            }) {
                expandPrintVariables(overlay.view, pageContext.pageNumber, pages.size)
                overlay.view.measure(
                    View.MeasureSpec.makeMeasureSpec(page.canvas.width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(page.canvas.height, View.MeasureSpec.EXACTLY)
                )
                overlay.view.layout(0, 0, page.canvas.width, page.canvas.height)
                overlay.view.layout(0, 0, page.canvas.width, page.canvas.height)
                overlay.view.draw(page.canvas)
            }

            // Finish the current page
            pdfDocument.finishPage(page)
        }

        return pdfDocument
    }

    private fun expandPrintVariables(view: View, currentPage: Int, totalPages: Int) {
        if (view is ViewGroup) {
            for (child in view.children) {
                expandPrintVariables(child, currentPage, totalPages)
            }
        }
        else if (view is TextView) {
            // Check if the text contains variables and if so, register the template
            if (containsVariables(view.text)) {
                expandedTemplates[view.id] = view.text.toString()
            }

            // If a template is found, expand it and set the text
            val template = expandedTemplates[view.id]
            if (template != null) {
                view.text = template
                    .replace("{CURRENT_PAGE}", currentPage.toString())
                    .replace("{TOTAL_PAGES}", totalPages.toString())
                    .replace("{PRINT_DATE}", dateFormatter.format(printDate))
                    .replace("{PRINT_TIME}", timeFormatter.format(printDate))
            }
        }
    }

    private fun containsVariables(value: CharSequence?): Boolean {
        return !value.isNullOrEmpty() && value.contains(variablePattern)
    }

    private fun getSize(
        offset: ChildOffset,
        content: ViewGroup,
        maxHeight: Int,
        isEmptyPage: Boolean = true
    ): Int? {
        var heightCounter = 0
        var isCutOff = false

        // Traverse all child views and get the best suitable height to try and prevent cutting off views
        for (i in offset.index until content.childCount) {
            val child = content.getChildAt(i)

            // Get the full height of the view
            if (child is ViewGroup) {
                val innerOffset = offset.inner ?: ChildOffset()
                val innerMaxHeight = maxHeight - heightCounter - child.paddingTop - child.paddingBottom
                val innerIsEmptyPage = offset.index == i && isEmptyPage

                if (layout.isVerticalSplittableViewGroup(child, innerMaxHeight)) {
                    val height = getSize(innerOffset, child, innerMaxHeight, innerIsEmptyPage)

                    // In case the ViewGroup only fits partially into the page,
                    // draw the fitting content and finish the current page
                    if (height != null) {
                        val paddingDiff = max(0, child.paddingTop - offset.heightOffset)
                        heightCounter += height + paddingDiff
                        offset.heightOffset += height + paddingDiff
                        offset.inner = innerOffset
                        offset.index = i
                        isCutOff = true
                        break
                    }
                }
                else if (layout.isGridSplittableViewGroup(child)) {
                    // Handle RecyclerView with vertical GridLayoutManager
                    if (child is RecyclerView && child.layoutManager is GridLayoutManager) {
                        val manager = child.layoutManager as GridLayoutManager

                        var itemCount = 0
                        var isGridCutOff = false

                        var currentHeight = 0
                        while (innerOffset.index + itemCount < manager.itemCount) {
                            // Get the amount of items in the current row
                            var itemsInRow = 0
                            var spanCounter = 0
                            while (spanCounter < manager.spanCount && innerOffset.index + itemCount + itemsInRow < manager.itemCount) {
                                spanCounter += manager.spanSizeLookup.getSpanSize(innerOffset.index + itemCount + itemsInRow)
                                itemsInRow++
                            }

                            // Get the height of the first cell (all cells in the row have the same height)
                            val cells = child.children.drop(innerOffset.index + itemCount).take(itemsInRow).toList()
                            val rowHeight = cells.firstOrNull()?.height ?: 0

                            // Check if the row would exceed the current max content height
                            val addedHeight = rowHeight - innerOffset.heightOffset
                            if (currentHeight + addedHeight > innerMaxHeight) {

                                // Try to prevent an infinite loop by forcing to split the row when
                                // the row would not fit within a full blank page
                                if (itemCount == 0 && isEmptyPage) {
                                    val remainingSpace = innerMaxHeight - currentHeight + innerOffset.heightOffset
                                    val sliceHeights = IntArray(itemsInRow) {
                                        tryGetSliceHeight(true, remainingSpace, cells[it])
                                    }

                                    // Try to find the best split height. This works well for single cell rows.
                                    // For multiple cells, this will take the split height of the first cell
                                    // and slice the other cells at that height as well. This might cause views
                                    // to be sliced at an undesirable position for cell index != 0
                                    val sliceHeight = sliceHeights[0] - innerOffset.heightOffset
                                    currentHeight += sliceHeight
                                    innerOffset.heightOffset += sliceHeight
                                }

                                isGridCutOff = true
                                break
                            }

                            innerOffset.heightOffset = 0
                            currentHeight += addedHeight
                            itemCount += itemsInRow
                        }

                        // In case the ViewGroup only fits partially into the page,
                        // draw the fitting content and finish the current page
                        if (isGridCutOff) {
                            innerOffset.index += itemCount
                            heightCounter += currentHeight
                            offset.heightOffset += currentHeight
                            offset.inner = innerOffset
                            offset.index = i
                            isCutOff = true
                            break
                        }
                    }
                }

                // The ViewGroup fits the available space entirely or is a ViewGroup that cannot be split
                offset.inner = null
            }

            // Check if the view fits into the given page
            val addedHeight = child.height - offset.heightOffset
            if (heightCounter + addedHeight > maxHeight) {
                val isEmptyPage = offset.index == i && isEmptyPage
                val remainingSpace = maxHeight - heightCounter + offset.heightOffset
                val slicedHeight = tryGetSliceHeight(isEmptyPage, remainingSpace, child) - offset.heightOffset
                heightCounter += slicedHeight

                offset.heightOffset += slicedHeight
                offset.index = i
                isCutOff = true
                break
            }

            // Increase the currently used page space counter
            offset.heightOffset = 0
            heightCounter += addedHeight
        }

        // Return the amount of the view drawn or null if it fit entirely on the available space
        return if (isCutOff) heightCounter else null
    }

    private fun tryGetSliceHeight(isEmptyPage: Boolean, remainingSpace: Int, child: View): Int {
        // Try to fit content that would be cut off on to the current page to prevent large
        // empty spaces from being created. In case this is a full blank page, draw part
        // of the anyway as it would never fit otherwise
        if (isEmptyPage || (remainingSpace >= layout.maxEmptySpaceThreshold && child.tag != "no-break" && !isNonSplittableView(child))) {
            return getBestViewCutHeight(child, remainingSpace)
        }

        return 0
    }

    private fun getBestViewCutHeight(child: View, maxHeight: Int): Int {
        if (child is ViewGroup && layout.isVerticalSplittableViewGroup(child, maxHeight)) {
            val sliced = getSlicedView(child, maxHeight) ?: return maxHeight
            return sliced.second + sliceView(sliced.first, maxHeight - sliced.second)
        }
        return sliceView(child, maxHeight)
    }

    private fun sliceView(view: View, maxHeight: Int): Int = if (view is TextView) {
        val textInset = view.measuredHeight - view.paddingTop - view.lineCount * view.lineHeight
        ((maxHeight - view.paddingTop - textInset) / view.lineHeight) * view.lineHeight + view.paddingTop + textInset
    } else {
        maxHeight
    }

    private fun getSlicedView(container: ViewGroup, maxHeight: Int): Pair<View, Int>? {
        var offset = 0
        for (child in container.children) {
            if (child is ViewGroup && layout.isVerticalSplittableViewGroup(child, maxHeight)) {
                val inner = getSlicedView(child, maxHeight - offset)
                if (inner != null) {
                    return Pair(inner.first, offset + inner.second + child.paddingTop)
                }
                offset += child.measuredHeight
            } else {
                if (offset + child.measuredHeight > maxHeight) {
                    return Pair(child, offset)
                }
                offset += child.measuredHeight
            }
        }
        return null
    }

    private fun isNonSplittableView(view: View): Boolean {
        return view is ImageView
    }

    class Builder(width: Int, height: Int) {
        private val generator = PdfGenerator(width, height)

        fun overlay(view: View): Builder {
            generator.overlays.add(view)
            return this
        }

        fun overlay(view: View, displayCondition: (PageContext) -> Boolean): Builder {
            generator.overlays.add(view, displayCondition)
            return this
        }

        fun overlay(overlay: Overlay): Builder {
            generator.overlays.add(overlay)
            return this
        }

        fun overlay(overlayGenerator: (DocumentLayout) -> Overlay): Builder {
            val overlay = overlayGenerator(generator.layout)
            generator.overlays.add(overlay)
            return this
        }

        fun layout(view: ViewGroup): Builder {
            if (!generator.layout.isVerticalSplittableViewGroup(view)) throw IllegalArgumentException("The content array must only contain vertical wrappable ViewGroups")
            generator.contents.add(view)
            return this
        }

        suspend fun layout(layoutGenerator: suspend (DocumentLayout) -> ViewGroup): Builder {
            val view = layoutGenerator(generator.layout)
            if (!generator.layout.isVerticalSplittableViewGroup(view)) throw IllegalArgumentException("The content array must only contain vertical wrappable ViewGroups")
            generator.contents.add(view)
            return this
        }

        suspend fun <T> layouts(items: Collection<T>, layoutGenerator: suspend (T, DocumentLayout) -> ViewGroup): Builder {
            for (item in items) {
                val view = layoutGenerator(item, generator.layout)
                if (!generator.layout.isVerticalSplittableViewGroup(view)) throw IllegalArgumentException("The content array must only contain vertical wrappable ViewGroups")
                generator.contents.add(view)
            }
            return this
        }

        suspend fun generate(file: File) {
            generator.generate(file)
        }

        suspend fun generate(stream: OutputStream) {
            generator.generate(stream)
        }

        suspend fun generatePreview(context: Context, pageNumber: Int, scale: Float = 1f): Bitmap {
            return generator.generatePreview(context, pageNumber, scale)
        }
    }
}