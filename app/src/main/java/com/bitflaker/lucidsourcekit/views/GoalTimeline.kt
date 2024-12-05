package com.bitflaker.lucidsourcekit.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.utils.Tools
import java.text.DateFormat
import java.util.Calendar
import kotlin.math.max
import kotlin.math.min

class GoalTimeline @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT)
    private val textBounds = Rect()
    private val midnightTimestamp = Tools.getMidnightTime()
    private val fullHeight = Tools.dpToPx(context, 768.0)
    private val totalDayTime = 1000 * 60 * 60 * 24f
    private var actualHeight = fullHeight
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
        color = Tools.getAttrColor(R.attr.primaryTextColor, context.theme)
        textAlign = Paint.Align.LEFT
        isAntiAlias = true
        textSize = Tools.spToPx(context, 11f).toFloat()
    }
    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Tools.getAttrColor(R.attr.colorPrimary, context.theme)
        strokeWidth = Tools.dpToPx(context, 4.0).toFloat()
        xfermode = null
        pathEffect = null
        strokeCap = Paint.Cap.ROUND
    }
    private val backgroundPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Tools.getAttrColor(R.attr.colorSurfaceContainer, context.theme)
        strokeWidth = Tools.dpToPx(context, 4.0).toFloat()
        xfermode = null
        pathEffect = null
        strokeCap = Paint.Cap.ROUND
    }
    private val tickPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Tools.getAttrColor(R.attr.colorPrimary, context.theme)
        strokeWidth = Tools.dpToPx(context, 1.0).toFloat()
        xfermode = null
        pathEffect = null
        strokeCap = Paint.Cap.ROUND
    }

    var achieved: ArrayList<Long> = arrayListOf()
        set(values) {
            field = ArrayList(values)
            field.sort()
            var lastDisplayTime = 0L
            for (i in field.indices) {
                field[i] -= midnightTimestamp
                if (i > 0 && field[i] - lastDisplayTime < (1000 * 60 * 30)) {
                    field[i] = 0
                }
                else {
                    lastDisplayTime = field[i]
                }
            }
            invalidate()
        }
    var indicatorRadius: Float = Tools.dpToPx(context, 6.0).toFloat()
    var shuffleInitTime: Long = 0
        set(initTimestamp) {
            field = initTimestamp - midnightTimestamp
            actualHeight = (fullHeight / totalDayTime * (totalDayTime - field)).toInt()
            requestLayout()
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val stepMillis = (height - backgroundPaint.strokeWidth) / (totalDayTime - shuffleInitTime)

        val currentDayProgress = Calendar.getInstance().timeInMillis - midnightTimestamp - shuffleInitTime
        val progressedSinceStart = stepMillis * currentDayProgress

        canvas.drawLine((width - backgroundPaint.strokeWidth) / 2f, backgroundPaint.strokeWidth / 2f, (width - backgroundPaint.strokeWidth) / 2f, height.toFloat() - backgroundPaint.strokeWidth / 2f, backgroundPaint)
        canvas.drawLine((width - paint.strokeWidth) / 2f, paint.strokeWidth / 2f, (width - paint.strokeWidth) / 2f, progressedSinceStart + paint.strokeWidth / 2f, paint)

        var skipped = 0
        for (i in achieved.indices) {
            if (achieved[i] == 0L) {
                skipped++
                continue
            }
            val pointLocation = stepMillis * (achieved[i] - shuffleInitTime)
            val yValue = pointLocation + paint.strokeWidth / 2f

            // Draw circle indicator
            canvas.drawCircle((width - paint.strokeWidth) / 2f, yValue, indicatorRadius, paint)

            // Draw line to text
            val direction = if ((i - skipped) % 2 == 0) 1 else -1
            val spacing = Tools.dpToPx(context, 8.0)
            val lineStartX = (width - paint.strokeWidth) / 2f + (indicatorRadius + tickPaint.strokeWidth / 2f + spacing) * direction
            val lineStopX = lineStartX + Tools.dpToPx(context, 16.0) * direction
            canvas.drawLine(lineStartX, yValue, lineStopX, yValue, tickPaint)

            // Draw time
            val time = timeFormat.format(achieved[i])
            textPaint.color = Tools.getAttrColor(R.attr.primaryTextColor, context.theme)
            textPaint.isFakeBoldText = true
            textPaint.getTextBounds(time, 0, time.length, textBounds)
            val timeTextOffset = if (direction == -1) textBounds.width() else 0
            canvas.drawText(time, lineStopX + (spacing + timeTextOffset) * direction, yValue + textBounds.height() / 2f, textPaint)

            val offsetTop = textBounds.height() / 2f
            val firstGoal = "achieved ${countAchievedGoals(i)} goals"
            textPaint.color = Tools.getAttrColor(R.attr.secondaryTextColor, context.theme)
            textPaint.isFakeBoldText = false
            textPaint.getTextBounds(firstGoal, 0, firstGoal.length, textBounds)
            val goalTextOffset = if (direction == -1) textBounds.width() else 0
            canvas.drawText(firstGoal, lineStopX + (spacing + goalTextOffset) * direction, yValue + offsetTop + textBounds.height(), textPaint)
        }
    }

    private fun countAchievedGoals(achievedIndex: Int): Int {
        var count = 1
        while (achieved.size > achievedIndex + count && achieved[achievedIndex + count] == 0L) {
            count++
        }
        return count
    }

    private val graphTime
        get() = totalDayTime - shuffleInitTime

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val minSize: Rect = getMinimumSize()

        var width = when (widthMode) {
            MeasureSpec.EXACTLY -> max(widthSize, minSize.width())
            MeasureSpec.AT_MOST -> min(minSize.width(), widthSize)
            else -> minSize.width()
        }

        var height = when (heightMode) {
            MeasureSpec.EXACTLY -> max(heightSize, minSize.height())
            MeasureSpec.AT_MOST -> min(minSize.height(), heightSize)
            else -> minSize.height()
        }

        height = if (minimumHeight != 0 && minimumHeight > height) minimumHeight else height
        width = if (minimumWidth != 0 && minimumWidth > width) minimumWidth else width

        setMeasuredDimension(width, height)
    }

    private fun getMinimumSize(): Rect {
        return Rect(0, 0, 0, actualHeight)
    }
}
