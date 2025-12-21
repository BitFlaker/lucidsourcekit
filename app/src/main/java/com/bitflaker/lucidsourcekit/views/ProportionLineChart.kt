package com.bitflaker.lucidsourcekit.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.utils.attrColor
import com.bitflaker.lucidsourcekit.utils.dpToPx
import com.bitflaker.lucidsourcekit.utils.getTextBounds
import com.bitflaker.lucidsourcekit.utils.spToPx
import kotlin.math.max
import kotlin.math.min

class ProportionLineChart @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    // Pre-configured paints
    private var trackWidth = 12.dpToPx
    @ColorInt
    private val trackColor = context.attrColor(R.attr.colorSurface)

    private val dataLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
        color = context.attrColor(R.attr.secondaryTextColor)
        textAlign = Paint.Align.LEFT
        isFakeBoldText = true
        textSize = 12f.spToPx
    }
    private val dataLinePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
        color = trackColor
        strokeWidth = trackWidth.toFloat()
        strokeCap = Paint.Cap.ROUND
        style = Paint.Style.STROKE
    }
    private val dataLegendPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
        color = trackColor
        style = Paint.Style.FILL
    }

    // Configurable view specific fields
    var drawTrack = false
    var valueGap = 4.dpToPx
    var legendMarginTop = 8.dpToPx
    var legendCircleRadius = 4f.dpToPx
    var legendCircleTextMargin = 4.dpToPx
    var legendItemsMargin = 16.dpToPx
    var paddingVertical = 4.dpToPx
    var values: Array<DataPoint> = arrayOf()
        set(value) {
            field = value
            maxLegendTextHeight = 0
            labelWidths = value.map {
                dataLabelPaint.getTextBounds(it.label, textBounds)
                maxLegendTextHeight = max(maxLegendTextHeight, textBounds.height())
                textBounds.width()
            }.toIntArray()
            hideTooSmallValues(width)
            requestLayout()
            invalidate()
        }

    // Internal values
    private var maxLegendTextHeight = 0
    private var valuesZeroed: Array<DataPoint> = arrayOf()
    private var valueTrackWidths: FloatArray = floatArrayOf()
    private var labelWidths: IntArray = intArrayOf()
    private var textBounds = Rect()

//    init {
//        values = arrayOf(
//            DataPoint(context.color(R.color.lighter_green), 100f, "Value1"),
//            DataPoint(context.color(R.color.lighter_orange), 18f, "SomeValue2"),
//            DataPoint(context.color(R.color.lighter_red), 273f, "Value3")
//        )
//    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val lineStrokeRadius = dataLinePaint.strokeWidth / 2f

        // Draw the track if required
        if (drawTrack) {
            dataLinePaint.color = trackColor
            canvas.drawLine(
                lineStrokeRadius,
                paddingVertical + lineStrokeRadius,
                width - lineStrokeRadius,
                lineStrokeRadius + paddingVertical,
                dataLinePaint
            )
        }

        // Draw the legend and values for non hidden values
        var textPositionX = 0f
        var valuePositionX = lineStrokeRadius
        valuesZeroed.forEachIndexed { i, current ->
            if (current.value > 0f) {
                // Draw the color indicator circle
                dataLegendPaint.color = current.color
                canvas.drawCircle(
                    textPositionX + legendCircleRadius,
                    lineStrokeRadius * 2 + paddingVertical + legendMarginTop + maxLegendTextHeight - legendCircleRadius,
                    legendCircleRadius,
                    dataLegendPaint
                )

                // Draw the label text
                canvas.drawText(
                    current.label,
                    textPositionX + legendCircleRadius * 2 + legendCircleTextMargin,
                    lineStrokeRadius * 2 + paddingVertical + legendMarginTop + maxLegendTextHeight,
                    dataLabelPaint
                )

                // Draw the value line
                dataLinePaint.color = current.color
                canvas.drawLine(
                    valuePositionX,
                    lineStrokeRadius + paddingVertical,
                    valuePositionX + valueTrackWidths[i],
                    lineStrokeRadius + paddingVertical,
                    dataLinePaint
                )

                // Move to the next text and value x-positions
                textPositionX += legendCircleRadius * 2 + legendCircleTextMargin + legendItemsMargin + labelWidths[i]
                valuePositionX += valueTrackWidths[i] + 2 * lineStrokeRadius + valueGap
            }
        }
    }

    /**
     * Calculates the line widths on the chart and hides all values which would be too short to
     * be displayed in the chart. It removes the smallest values until it can fit the rest entirely
     * into the view
     */
    private fun hideTooSmallValues(width: Int) {
        var indexToHide: Int
        valuesZeroed = values.map { it.clone() }.toTypedArray()

        do {
            indexToHide = -1

            val gapCount = valuesZeroed.count { it.value != 0f } - 1
            val total = valuesZeroed.sumOf { it.value.toDouble() }.toFloat()

            // Get the usable track width by removing the sizes of gaps between values
            val usableTrackWidth = width.toFloat() - gapCount * valueGap

            // Calculate the widths of every normalized data point and get the index of
            // the shortest width not fitting inside the view
            var smallestWidth = usableTrackWidth
            valueTrackWidths = valuesZeroed.mapIndexed { i, point ->
                if (point.value == 0f) {
                    return@mapIndexed 0f
                }

                val valueTrackWidth = (point.value / total) * usableTrackWidth - dataLinePaint.strokeWidth
                if (valueTrackWidth < 0 && valueTrackWidth < smallestWidth) {
                    smallestWidth = valueTrackWidth
                    indexToHide = i
                }
                valueTrackWidth
            }.toFloatArray()

            // Hide a value if it is too small to be displayed
            if (indexToHide != -1) {
                valuesZeroed[indexToHide].value = 0f
            }
        } while (indexToHide != -1)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        // Calculate the minimum required height of the view
        val minHeight = trackWidth + legendMarginTop + maxLegendTextHeight + paddingVertical * 2

        // Get required width
        var width = MeasureSpec.getSize(widthMeasureSpec)

        // Get required height
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        var height = when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.EXACTLY -> max(heightSize, minHeight)
            MeasureSpec.AT_MOST -> min(heightSize, minHeight)
            else -> minHeight
        }

        // Comply with minimum width and height
        height = max(minimumHeight, height)
        width = max(minimumWidth, width)

        // Apply the measured size
        setMeasuredDimension(width, height)

        // Recalculate values to hide
        hideTooSmallValues(width)
        invalidate()
    }

    class DataPoint(
        var color: Int,
        var value: Float,
        var label: String
    ) : Cloneable {
        public override fun clone(): DataPoint = super.clone() as DataPoint
    }
}
