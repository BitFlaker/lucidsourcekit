package com.bitflaker.lucidsourcekit.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.utils.Tools.manipulateAlpha
import com.bitflaker.lucidsourcekit.utils.attrColor
import com.bitflaker.lucidsourcekit.utils.spToPx

class TextLegend @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val paintBackground = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
        color = context.attrColor(R.attr.colorSecondary)
    }
    private val paintText = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 18.spToPx.toFloat()
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private var legendPaints: List<Paint> = listOf()
    private var legendBackgroundPaints: List<Paint> = listOf()
    private var primaryTextColor = context.attrColor(R.attr.secondaryTextColor)
    private var secondaryTextColor = context.attrColor(R.attr.tertiaryTextColor)
    private var colors: IntArray = intArrayOf()
    private var labels: Array<String> = arrayOf()
    private var symbolRectPos: Array<RectF> = arrayOf()
    private var textBounds = Rect()
    private var currentSelectedIndex = -1

    fun setData(labels: Array<String>, colors: IntArray) {
        this.colors = colors
        this.labels = labels

        // Generate paints for the active color
        legendPaints = colors.map { c ->
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
                color = c
            }
        }.toList()

        // Generate paints for the background color
        legendBackgroundPaints = colors.map { c ->
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
                color = manipulateAlpha(c, 0.3f)
            }
        }.toList()

        println("SET")

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw the background gradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintBackground)

        // Draw labels and highlights
        for (i in labels.indices) {
            // Draw highlight for the current value
            if (i == currentSelectedIndex) {
                canvas.drawRoundRect(
                    symbolRectPos[i],
                    height / 2.0f,
                    height / 2.0f,
                    legendPaints[i % legendPaints.size]
                )
            }

            // Draw the label
            paintText.color = if (i != currentSelectedIndex) secondaryTextColor else primaryTextColor
            paintText.getTextBounds(labels[i], 0, labels[i].length, textBounds)
            canvas.drawText(
                labels[i],
                symbolRectPos[i].centerX(),
                symbolRectPos[i].centerY() - textBounds.exactCenterY(),
                paintText
            )
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec).toFloat()
        val height = MeasureSpec.getSize(heightMeasureSpec).toFloat()

        // Calculate content positions
        symbolRectPos = labels.indices.map {
            val xFrom = it / labels.size.toFloat() * width
            val xTo = (it + 1) / labels.size.toFloat() * width
            RectF().apply {
                set(xFrom, 0f, xTo, height)
            }
        }.toTypedArray()

        // Set background paint shader
        paintBackground.setShader(LinearGradient(
            0f,
            0f,
            width,
            0f,
            LineGraph.manipulateAlphaArray(colors, 0.3f),
            (colors.indices).map { it.toFloat() / (colors.size - 1) }.toFloatArray(),
            Shader.TileMode.CLAMP
        ))

        invalidate()
    }

    fun setCurrentSelectedIndex(currentSelectedIndex: Int) {
        this.currentSelectedIndex = currentSelectedIndex
        invalidate()
    }
}

