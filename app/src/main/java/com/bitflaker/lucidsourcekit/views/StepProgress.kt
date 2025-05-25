package com.bitflaker.lucidsourcekit.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.utils.Tools

class StepProgress @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var colorActive = Tools.getAttrColor(R.attr.colorPrimary, context.theme)
    private var colorCurrent = Tools.getAttrColor(R.attr.colorPrimaryContainer, context.theme)
    private var colorInactive = Tools.getAttrColor(R.attr.colorSurfaceContainer, context.theme)
    private var strokeHeight = Tools.dpToPx(context, 6.0).toFloat()
    private var strokeGap = Tools.dpToPx(context, 8.0).toFloat()
    private var paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = strokeHeight
        strokeCap = Paint.Cap.ROUND
    }
    var totalStepCount = 10
        set(value) {
            field = value
            invalidate()
        }
    var currentStepCount = 4
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val padding = strokeHeight / 2.0f
        val lineWidth = (width - (totalStepCount - 1) * (strokeGap + strokeHeight) - strokeHeight) / totalStepCount.toFloat()

        for (i in 0..<totalStepCount) {
            paint.color = if (i + 1 < currentStepCount) colorActive else if (i + 1 == currentStepCount) colorCurrent else colorInactive
            val startX = i * (lineWidth + (strokeGap + strokeHeight)) + padding
            canvas.drawLine(startX, height / 2.0f, startX + lineWidth, height / 2.0f, paint)
        }
    }
}