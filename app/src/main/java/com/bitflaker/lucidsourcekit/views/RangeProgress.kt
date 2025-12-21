package com.bitflaker.lucidsourcekit.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.utils.Tools.manipulateAlpha
import com.bitflaker.lucidsourcekit.utils.attrColor
import com.bitflaker.lucidsourcekit.utils.spToPx
import com.bitflaker.lucidsourcekit.utils.toBitmap
import kotlin.Int
import kotlin.String
import kotlin.floatArrayOf
import kotlin.intArrayOf
import androidx.core.graphics.createBitmap
import com.bitflaker.lucidsourcekit.utils.getTextBounds
import kotlin.math.max
import kotlin.math.min

class RangeProgress @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    // Pre-configured paints
    private val dataLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
        color = context.attrColor(R.attr.primaryTextColor)
        isFakeBoldText = true
        textAlign = Paint.Align.LEFT
        textSize = 14f.spToPx
    }
    private val dataLinePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
        color = context.attrColor(R.attr.colorSurfaceContainer)
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 0f
    }
    private val iconGradientPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    }

    // Configurable view specific fields
    var label: String? = null
        set(value) {
            field = value
            textCenterY = value?.let {
                val labelBounds = Rect()
                dataLabelPaint.getTextBounds(value, labelBounds)
                labelBounds.exactCenterY()
            } ?: 0f
        }

    var icon: Drawable? = null
        set(value) {
            field = value
            if (value == null)  {
                iconBitmap = null
            }
            else {
                text = null
            }
        }

    var text: String? = null
        set(value) {
            field = value
            if (value != null) {
                icon = null
                if (value == "NaN") {
                    field = "-"
                }
            }
            dataLabelPaint.getTextBounds(text ?: "", textBounds)
        }

    var labelInsetX = 27f
    var iconInsetX = 5f

    // Internal values
    private var progressColors = intArrayOf(
        context.attrColor(R.attr.colorTertiary),
        context.attrColor(R.attr.colorSurfaceContainer)
    )
    private var textColors = intArrayOf(
        context.attrColor(R.attr.colorOnTertiary),
        manipulateAlpha(context.attrColor(R.attr.secondaryTextColor), 0.7f)
    )
    private var value = 0f
    private var xMax = 0f
    private var positions = floatArrayOf(0f, 0f)
    private var percentage = 0f
    private var iconBitmap: Bitmap? = null
    private var iconGradientBitmap = createBitmap(1, 1)
    private var iconGradientCanvas = Canvas(iconGradientBitmap)
    private val iconGradientMatrix = Matrix()
    private val textBounds = Rect()
    private var textCenterY = 0f
    private var iconSize: Int = 0
        set(value) {
            field = value
            iconBitmap = icon?.toBitmap(value)
            iconGradientBitmap = createBitmap(value, value)
            iconGradientCanvas = Canvas(iconGradientBitmap)
            iconGradientPaint.shader = LinearGradient(
                0f,
                0f,
                value.toFloat(),
                0f,
                textColors,
                floatArrayOf(0f, 0f),
                Shader.TileMode.CLAMP
            )
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val inset = height / 2.0f

        // Draw the progress and track line
        canvas.drawLine(
            inset,
            height / 2.0f,
            width - inset,
            height / 2.0f,
            dataLinePaint
        )

        // Draw the label if present
        label?.let {
            canvas.drawText(
                it,
                labelInsetX,
                height / 2.0f - textCenterY,
                dataLabelPaint
            )
        }

        // Draw the icon on the right end of the view if present
        iconBitmap?.let {
            // Get the x position of the start of the icon
            val xStart = width - iconSize - (height - iconSize) / 2.0f - iconInsetX

            // Get the percentage of the icon being on top of the filled progress and update the icon gradient
            val progressStopX = percentage * width
            val iconFillPercentage = min(iconSize.toFloat(), max(0f, progressStopX - xStart)) / iconSize
            updateIconGradient(it, iconFillPercentage)

            // Draw the icon vertically centered to the target x-position
            canvas.drawBitmap(
                iconGradientBitmap,
                xStart,
                (height - iconSize) / 2.0f,
                dataLabelPaint
            )
        }

        // Draw the text on the right end of the view if present
        text?.let {
            canvas.drawText(
                it,
                width - labelInsetX - textBounds.width(),
                height / 2.0f - textBounds.exactCenterY(),
                dataLabelPaint
            )
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec).toFloat()
        val height = MeasureSpec.getSize(heightMeasureSpec).toFloat()

        // Update paint strokes to new height
        dataLinePaint.strokeWidth = height

        // Update shaders to new width
        dataLinePaint.shader = getLineShader(width)
        dataLabelPaint.shader = getTextShader(width)

        // Update icon size to new view height
        iconSize = (height / 5.0f * 3.25).toInt()

        invalidate()
    }

    private fun getTextShader(width: Float): LinearGradient = LinearGradient(
        0f,
        0f,
        width,
        0f,
        textColors,
        positions,
        Shader.TileMode.MIRROR
    )

    private fun getLineShader(width: Float): LinearGradient = LinearGradient(
        0f,
        0f,
        width,
        0f,
        progressColors,
        positions,
        Shader.TileMode.MIRROR
    )

    private fun updateIconGradient(src: Bitmap, percentage: Float) {
        // Translate the matrix of the gradient to the percentage of the icon width on top of the progress
        iconGradientMatrix.reset()
        iconGradientMatrix.setTranslate(iconSize * percentage, 0f)
        (iconGradientPaint.shader as LinearGradient).setLocalMatrix(iconGradientMatrix)

        // Re-draw the icon to the bitmap with the updated gradient
        iconGradientCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        iconGradientCanvas.drawBitmap(src, 0f, 0f, null)
        iconGradientCanvas.drawRect(0f, 0f, iconSize.toFloat(), iconSize.toFloat(), iconGradientPaint)
    }

    fun setBackgroundAttrColor(colorRes: Int) {
        dataLinePaint.color = context.attrColor(colorRes)
        progressColors = intArrayOf(
            context.attrColor(R.attr.colorTertiary),
            dataLinePaint.color
        )

        invalidate()
    }

    fun setValue(value: Float, max: Float) {
        this.value = value
        this.xMax = max

        percentage = value / xMax
        if (xMax == 0f || value.isNaN()) {
            percentage = 0f
            text = "-"
        }

        positions[0] = percentage
        positions[1] = percentage
        dataLinePaint.shader = getLineShader(width.toFloat())
        dataLabelPaint.shader = getTextShader(width.toFloat())

        invalidate()
    }
}