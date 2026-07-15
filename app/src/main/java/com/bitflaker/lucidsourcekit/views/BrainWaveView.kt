package com.bitflaker.lucidsourcekit.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

class BrainWaveView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val path = Path()
    private var offset = 0f
    private val seed = (0..500).random()

    private fun sample(worldX: Float): Float {
        val t = worldX * 0.012f
        val delta = sin(t) * 0.5f + sin(t * 0.37f + 1.7f) * 0.3f
        val theta = sin(t * 2.9f + sin(t * 0.13f) * 4f) * 0.2f
        val spindle = sin(t * 11f) * 0.25f * max(0f, sin(t * 0.21f + 2f)).pow(6)
        val noise = (sin(t * 23.7f) * sin(t * 17.3f + 5f) + sin(t * 31.1f)) * 0.05f
        return delta + theta + spindle + noise
    }

    override fun onDraw(canvas: Canvas) {
        val midY = height / 2f
        val amp = midY - paint.strokeWidth
        path.rewind()
        var x = -STEP
        path.moveTo(x, midY + sample(x + offset + seed) * amp)
        while (x <= width + STEP) {
            x += STEP
            path.lineTo(x, midY + sample(x + offset + seed) * amp)
        }
        canvas.drawPath(path, paint)
        offset += SPEED
        postInvalidateOnAnimation()
    }

    private companion object {
        const val STEP = 4f
        const val SPEED = 3f
    }
}