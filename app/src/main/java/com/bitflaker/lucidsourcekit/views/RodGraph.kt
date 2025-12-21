package com.bitflaker.lucidsourcekit.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.utils.attrColor
import com.bitflaker.lucidsourcekit.utils.dpToPx
import com.bitflaker.lucidsourcekit.utils.resolveDrawable
import com.bitflaker.lucidsourcekit.utils.spToPx
import com.bitflaker.lucidsourcekit.utils.toBitmap
import com.bitflaker.lucidsourcekit.views.types.DataValue
import kotlin.math.ceil
import kotlin.math.max

class RodGraph @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    // Pre-configured paints
    private val dataLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
        color = context.attrColor(R.attr.secondaryTextColor)
        textSize = 14f.spToPx
        textAlign = Paint.Align.CENTER
    }
    private val dataLinePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
        color = context.attrColor(R.attr.colorTertiary)
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 9f.dpToPx
    }
    private val dataLinePaintBackground = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
        color = context.attrColor(R.attr.colorSurfaceContainer)
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 9f.dpToPx
    }

    // Configurable view specific fields
    var extraSpacing = 28.dpToPx
    var iconSize = 24.dpToPx
    var textMargin = 6f.dpToPx
    var iconMargin = 6f.dpToPx
    var data: MutableList<DataValue> = ArrayList()
        set(value) {
            field = value
            xMax = field.size.toFloat()
            yMax = calculateMaxValueY()
        }
    var icons: Array<Drawable>? = null
        set(value) {
            field = value
            iconBitmaps = field?.map {
                it.toBitmap(iconSize)
            }?.toTypedArray()
        }

    // Internal values
    private var textHeight = 0f
    private var xMax = 0f
    private var yMax = 0f
    private val fontMetrics = dataLabelPaint.fontMetrics
    private val textLineHeight = ceil(fontMetrics.descent - fontMetrics.ascent)
    private var iconBitmaps: Array<Bitmap>? = null
        set(value) {
            field = value
            yMax = calculateMaxValueY()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Get the width of the area for icons as well as the width available for each data point
        val iconAreaWidth = if (iconBitmaps != null) iconSize + iconMargin else 0f
        val dataPointAreaWidth = (width - iconAreaWidth) / xMax

        // Get the top and bottom margin for the lines and icons in a way that both the icons and
        // the lines fit within the view and are not cut off
        val iconLineInset = max(iconAreaWidth / 2f, dataLinePaint.strokeWidth / 2f)

        // Get the additional margin required for the text to achieve at least `textMargin`. This is
        // due to icons already creating a larger offset between the data line track and the text
        val extraTextMargin = if (iconBitmaps != null) (iconSize - dataLinePaint.strokeWidth) / 2f else 0f
        val actualTextMargin = max(0f, textMargin - extraTextMargin)

        // Get the maximum text area height
        textHeight = (data.maxOfOrNull { it.getLines().count() } ?: 0) * textLineHeight

        // Get the bottom y position of the progress lines
        val lineBottomY = height - textHeight - actualTextMargin - iconLineInset

        // Get the step size of the value line (which is the same as
        // the distance between the center of two icons)
        val stepSize = (iconLineInset - lineBottomY) / yMax

        // Draw the label and value progress for each data point
        data.forEachIndexed { i, current ->

            // Get the x position of the current data point
            val xPos = iconAreaWidth + (i + 0.5f) * dataPointAreaWidth

            // Draw all lines of text for the current data point
            current.getLines().forEachIndexed { i, line ->
                    canvas.drawText(
                        line,
                        xPos,
                        height - textHeight - fontMetrics.ascent + i * textLineHeight,
                        dataLabelPaint
                    )
                }

            // Draw the track line of the current data point
            canvas.drawLine(
                xPos,
                lineBottomY,
                xPos,
                iconLineInset,
                dataLinePaintBackground
            )

            // Draw the value line of the current data point if there is any data
            if (current.value > -1) {
                canvas.drawLine(
                    xPos,
                    lineBottomY,
                    xPos,
                    lineBottomY + current.value.toFloat() * stepSize,
                    dataLinePaint
                )
            }
        }

        // Draw the icons for all values
        iconBitmaps?.forEachIndexed { i, icon ->
            canvas.drawBitmap(
                icon,
                0f,
                lineBottomY - iconSize / 2f + i * stepSize,
                dataLinePaintBackground
            )
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        // TODO: Re-calculate the minimum height when the data, icons, iconSize changes

        val iconCount = iconBitmaps?.size ?: 4
        textHeight = (data.maxOfOrNull { it.getLines().count() } ?: 0) * textLineHeight
        minimumHeight = ceil(iconCount * iconSize + textHeight).toInt()

        // TODO: Do not overwrite a custom defined height with the automatically calculated one here
        setMeasuredDimension(
            widthMeasureSpec,
            minimumHeight + extraSpacing
        )
    }

    private fun calculateMaxValueY(): Float {
        return iconBitmaps?.let { it.size - 1f } ?: data.maxOfOrNull { it.value.toFloat() } ?: 0f
    }
}