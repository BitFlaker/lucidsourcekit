package com.bitflaker.lucidsourcekit.views

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.graphics.minus
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.utils.Tools.manipulateAlpha
import com.bitflaker.lucidsourcekit.utils.attrColor
import com.bitflaker.lucidsourcekit.utils.dpToPx
import com.bitflaker.lucidsourcekit.utils.resolveDrawableBitmap
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class OutsideSlider @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    // Pre-configured paints
    private var primColor = context.attrColor(R.attr.primaryTextColor)
    private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
        color = primColor
    }
    private val dataLinePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
        color = context.attrColor(R.attr.primaryTextColor)
        style = Paint.Style.STROKE
        strokeWidth = 2f.dpToPx
    }

    // Configurable view specific fields
    var leftIcon = context.resolveDrawableBitmap(R.drawable.ic_baseline_check_24, primColor, 32.dpToPx)!!
    var rightIcon = context.resolveDrawableBitmap(R.drawable.ic_baseline_snooze_24, primColor, 32.dpToPx)!!
    var leftArrow = context.resolveDrawableBitmap(R.drawable.ic_round_keyboard_double_arrow_left_24, 24.dpToPx)!!
    var rightArrow = context.resolveDrawableBitmap(R.drawable.ic_round_keyboard_double_arrow_right_24, 24.dpToPx)!!
    var requiresSwipeMotion: Boolean = false
    var buttonSelectorRadius = 24f.dpToPx
    var sidePadding = 32f.dpToPx
    var indicatorMoveTo = 50.dpToPx

    // Event listeners
    var onLeftSideSelectedListener: (() -> Unit)? = null
    var onRightSideSelectedListener: (() -> Unit)? = null
    var onFadedAwayListener: (() -> Unit)? = null

    // Internal values
    private var dragButtonPosition = PointF()
    private var currentArrowMargin = 0f
    private var isHoldSwiping = false
    private var isSwiping = false
    private var arrowAnimation = ValueAnimator.ofInt(0, indicatorMoveTo).apply {
        duration = 1300
        interpolator = LinearInterpolator()
        repeatCount = -1
        repeatMode = ValueAnimator.RESTART
        addUpdateListener {
            currentArrowMargin = (it.getAnimatedValue() as Int).toFloat()
            invalidate()
        }
    }

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
        arrowAnimation.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Set the arrow icon alpha to the appropriate value of the animation
        indicatorPaint.color = manipulateAlpha(primColor, getCurrentAnimatedAlpha())

        // Draw the animated left and right arrows
        canvas.drawBitmap(
            leftArrow,
            (width - sidePadding) / 2.0f - leftArrow.width - currentArrowMargin,
            (height - leftArrow.height) / 2.0f,
            indicatorPaint
        )
        canvas.drawBitmap(
            rightArrow,
            (width + sidePadding) / 2.0f + currentArrowMargin,
            (height - leftArrow.height) / 2.0f,
            indicatorPaint
        )

        // Draw the left and right icons
        canvas.drawBitmap(
            leftIcon,
            sidePadding,
            (height - leftIcon.height) / 2.0f,
            dataLinePaint
        )
        canvas.drawBitmap(
            rightIcon,
            width - rightIcon.width - sidePadding,
            (height - rightIcon.height) / 2.0f,
            dataLinePaint
        )

        // Draw the drag indicator button
        canvas.drawCircle(dragButtonPosition.x, height / 2.0f, buttonSelectorRadius, dataLinePaint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        dragButtonPosition.x = width / 2f
        dragButtonPosition.y = height / 2f
        invalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                checkIfIsSwiping(event)
                setButtonPosition(event)
                isSwiping = true
            }
            MotionEvent.ACTION_MOVE -> setButtonPosition(event)
            MotionEvent.ACTION_UP -> {
                setButtonPosition(event)

                // Check if let go above one of the option icons
                when (getHoverOverSide()) {
                    HoveringOverSide.NONE -> {
                        resetButtonPos()
                        isHoldSwiping = false
                        isSwiping = false
                    }

                    // Selected the left icon
                    HoveringOverSide.LEFT -> {
                        onLeftSideSelectedListener?.invoke()
                        fadeOut()
                    }

                    // Selected the right option
                    HoveringOverSide.RIGHT -> {
                        onRightSideSelectedListener?.invoke()
                        fadeOut()
                    }
                }
            }
        }

        invalidate()
        return true
    }

    private fun getCurrentAnimatedAlpha(): Float {
        val pos = currentArrowMargin / indicatorMoveTo.toFloat()
        if (requiresSwipeMotion && !isHoldSwiping || !requiresSwipeMotion && !isSwiping) {
            return max(min(7.48164 * pos.toDouble().pow(3.0) - 15.36474 * pos.toDouble().pow(2.0) + 7.88309 * pos, 1.0), 0.0).toFloat()
        }
        return 0f
    }

    private fun fadeOut() {
        ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 300
            interpolator = LinearInterpolator()
            addUpdateListener {
                val value = it.getAnimatedValue() as Float
                setAlpha(value)
                if (value == 0.0f) {
                    onFadedAwayListener?.invoke()
                }
            }
        }.start()
    }

    private fun checkIfIsSwiping(event: MotionEvent) {
        val eventPosition = PointF(event.x, event.y)
        val distance = dragButtonPosition - eventPosition
        isHoldSwiping = distance.length() <= buttonSelectorRadius
    }

    private fun resetButtonPos() {
        dragButtonPosition.x = width / 2.0f
        arrowAnimation.cancel()
        arrowAnimation.start()
    }

    private fun setButtonPosition(event: MotionEvent) {
        if (!requiresSwipeMotion || isHoldSwiping) {
            val xMin = sidePadding + leftIcon.width / 2f
            val xMax = width - sidePadding - rightIcon.width / 2f
            dragButtonPosition.x = min(max(event.x, xMin), xMax)
        }
    }

    private fun getHoverOverSide(): HoveringOverSide {
        if (dragButtonPosition.x <= sidePadding + leftIcon.width) {
            return HoveringOverSide.LEFT
        } else if (dragButtonPosition.x >= width - sidePadding - rightIcon.width) {
            return HoveringOverSide.RIGHT
        }
        return HoveringOverSide.NONE
    }

    internal enum class HoveringOverSide {
        NONE,
        LEFT,
        RIGHT
    }
}