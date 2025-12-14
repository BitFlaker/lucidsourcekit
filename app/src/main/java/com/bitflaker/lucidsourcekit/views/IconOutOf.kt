package com.bitflaker.lucidsourcekit.views

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.utils.attrColor
import com.bitflaker.lucidsourcekit.utils.color
import com.bitflaker.lucidsourcekit.utils.dpToPx
import com.bitflaker.lucidsourcekit.utils.getBitmap
import com.bitflaker.lucidsourcekit.utils.getDimension
import com.bitflaker.lucidsourcekit.utils.getTextBounds
import com.bitflaker.lucidsourcekit.utils.obtainStyledAttributes
import com.bitflaker.lucidsourcekit.utils.resolveDrawableBitmap
import com.bitflaker.lucidsourcekit.utils.spToPx
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class IconOutOf @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    @ColorInt
    private var mainColor: Int = context.color(R.color.pastel_orange)
    private var declarations = attrs?.obtainStyledAttributes(context, R.styleable.IconOutOf)

    // Pre-allocated fields
    private var textBounds = Rect()
    private var textBoundsOf = Rect()
    private var textBoundsDescription = Rect()
    private var viewBounds = RectF()

    // Pre-configured paints
    private val dataLabelPrimaryPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
        textSize = declarations.getDimension(R.styleable.IconOutOf_textSizeValue) ?: 16f.spToPx
        color = context.attrColor(R.attr.primaryTextColor)
        textAlign = Paint.Align.LEFT
        isFakeBoldText = true
        textSize = 16f.spToPx
    }
    private val dataLabelSecondaryPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
        textSize = declarations.getDimension(R.styleable.IconOutOf_textSizeValueOf) ?: 12f.spToPx
        color = context.attrColor(R.attr.secondaryTextColor)
        textAlign = Paint.Align.LEFT
        isFakeBoldText = false
        textSize = 12f.spToPx
    }
    private val dataLinePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
        strokeWidth = declarations.getDimension(R.styleable.IconOutOf_lineWidth) ?: 2f.dpToPx
        color = mainColor
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 4f.dpToPx
    }
    private val dataLineTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
        strokeWidth = declarations.getDimension(R.styleable.IconOutOf_lineWidth) ?: 2f.dpToPx
        color = context.attrColor(R.attr.colorSurfaceContainer)
        style = Paint.Style.STROKE
        strokeWidth = 4f.dpToPx
    }

    // View specific fields
    var icon = declarations.getBitmap(R.styleable.IconOutOf_icon, mainColor, 18.dpToPx)

    var description = ""
        set(value) {
            field = value
            dataLabelSecondaryPaint.getTextBounds(field, textBoundsDescription)
        }

    var value = 0
        set(value) {
            field = value
            dataLabelPrimaryPaint.getTextBounds(field.toString(), textBounds)
        }

    var maxValue = 0
        set(value) {
            field = value
            dataLabelSecondaryPaint.getTextBounds("/$field", textBoundsOf)
        }

    // Dimensional
    private var padding = 0f
    private var diameter = declarations.getDimension(R.styleable.IconOutOf_diameter) ?: 48f.dpToPx
    private var textMargin = declarations.getDimension(R.styleable.IconOutOf_textSpacing) ?: 10f.dpToPx
    private var textOfMargin = declarations.getDimension(R.styleable.IconOutOf_textOfSpacing) ?: 4f.dpToPx
    private var descriptionSpacing = 6f.dpToPx


    init {
        description = declarations?.getString(R.styleable.IconOutOf_description) ?: ""
        value = 0
        maxValue = 0
        declarations?.recycle()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw the track
        canvas.drawArc(viewBounds, 270f, 360f, false, dataLineTrackPaint)

        // If there is something to draw, draw the progress bar
        if (maxValue > 0) {
            canvas.drawArc(viewBounds, 270f, 360 * value / maxValue.toFloat(), false, dataLinePaint)
        }

        // Draw the icon if one was provided
        icon?.let {
            canvas.drawBitmap(it, padding + diameter / 2f - it.width / 2.0f - 1f.dpToPx, padding + diameter / 2f - it.height / 2f - 1f.dpToPx, dataLinePaint)
        }

        // Get the y position for the text to draw
        val valueTextPositionY = padding + diameter / 2f - textBounds.exactCenterY() - textBoundsDescription.height() / 2f - descriptionSpacing / 2f

        // Draw the text for the actual value
        canvas.drawText(
            this.value.toString(),
            padding * 2 + diameter + textMargin,
            valueTextPositionY,
            dataLabelPrimaryPaint
        )

        // Draw the separator slash
        canvas.drawText(
            "/ " + this.maxValue,
            padding * 2 + diameter + textMargin + textBounds.width() + textOfMargin,
            valueTextPositionY,
            dataLabelSecondaryPaint
        )

        // Draw the max value
        canvas.drawText(
            description,
            padding * 2 + diameter + textMargin,
            valueTextPositionY + textBoundsDescription.height() + descriptionSpacing,
            dataLabelSecondaryPaint
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        padding = dataLinePaint.strokeWidth / 2.0f
        viewBounds.set(padding, padding, diameter, diameter)

        // Calculate the minimum required size of the view
        val maxTextWidth = max(textBounds.width().toFloat() + textOfMargin + textBoundsOf.width().toFloat(), textBoundsDescription.width().toFloat())
        val minWidth = ceil(padding + diameter + padding + textMargin + maxTextWidth + 4.dpToPx).toInt()
        val minHeight = ceil(padding + diameter).toInt()

        // Get required width
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        var width = when (MeasureSpec.getMode(widthMeasureSpec)) {
            MeasureSpec.EXACTLY -> max(widthSize, minWidth)
            MeasureSpec.AT_MOST -> min(widthSize, minWidth)
            else -> minWidth
        }

        // Get required height
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        var height = when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.EXACTLY -> max(heightSize, minHeight)
            MeasureSpec.AT_MOST -> min(heightSize, minHeight)
            else -> minHeight
        }

        // Comply with minimum width and height
        height = max(minimumHeight, height)
        width = max(minimumWidth, width)

        // Apply the measured size
        setMeasuredDimension(width, height)
    }
}
