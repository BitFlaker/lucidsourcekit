package com.bitflaker.lucidsourcekit.views

import android.animation.ValueAnimator
import android.animation.ValueAnimator.REVERSE
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ComposeShader
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.annotation.ColorInt
import com.bitflaker.lucidsourcekit.utils.Tools
import kotlin.math.min

class GradientCircle @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gradientMatrix = Matrix()
    private val animator: ValueAnimator
    private var startOnMeasured = false
    private var gradientsInitialized = false

    // Values computed after size change to center fit the gradient circle
    private var radius = 0f
    private var centerX = 0f
    private var centerY = 0f

    // The offset of the mask gradient at the beginning of the animation
    private var startMaskOffset = 0.7f

    // The masking gradient for slowly fading shadow
    private lateinit var maskGradient: RadialGradient
    private var maskColor = Color.BLACK

    // The base gradient with transparent center and colored shadow
    private lateinit var baseGradient: RadialGradient
    private var colors = intArrayOf(
        Color.TRANSPARENT,
        Color.TRANSPARENT,
        Color.YELLOW,
        Color.YELLOW
    )

    // The gradient stops for the circle and the current mask scale offset
    private var baseStops = floatArrayOf(0f, 0.7f, 0.74f, 1f)
    private var currentScale = startMaskOffset

    init {
        animator = ValueAnimator.ofFloat(startMaskOffset, 1f).apply {
            duration = 750
            repeatMode = REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                currentScale = animation.animatedValue as Float
                updateMaskMatrix()
                invalidate()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawCircle(centerX, centerY, radius, paint)
        // TODO: Add some kind of unreadable or changing text in the center of the circle
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidh: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidh, oldHeight)

        radius = min(width, height) / 2f
        centerX = width / 2f
        centerY = height / 2f

        setupGradient()
        if (startOnMeasured && !animator.isStarted) {
            animator.start()
        }
    }

    private fun setupGradient() {
        // The base gradient cannot be initialized yet, as the radius still is zero, therefore wait
        // for the onSizeChanged(...) call later
        if (radius == 0f) {
            return
        }
        maskGradient = RadialGradient(
            0f, 0f,
            1f,
            intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT, maskColor),
            baseStops,
            Shader.TileMode.CLAMP
        )
        baseGradient = RadialGradient(
            centerX,
            centerY,
            radius,
            colors,
            baseStops,
            Shader.TileMode.CLAMP
        )
        gradientsInitialized = true
        paint.shader = ComposeShader(baseGradient, maskGradient, PorterDuff.Mode.XOR)
        updateMaskMatrix()
    }

    private fun updateMaskMatrix() {
        gradientMatrix.reset()
        gradientMatrix.postScale(radius * currentScale, radius * currentScale)
        gradientMatrix.postTranslate(centerX, centerY)
        maskGradient.setLocalMatrix(gradientMatrix)
    }

    fun setColor(@ColorInt color: Int) {
        colors[2] = color
        colors[3] = color
        setupGradient()
    }

    fun setMaskColor(@ColorInt color: Int) {
        maskColor = color
        setupGradient()
    }

    fun setStopsExact(stops: FloatArray) {
        baseStops = stops
        setupGradient()
    }

    fun setStops(stop: Float, stopThreshold: Float = 0.02f) {
        val stopMedian = stop.coerceIn(stopThreshold, 1 - stopThreshold)
        baseStops = floatArrayOf(0f, stopMedian - stopThreshold, stopMedian + stopThreshold, 1f)
        setStartMaskOffset(baseStops[1])
        setupGradient()
    }

    fun setStartMaskOffset(offset: Float) {
        startMaskOffset = offset
        animator.setFloatValues(startMaskOffset, 1f)
        currentScale = startMaskOffset
    }

    fun setAnimationDuration(duration: Long) {
        animator.duration = duration
    }

    fun startAnimation(startDelay: Long = 0) {
        startOnMeasured = true
        animator.startDelay = startDelay
        if (gradientsInitialized) {
            animator.start()
        }
    }

    fun setBlink(blink: Boolean) {
        animator.repeatCount = if (blink) 1 else 0
    }

    fun stopAnimation() {
        animator.cancel()
    }
}