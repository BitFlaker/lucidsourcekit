package com.bitflaker.lucidsourcekit.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.utils.attrColor
import com.bitflaker.lucidsourcekit.utils.dpToPx
import kotlin.math.cos
import kotlin.math.sin

class CircleGraph(context: Context, attributeSet: AttributeSet?) : View(context, attributeSet) {
    var value1: Int = 2
    var value2: Int = 3
    var lineWidth: Float = 15.0f.dpToPx
    var separatorWidth: Float = 1.25f.dpToPx
    private val startAngle: Float = 270f
    private val bounds = RectF()
    private val dataLinePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val dataLinePaintEraser = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun setData(value1: Int, value2: Int, lineWidth: Float, separatorWidth: Float) {
        this.value1 = value1
        this.value2 = value2
        this.lineWidth = lineWidth
        this.separatorWidth = separatorWidth
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Cache current view dimensions
        val height = height
        val width = width

        // Get the radius of the shortest side and the center to fit the circle in the view
        val radius = if (width > height) height * 0.5f else width * 0.5f
        val centerX = width * 0.5f
        val centerY = height * 0.5f

        // Get the sweep angle of both values
        val sweepValue1 = (360.0f * value1) / (value1 + value2)
        val sweepValue2 = 360 - sweepValue1

        // Set the bounds used for the arcs
        bounds.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius)

        // Draw both value arcs
        dataLinePaint.color = context.attrColor(R.attr.colorTertiary)
        canvas.drawArc(bounds, startAngle, sweepValue1, true, dataLinePaint)
        dataLinePaint.color = context.attrColor(R.attr.colorPrimary)
        canvas.drawArc(bounds, startAngle + sweepValue1, sweepValue2, true, dataLinePaint)

        // Draw a separator line between both values
        if (value1 != 0 && value2 != 0) {
            val angle = (startAngle + sweepValue1) * Math.PI / 180.0
            val xVal = centerX + cos(angle).toFloat() * centerX
            val yVal = centerY + sin(angle).toFloat() * centerY

            // Draw separator lines with eraser
            dataLinePaintEraser.strokeWidth = separatorWidth
            canvas.drawLine(centerX, centerY, centerX, 0f, dataLinePaintEraser)
            canvas.drawLine(centerX, centerY, xVal, yVal, dataLinePaintEraser)
        }

        // Clear the center of the circle to be left with only the outlines
        dataLinePaintEraser.strokeWidth = 0f
        canvas.drawCircle(centerX, centerY, radius - lineWidth, dataLinePaintEraser)
    }
}