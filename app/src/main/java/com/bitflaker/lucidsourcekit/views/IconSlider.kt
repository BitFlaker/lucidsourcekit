package com.bitflaker.lucidsourcekit.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.util.AttributeSet
import android.text.TextPaint
import android.content.Context
import androidx.annotation.ColorInt
import androidx.core.graphics.withTranslation
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.withScale
import androidx.core.content.ContextCompat
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.utils.Oklab
import com.bitflaker.lucidsourcekit.utils.attrColor
import com.bitflaker.lucidsourcekit.utils.dpToPx
import com.bitflaker.lucidsourcekit.utils.spToPx
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.pow

class IconSlider @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var trackHeight = 56f.dpToPx
    private var trackInset = 5f.dpToPx
    private var tooltipGap = 10f.dpToPx
    private val tooltipPadH = 14f.dpToPx
    private val tooltipPadV = 7f.dpToPx

    @ColorInt private var trackColor = context.attrColor(R.attr.colorSurfaceContainer)
    @ColorInt private var iconColor = context.attrColor(R.attr.colorOutline)
    private var colorRamp = intArrayOf(0xFF8B5CF6.toInt(), 0xFF7CB342.toInt())

    private val trackRect = RectF()

    private var thumbPos = 0f
    private var press = 0f
    private var animationProgressTooltip = 0f
    private var centerY = 0f
    private var thumbRadius = 0f
    private var iconSize = 0f
    private var slotStep = 0f
    private var firstSlotX = 0f

    private var thumbAnimator: ValueAnimator? = null
    private var grabAnimator: ValueAnimator? = null
    private var pressAnimator: ValueAnimator? = null
    private var tooltipAnimator: ValueAnimator? = null
    private val hideTooltip = Runnable { animateTooltip(0f) }

    private var dragging = false
    private var touchStartX = 0f
    private var grabOffset = 0f
    private var lastMovePosition = 0f
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private val tooltipDrawable = TooltipDrawable()
    private var tooltipHost: ViewGroup? = null
    private var tooltipAttached = false
    private val locSelf = IntArray(2)
    private val locHost = IntArray(2)
    private val scrollListener = ViewTreeObserver.OnScrollChangedListener {
        if (tooltipAttached) invalidate()
    }

    private val tooltipPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 1.5f.dpToPx
    }
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 13f.spToPx
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    var onValueChange: ((index: Int) -> Unit)? = null

    var items: List<Item> = emptyList()
        set(value) {
            require(value.size >= 2) { "At least 2 items are required" }
            field = value.onEach { it.icon.mutate() }
            selectedIndex = selectedIndex   // Ensure selected index is within icon range
            requestLayout()
        }

    fun setItems(vararg items: Pair<Int, String>) {
        this.items = items.map { (icon, label) ->
            Item(checkNotNull(ContextCompat.getDrawable(context, icon)), label)
        }
    }

    var selectedIndex: Int = 0
        private set(value) {
            if (field == value) return
            field = value.coerceIn(0, max(items.size - 1, 0))
            onValueChange?.invoke(field)
            thumbAnimator?.cancel()
            invalidate()
        }

    var value: Int
        get() = selectedIndex
        set(newValue) {
            selectedIndex = newValue
            thumbPos = selectedIndex.toFloat()
            invalidate()
        }

    fun setGradient(@ColorInt vararg colors: Int) {
        require(colors.size >= 2) { "A gradient needs at least two colors" }
        colorRamp = colors.copyOf()
        invalidate()
    }

    private val lastIndex get() = max(items.size - 1, 0)

    init {
        isClickable = true
        isFocusable = true
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.IconSlider, defStyleAttr, 0)
        try {
            trackHeight = a.getDimension(R.styleable.IconSlider_sliderTrackHeight, trackHeight)
            trackInset = a.getDimension(R.styleable.IconSlider_sliderTrackInset, trackInset)
            tooltipGap = a.getDimension(R.styleable.IconSlider_sliderTooltipGap, tooltipGap)
            trackColor = a.getColor(R.styleable.IconSlider_sliderTrackColor, trackColor)
            iconColor = a.getColor(R.styleable.IconSlider_sliderIconColor, iconColor)
            colorRamp = intArrayOf(
                a.getColor(R.styleable.IconSlider_sliderStartColor, colorRamp.first()),
                a.getColor(R.styleable.IconSlider_sliderEndColor, colorRamp.last())
            )
        } finally {
            a.recycle()
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (items.size < 2) return
        val pos = thumbPos.coerceIn(0f, lastIndex.toFloat())
        val disabledDimming = if (isEnabled) 1f else 0.4f
        val color = sampleColorAt(pos / lastIndex)
        val onColor = Oklab.onColor(color)
        val thumbX = slotX(pos)

        // Draw track
        paint.shader = null
        paint.color = trackColor
        canvas.drawRoundRect(trackRect, trackHeight / 2f, trackHeight / 2f, paint)

        // Draw dividers
        paint.color = ColorUtils.setAlphaComponent(iconColor, (60 * disabledDimming).toInt())
        val halfDividerHeight = trackHeight * 0.13f
        for (i in 0 until lastIndex) {
            val x = slotX(i + 0.5f)
            canvas.drawLine(x, centerY - halfDividerHeight, x, centerY + halfDividerHeight, paint)
        }

        // Draw icons on track
        items.forEachIndexed { i, item ->
            drawIcon(canvas, item.icon, slotX(i.toFloat()), iconColor, disabledDimming, 1f)
        }

        // Draw thumb colored base
        paint.color = ColorUtils.setAlphaComponent(color, (255 * disabledDimming).toInt())
        val thumbDrawnRadius = thumbRadius * (1f + 0.05f * press)
        canvas.drawCircle(thumbX, centerY, thumbDrawnRadius, paint)

        // Handle thumb icon transition between 2 values
        val min = floor(pos).toInt()
        val max = ceil(pos).toInt()
        val swapProgress = smoothStep(pos - min, 0.36f, 0.64f)
        val scale = 1f - (1f - abs(swapProgress * 2f - 1f)) * 0.25f
        drawIcon(canvas, items[min].icon, thumbX, onColor, (1f - swapProgress) * disabledDimming, scale)
        if (max != min) drawIcon(canvas, items[max].icon, thumbX, onColor, swapProgress * disabledDimming, scale)

        // Update the tooltip if it is being displayed
        if (tooltipAttached) updateTooltip(thumbX, color, onColor)
    }

    private fun drawIcon(canvas: Canvas, drawable: Drawable, cx: Float, tint: Int, alpha: Float, scale: Float) {
        if (alpha <= 0.02f) return
        val iconSizeRadius = (iconSize * scale / 2f).roundToInt()
        val d = drawable.constantState?.newDrawable()?.mutate() ?: drawable     // TODO: Handle this differently to prevent copying drawables on every draw call (changing without copy affects already drawn drawables of same instance)
        d.setBounds(-iconSizeRadius, -iconSizeRadius, iconSizeRadius, iconSizeRadius)
        d.setTint(tint)
        d.alpha = (alpha * 255).roundToInt()
        canvas.withTranslation(cx, centerY) {
            d.draw(this)
        }
    }

    private fun smoothStep(value: Float, from: Float, to: Float): Float {
        val x = ((value - from) / (to - from)).coerceIn(0f, 1f)
        return x.pow(2) * (3f - 2f * x)
    }

    private fun sampleColorAt(value: Float): Int {
        val indexFraction = value.coerceIn(0f, 1f) * (colorRamp.size - 1)
        val index = floor(indexFraction).toInt().coerceAtMost(colorRamp.size - 2)
        return Oklab.mix(colorRamp[index], colorRamp[index + 1], indexFraction - index)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = (paddingTop + trackHeight + paddingBottom).roundToInt()
        val width = (paddingLeft + paddingRight + trackHeight * max(items.size, 3)).roundToInt()
        setMeasuredDimension(resolveSize(width, widthMeasureSpec), resolveSize(height, heightMeasureSpec))
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        val available = height - paddingTop - paddingBottom
        val top = paddingTop + (available - trackHeight) / 2f
        trackRect.set(paddingLeft.toFloat(), top, (width - paddingRight).toFloat(), top + trackHeight)
        centerY = trackRect.centerY()
        thumbRadius = trackHeight / 2f - trackInset
        iconSize = thumbRadius * 1.15f
        firstSlotX = trackRect.left + trackInset + thumbRadius
        slotStep = if (items.size < 2) 0f else (trackRect.width() - (trackInset + thumbRadius) * 2f) / lastIndex
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled || items.size < 2) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)    // TODO: Only do this for primarily horizontal scroll events, e.g. permit vertical scrolling
                removeCallbacks(hideTooltip)

                touchStartX = event.x
                isPressed = true
                animatePress(1f)
                animateTooltip(1f)

                val isThumbPressed = abs(event.x - slotX(thumbPos)) <= thumbRadius * 1.6f
                val targetSelectedIndex = if (isThumbPressed) -1 else positionFor(event.x).roundToInt()
                snapToTouch(event, targetSelectedIndex)
            }

            MotionEvent.ACTION_MOVE -> {
                if (!dragging && abs(event.x - touchStartX) > touchSlop) snapToTouch(event)

                if (dragging) {
                    lastMovePosition = event.x
                    thumbPos = positionFor(event.x + grabOffset)
                    selectedIndex = thumbPos.roundToInt()
                    invalidate()
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)

                if (event.actionMasked == MotionEvent.ACTION_UP && !dragging) performClick()

                // Snap to the target value
                if (dragging && grabAnimator != null) {
                    grabAnimator?.addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            selectedIndex = positionFor(lastMovePosition).roundToInt()
                            animateThumb(selectedIndex.toFloat())
                            animatePress(0f)
                        }
                    })
                }
                else {
                    animateThumb(selectedIndex.toFloat())
                    animatePress(0f)
                }

                dragging = false
                grabOffset = 0f
                isPressed = false
                postDelayed(hideTooltip, 350)
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun snapToTouch(event: MotionEvent, targetSelectedIndex: Int = -1) {
        dragging = targetSelectedIndex == -1
        thumbAnimator?.cancel()
        grabOffset = slotX(thumbPos) - event.x
        lastMovePosition = event.x
        animateGrab(targetSelectedIndex)
    }

    private fun slotX(position: Float) = firstSlotX + slotStep * position
    private fun positionFor(x: Float) = if (slotStep == 0f) 0f else ((x - firstSlotX) / slotStep).coerceIn(0f, lastIndex.toFloat())

    private fun animateThumb(to: Float) {
        thumbAnimator?.cancel()
        if (thumbPos == to) return
        val animator = ValueAnimator.ofFloat(thumbPos, to)
        animator.duration = 320
        animator.interpolator = OvershootInterpolator(1.8f)
        animator.addUpdateListener {
            thumbPos = it.animatedValue as Float
            invalidate()
        }
        animator.start()
        thumbAnimator = animator
    }

    private fun animateGrab(targetSelectedIndex: Int = -1) {
        grabAnimator?.cancel()
        // TODO: Make fast flings look less jittery when fling and animation go in opposite direction (both towards one another)
        val animator = fadeAnimator(grabOffset, 0f, 70) {
            grabOffset = it
            thumbPos = positionFor(lastMovePosition + grabOffset)
            selectedIndex = if (targetSelectedIndex == -1) thumbPos.roundToInt() else targetSelectedIndex
            invalidate()
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                grabAnimator = null
            }
        })
        grabAnimator = animator
    }

    private fun animatePress(to: Float) {
        pressAnimator?.cancel()
        pressAnimator = fadeAnimator(press, to, if (to > 0f) 180 else 220) {
            press = it
        }
    }

    private fun animateTooltip(to: Float) {
        tooltipAnimator?.cancel()
        if (to > 0f) attachTooltip()
        val animator = fadeAnimator(animationProgressTooltip, to, if (to > 0f) 160 else 220) {
            animationProgressTooltip = it
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (animationProgressTooltip <= 0.01f) detachTooltip()
            }
        })
        tooltipAnimator = animator
    }

    private fun fadeAnimator(from: Float, to: Float, ms: Long, apply: (Float) -> Unit): ValueAnimator {
        val animator = ValueAnimator.ofFloat(from, to)
        animator.duration = ms
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener {
            apply(it.animatedValue as Float)
            invalidate()
        }
        animator.start()
        return animator
    }

    private fun updateTooltip(x: Float, backgroundColor: Int, textColor: Int) {
        val host = tooltipHost ?: return
        tooltipDrawable.label = items[selectedIndex].label
        tooltipDrawable.backgroundColor = backgroundColor
        tooltipDrawable.textColor = textColor
        tooltipDrawable.progress = animationProgressTooltip

        getLocationInWindow(locSelf)
        host.getLocationInWindow(locHost)
        val dx = locSelf[0] - locHost[0]
        val dy = locSelf[1] - locHost[1]

        val tooltipWidth = tooltipDrawable.intrinsicWidth
        val tooltipHeight = tooltipDrawable.intrinsicHeight
        val left = (dx + x - tooltipWidth / 2f).roundToInt().coerceIn(0, max(0, host.width - tooltipWidth))
        val above = dy + (centerY - thumbRadius - tooltipGap).roundToInt() - tooltipHeight
        val flipped = above < 0
        val top = if (flipped) dy + (centerY + thumbRadius + tooltipGap).roundToInt() else above

        tooltipDrawable.flipped = flipped
        tooltipDrawable.setBounds(left, top, left + tooltipWidth, top + tooltipHeight)
        tooltipDrawable.invalidateSelf()
    }

    private fun attachTooltip() {
        if (tooltipAttached) return
        val host = tooltipHost ?: findTooltipHost()?.also { tooltipHost = it } ?: return
        host.overlay.add(tooltipDrawable)
        tooltipAttached = true
    }

    private fun detachTooltip() {
        if (!tooltipAttached) return
        tooltipHost?.overlay?.remove(tooltipDrawable)
        tooltipAttached = false
    }

    private fun findTooltipHost(): ViewGroup? {
        var v: View = this
        while (true) {
            if (v.id == android.R.id.content && v is ViewGroup) return v
            val parent = v.parent
            if (parent is View) v = parent else break
        }
        return v as? ViewGroup
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        tooltipHost = findTooltipHost()
        viewTreeObserver.addOnScrollChangedListener(scrollListener)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(hideTooltip)
        listOf(thumbAnimator, pressAnimator, tooltipAnimator).forEach { it?.cancel() }
        viewTreeObserver.takeIf { it.isAlive }?.removeOnScrollChangedListener(scrollListener)
        detachTooltip()
        tooltipHost = null
        super.onDetachedFromWindow()
    }

    private inner class TooltipDrawable : Drawable() {
        private val tooltipRect = RectF()
        var label: CharSequence = ""
        var backgroundColor = Color.WHITE
        var textColor = Color.BLACK
        var progress = 0f
        var flipped = false

        override fun draw(canvas: Canvas) {
            if (progress <= 0.01f || label.isEmpty()) return

            val alpha = (255 * progress).toInt()
            val scale = 0.85f + 0.15f * progress
            val pivotY = if (flipped) bounds.top - tooltipGap else bounds.bottom + tooltipGap
            val baseline = bounds.exactCenterY() - (textPaint.descent() + textPaint.ascent()) / 2f

            tooltipPaint.color = ColorUtils.setAlphaComponent(backgroundColor, alpha)
            textPaint.color = ColorUtils.setAlphaComponent(textColor, alpha)

            canvas.withScale(scale, scale, bounds.exactCenterX(), pivotY) {
                tooltipRect.set(bounds)
                drawRoundRect(tooltipRect, bounds.height() / 2f, bounds.height() / 2f, tooltipPaint)
                drawText(label.toString(), bounds.exactCenterX(), baseline, textPaint)
            }
        }

        override fun getIntrinsicWidth() = (textPaint.measureText(label.toString()) + tooltipPadH * 2).roundToInt()
        override fun getIntrinsicHeight() = (textPaint.fontSpacing + tooltipPadV * 2).roundToInt()

        override fun setAlpha(alpha: Int) = Unit
        override fun setColorFilter(colorFilter: ColorFilter?) = Unit

        @Deprecated("Deprecated in Java")
        override fun getOpacity() = PixelFormat.TRANSLUCENT
    }

    data class Item(val icon: Drawable, val label: CharSequence)
}
