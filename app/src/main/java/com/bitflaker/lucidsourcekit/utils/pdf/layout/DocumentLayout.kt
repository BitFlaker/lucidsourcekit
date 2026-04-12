package com.bitflaker.lucidsourcekit.utils.pdf.layout

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isNotEmpty
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class DocumentLayout(val pageWidth: Int, val pageHeight: Int) {
    companion object {
        private fun isCellContent(child: ViewGroup): Boolean {
            return child.isNotEmpty() && child.getChildAt(0).tag == "cell_content"
        }

        fun autoSpanGridLayout(view: RecyclerView, maxDynamicHeight: Int): IntArray {
            if (view.layoutManager !is GridLayoutManager) throw IllegalArgumentException("RecyclerView must have GridLayoutManager to automatically generate column spans")

            val manager = view.layoutManager as GridLayoutManager
            val lookup = IntArray(view.adapter?.itemCount ?: 0) { 1 }
            var index = 0

            // Calculate the column spans to best fit the content inside the grid
            while (index < lookup.size) {
                val heights = mutableListOf<Int>()
                val maxHeightMaps = hashMapOf<Int, Int>()
                val textCellIndices = mutableListOf<Int>()

                var maxHeight = 0
                for (i in 0..<manager.spanCount) {
                    if (index + i >= lookup.size || index + i >= view.childCount) break

                    // Get the height of the cell
                    val child = view.getChildAt(index + i)
                    val refChild = if (child is ViewGroup && isCellContent(child)) child.getChildAt(0) else child
                    heights.add(refChild.measuredHeight)

                    // Check if the cell is dynamically sized (auto column spans)
                    if (child.tag == "dynamic") {
                        textCellIndices.add(i)
                    } else {
                        maxHeight = max(maxHeight, refChild.measuredHeight)
                    }

                    // Set the maximum reference height based on cell heights up to the current cell
                    maxHeightMaps[maxHeightMaps.size] = maxHeight
                }

                // Calculate the column count and column spans for the current row
                var dropCount = 0
                var columnSpans: IntArray
                while (true) {
                    val targetColumnCount = min(manager.spanCount - dropCount, heights.size)

                    // The first cell in the row would require at least the full row width, therefore
                    // simply set its span to the column count and let it expand vertically.
                    // Otherwise, initialize the spans to single columns
                    if (targetColumnCount == 0) {
                        columnSpans = intArrayOf(manager.spanCount)
                        break
                    }
                    columnSpans = IntArray(targetColumnCount) { 1 }

                    // Get the max height after which the column span should be increased
                    val maxHeight = max(maxHeightMaps[targetColumnCount - 1]!!, maxDynamicHeight)

                    // Update the column spans and check if the target column count fits all its contents
                    var recalculate = false
                    var localSpanCounter = 0
                    for (i in textCellIndices.filter { it < targetColumnCount }) {
                        // In case the height is exceeded, set the span to the size it would require to fit
                        if (heights[i] > maxHeight) {
                            val span = ceil(heights[i] / maxHeight.toDouble()).toInt()
                            localSpanCounter += span - 1
                            columnSpans[i] = span
                        }

                        // In case the column spans would be larger than the available columns,
                        // drop the last column and recalculate
                        if (localSpanCounter > dropCount) {
                            recalculate = true
                            dropCount += 1
                            break
                        }
                    }

                    // A column was dropped, therefore recalculate again with new column count
                    if (recalculate) continue

                    // Add padding to the last column in the row if it is not the last row.
                    // This allows the dropped columns to wrap to the next row
                    if (index + columnSpans.size < lookup.size) {
                        columnSpans[columnSpans.size - 1] += manager.spanCount - columnSpans.sum()
                    }
                    break
                }

                // Update the final lookup array and increase the index to advance to the next row
                for (i in columnSpans.indices) {
                    lookup[index + i] = columnSpans[i]
                }
                index += columnSpans.size
            }

            return lookup
        }
    }

    var maxEmptySpaceThreshold: Int = 200   // Must be between 0 and (pageHeight - vertical page margins)   // Prevents wrapping for large non-no_wrap views still fitting inside page (has to be at max the height of the page - vertical page padding)

    fun isVerticalSplittableViewGroup(view: ViewGroup, maxHeight: Int = -1): Boolean {
        // Ignore wrappable view groups which explicitly define the "no-break" tag
        if (view.tag == "no-break") return false

        // Only break view when the empty space threshold has been exceeded, otherwise wrap to next page
        if (view.tag == "no-wrap-layout" && maxHeight != -1 && maxHeight < maxEmptySpaceThreshold) return false

        // Check if it is a vertical LinearLayout
            if (view is LinearLayout) {
                return view.orientation == LinearLayout.VERTICAL
            }

        // Check if it is a MaterialCardView
        if (view is MaterialCardView) {
            return true
        }

        // Check if it is a RecyclerView with a vertical LinearLayoutManager
        if (view is RecyclerView && view.layoutManager !is GridLayoutManager) {
            val manager = view.layoutManager as? LinearLayoutManager ?: return false
            return manager.orientation == LinearLayoutManager.VERTICAL
        }

        // Unsupported ViewGroup
        return false
    }

    fun isGridSplittableViewGroup(view: ViewGroup): Boolean {
        // Ignore wrappable view groups which explicitly define the "no-break" tag
        if (view.tag == "no-break") return false

        // Check if it is a RecyclerView with a vertical LinearLayoutManager
        if (view is RecyclerView) {
            val manager = view.layoutManager as? GridLayoutManager ?: return false
            return manager.orientation == LinearLayoutManager.VERTICAL
        }

        // Unsupported ViewGroup
        return false
    }

    fun isWrappableViewGroup(view: ViewGroup, maxHeight: Int): Boolean {
        return isVerticalSplittableViewGroup(view, maxHeight) ||
                isGridSplittableViewGroup(view)
    }

    fun measureLayout(view: ViewGroup) {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(pageWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        view.forceLayout()
        view.layout(0, 0, pageWidth, view.measuredHeight)
    }
}