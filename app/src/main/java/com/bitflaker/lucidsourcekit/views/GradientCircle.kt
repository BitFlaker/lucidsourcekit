package com.bitflaker.lucidsourcekit.views

import android.animation.ValueAnimator
import android.animation.ValueAnimator.INFINITE
import android.animation.ValueAnimator.REVERSE
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ComposeShader
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.annotation.ColorInt
import androidx.core.animation.doOnEnd
import com.bitflaker.lucidsourcekit.utils.Tools
import com.bitflaker.lucidsourcekit.utils.spToPx
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class GradientCircle @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
        isAntiAlias = true
        textSize = 16.spToPx.toFloat()
        xfermode = PorterDuffXfermode(PorterDuff.Mode.XOR)
        color = Color.WHITE
        typeface = Typeface.MONOSPACE
    }
    private val gradientMatrix = Matrix()
    private val circleAnimator: ValueAnimator
    private val textAnimator: ValueAnimator
    private val textFadeInAnimator: ValueAnimator
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

    // Text animation related variables
    private val textBounds: Rect = Rect()
    private val textOffsetY = labelPaint.textSize * 0f
    private val textPhrases = Array(3) { "" }
    private val textDimensions = Array(4) { IntArray(2) }
    private var currentTextSlide = -1f
    private var textMaskCharacter = "#"
    private var textMaskCharacters = 0
    private var textMaskString: String? = null
    private var textOpacity = 0f
    private var textJitterEnabled = false
    private var textAnimationDuration = 4200L
    private var maskJitter = 64
    private var textJitter = 24

    init {
        setTextPhrases("am I", "Dreaming?", "check!")

        // Setup animations
        textAnimator = ValueAnimator.ofFloat(-0.25f, 2.25f).apply {
            duration = textAnimationDuration
            repeatCount = INFINITE
            repeatMode = REVERSE
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                currentTextSlide = animation.animatedValue as Float
                invalidate()
            }
        }
        textFadeInAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1400
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                textOpacity = animation.animatedValue as Float
                invalidate()
            }
        }
        circleAnimator = ValueAnimator.ofFloat(startMaskOffset, 1f).apply {
            duration = 750
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                currentScale = animation.animatedValue as Float
                updateMaskMatrix()
                invalidate()
            }
        }
        circleAnimator.doOnEnd {
            textFadeInAnimator.start()
            textAnimator.start()
            circleAnimator.removeAllListeners()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw the animated text in case it is visible
        if (textOpacity > 0f) {
            labelPaint.alpha = (255 * textOpacity).toInt()

            drawText(canvas, 0)
            drawText(canvas, 1)
            drawText(canvas, 2)

            // Draw text mask
            textMaskString?.let {
                // Calculate the position of the mask (with optional jitter)
                val randomJitterX = if (textJitterEnabled) (-maskJitter..maskJitter).random() / 10f else 0f
                val randomJitterY = if (textJitterEnabled) (-maskJitter..maskJitter).random() / 10f else 0f
                val startX = (width - textDimensions[textDimensions.lastIndex][0]) / 2f + randomJitterX
                val startY = (height - textDimensions[textDimensions.lastIndex][1]) / 2f + randomJitterY

                // Draw the mask
                canvas.drawText(it, startX, startY, labelPaint)
            }
        }

        // Draw the animated gradient circle
        canvas.drawCircle(centerX, centerY, radius, paint)
    }

    private fun drawText(canvas: Canvas, index: Int) {
        // Calculate the position of the text (with optional jitter)
        val randomJitterX = if (textJitterEnabled) (-textJitter..textJitter).random() / 10f else 0f
        val randomJitterY = if (textJitterEnabled) (-textJitter..textJitter).random() / 10f else 0f
        val x = (width - textDimensions[index][0]) / 2f - randomJitterX
        val y = (height - textDimensions[index][1]) / 2f - textOffsetY + textOffsetY * index - randomJitterY

        // Calculate the alpha for the current text
        val previousAlpha = labelPaint.alpha
        labelPaint.alpha = min(255, (max(0f, (1 - abs(currentTextSlide - index) / 3f) - 0.3f) * 255).toInt() + (0..64).random())

        // Draw the text and reset the alpha to its original value
        canvas.drawText(textPhrases[index], x, y, labelPaint)
        labelPaint.alpha = previousAlpha
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidh: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidh, oldHeight)

        radius = min(width, height) / 2f
        centerX = width / 2f
        centerY = height / 2f

        setupGradient()
        if (startOnMeasured && !circleAnimator.isStarted) {
            circleAnimator.start()
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

    /*+************************************
     **   Circle configuration methods   **
     ************************************+*/

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
        circleAnimator.setFloatValues(startMaskOffset, 1f)
        currentScale = startMaskOffset
    }

    fun setBlink(blink: Boolean) {
        circleAnimator.repeatCount = if (blink) 1 else 0
    }

    fun setAnimationDuration(duration: Long) {
        circleAnimator.duration = duration
    }

    fun startAnimation(startDelay: Long = 0) {
        startOnMeasured = true
        circleAnimator.startDelay = startDelay
        if (gradientsInitialized) {
            circleAnimator.start()
        }
    }

    fun stopAnimation() {
        circleAnimator.cancel()
    }

    fun reverseAnimation() {
        textFadeInAnimator.duration /= 3
        textFadeInAnimator.reverse()
        textFadeInAnimator.doOnEnd {
            textAnimator.pause()
            circleAnimator.duration /= 2
            circleAnimator.reverse()
            circleAnimator.repeatCount = 0
        }
    }

    /*+**********************************
     **   Text configuration methods   **
     **********************************+*/

    fun setTextPhrases(topText: String?, centerText: String?, bottomText: String?) {
        setTextPhrase(0, topText)
        setTextPhrase(1, centerText)
        setTextPhrase(2, bottomText)

        val longestString = textPhrases.maxBy { it.length }

        // Setup the text mask
        textMaskCharacters = longestString.length
        textMaskString = textMaskString ?: textMaskCharacter.repeat(textMaskCharacters)

        // Calculate the mask string dimensions
        labelPaint.getTextBounds(textMaskString, 0, textMaskString!!.length, textBounds)
        textDimensions[3][0] = textBounds.width()
        textDimensions[3][1] = textBounds.centerY()
    }

    private fun setTextPhrase(index: Int, text: String?) {
        textPhrases[index] = text ?: textPhrases[index]
        labelPaint.getTextBounds(textPhrases[index], 0, textPhrases[index].length, textBounds)
        textDimensions[index][0] = textBounds.width()
        textDimensions[index][1] = textBounds.centerY()
    }

    fun setTextMaskCharacter(maskCharacter: Char) {
        textMaskCharacter = maskCharacter.toString()
        textMaskString = textMaskCharacter.repeat(textMaskCharacters)
    }

    fun setTextSize(textSize: Float) {
        labelPaint.textSize = textSize

        // Recalculate all text measurements without changing text
        setTextPhrases(null, null, null)
    }

    fun setTextColor(@ColorInt color: Int) {
        labelPaint.color = color
    }

    fun setJitterEnabled(enabled: Boolean) {
        textJitterEnabled = enabled
    }

    fun setMaskJitter(jitter: Int) {
        maskJitter = jitter
    }

    fun setTextJitter(jitter: Int) {
        textJitter = jitter
    }

    fun setTextAnimationDuration(duration: Long) {
        textAnimationDuration = duration
    }
}