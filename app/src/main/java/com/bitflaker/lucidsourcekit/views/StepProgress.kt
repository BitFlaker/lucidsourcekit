package com.bitflaker.lucidsourcekit.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.utils.Tools
import com.bitflaker.lucidsourcekit.utils.attrColor
import com.bitflaker.lucidsourcekit.utils.dpToPx

class StepProgress @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var colorActive = context.attrColor(R.attr.colorOutlineVariant)
    private var colorCurrent = context.attrColor(R.attr.colorOutline)
    private var colorInactive = context.attrColor(R.attr.colorSurfaceContainer)
    private var strokeHeight = 6.dpToPx.toFloat()
    private var strokeGap = 8.dpToPx.toFloat()
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