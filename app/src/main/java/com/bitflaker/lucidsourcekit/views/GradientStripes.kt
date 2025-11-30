package com.bitflaker.lucidsourcekit.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import com.bitflaker.lucidsourcekit.utils.Tools
import com.bitflaker.lucidsourcekit.utils.dpToPx
import kotlin.math.max
import androidx.core.graphics.toColorInt

class GradientStripes @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var stripeColor = "#43474E".toColorInt()
    private var stripeThickness = 3.dpToPx.toFloat()
    private var stripeGap = 0.dpToPx.toFloat()
    private var maxAlpha = 192f
    private var stripeCount = 4
    private var currentPositionOffset = 1f
    private var currentExpansion = 0f
    private val animatorPosition: ValueAnimator = ValueAnimator.ofFloat(1f, 0f).apply {
        duration = 500
        interpolator = AccelerateDecelerateInterpolator()
        addUpdateListener { animation ->
            currentPositionOffset = animation.animatedValue as Float
            invalidate()
        }
    }
    private val animatorExpansion: ValueAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 600
        startDelay = animatorPosition.duration - 150
        interpolator = AccelerateDecelerateInterpolator()
        addUpdateListener { animation ->
            currentExpansion = animation.animatedValue as Float
            invalidate()
        }
    }
    private var paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = stripeThickness
        color = stripeColor
        strokeCap = Paint.Cap.ROUND
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val totalWidth = stripeCount * stripeThickness + (stripeCount - 1) * stripeGap
        val marginStart = (width - totalWidth) / 2f + stripeThickness / 2.0f
        val offset = height.toFloat() * currentPositionOffset
        paint.alpha = ((1f - currentPositionOffset - currentExpansion) * maxAlpha).toInt()
        paint.strokeWidth = max(stripeThickness, (width * 2f) * currentExpansion)

        for (i in 0..<stripeCount) {
            val x = marginStart + (stripeThickness + stripeGap) * i
            canvas.drawLine(x, 0f + offset, x, height.toFloat() + offset, paint)
        }
    }

    fun startAnimations(startDelay: Long) {
        animatorPosition.startDelay = startDelay
        animatorExpansion.startDelay += startDelay
        animatorPosition.start()
        animatorExpansion.start()
    }
}