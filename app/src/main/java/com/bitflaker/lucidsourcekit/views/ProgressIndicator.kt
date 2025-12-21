package com.bitflaker.lucidsourcekit.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.utils.Tools
import com.bitflaker.lucidsourcekit.utils.attrColor

class ProgressIndicator @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val paint = Paint().apply {
        color = context.attrColor(R.attr.colorOutlineVariant)
        strokeCap = Paint.Cap.ROUND
    }
    private var progress = 0f

    override fun onDraw(canvas: Canvas) {
        paint.strokeWidth = height.toFloat()
        val progressWidth = width * progress / 100f
        canvas.drawLine(0f, height / 2f, progressWidth, height / 2f, paint)
    }

    fun setProgress(progress: Float) {
        this.progress = progress.coerceAtLeast(0f)
        invalidate()
    }
}