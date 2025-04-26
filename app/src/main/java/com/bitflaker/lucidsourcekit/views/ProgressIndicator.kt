package com.bitflaker.lucidsourcekit.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.utils.Tools

class ProgressIndicator : View {
    private val paint = Paint()
    private var progress = 0f

    constructor(context: Context?) : super(context) {
        setup()
    }

    constructor(context: Context?, attributeSet: AttributeSet?) : super(context, attributeSet) {
        setup()
    }

    private fun setup() {
        paint.color = Tools.getAttrColor(R.attr.colorOutlineVariant, context.theme)
        paint.strokeCap = Paint.Cap.ROUND
    }

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