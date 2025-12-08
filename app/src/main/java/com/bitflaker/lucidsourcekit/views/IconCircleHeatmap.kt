package com.bitflaker.lucidsourcekit.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.utils.Tools.getMidnightCalendar
import com.bitflaker.lucidsourcekit.utils.Tools.getMidnightMillis
import com.bitflaker.lucidsourcekit.utils.Tools.manipulateAlpha
import com.bitflaker.lucidsourcekit.utils.attrColor
import com.bitflaker.lucidsourcekit.utils.dpToPx
import com.bitflaker.lucidsourcekit.utils.spToPx
import java.util.Calendar
import java.util.Collections
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.math.roundToInt

class IconCircleHeatmap : View {
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context) : super(context)

    private val startAngle: Float = 270f
    private val textBounds = Rect()
    private val viewBounds = RectF()

    @ColorInt
    private var textColorTertiary = context.attrColor(R.attr.tertiaryTextColor)
    @ColorInt
    private var lineColor = context.attrColor(R.attr.colorSecondary)
    @ColorInt
    private var colors: IntArray = getColorShades(4)

    private val timestampCounts = HashMap<Long, Int>()
    private var maxValue = 0

    private val trackWidth = 12f.dpToPx
    private var labelTextSize = 11f.spToPx
    private var textMargin = 4.dpToPx

    private val dataLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
        color = textColorTertiary
        textSize = labelTextSize
        textAlign = Paint.Align.LEFT
        isFakeBoldText = true
    }
    private val dataTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
        color = context.attrColor(R.attr.colorSurfaceContainer)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = trackWidth
    }
    private val dataLinePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
        color = lineColor
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
        strokeWidth = trackWidth
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Get the smallest diameter to fully fit into the view
        val strokePadding = (dataTrackPaint.strokeWidth / 2).toInt()
        val drawingHeight = height - strokePadding * 2
        val drawingWidth = width - strokePadding * 2
        val diameter = min(drawingHeight, drawingWidth)

        // Get the view bounds within the available space to center the view
        val horizontalPadding = ((width - diameter) / 2).toFloat()
        val verticalPadding = ((height - diameter) / 2).toFloat()
        viewBounds.set(horizontalPadding, verticalPadding, diameter + horizontalPadding, diameter + verticalPadding)

        // Draw the track
        canvas.drawArc(viewBounds, startAngle, 360f, false, dataTrackPaint)

        // Init start values
        val calendar = getMidnightCalendar()
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val angleStepSweep = 360.0f / (24 * 2)
        var currentAngle = 0.0f

        // Step through the full day in 30 minute segments
        while (calendar.get(Calendar.DAY_OF_MONTH) == day) {
            val hourMS = TimeUnit.HOURS.toMillis(calendar.get(Calendar.HOUR_OF_DAY).toLong())
            val minuteMS = TimeUnit.MINUTES.toMillis(calendar.get(Calendar.MINUTE).toLong())
            val count = timestampCounts.getOrDefault(hourMS + minuteMS, 0)

            // Draw a colored segment with the appropriate color if the app open count
            // is greater than 0 in the current 30 minute segment
            if (count > 0) {
                val angle = startAngle + currentAngle
                val relativePercent = 100 * count / maxValue.toFloat()
                val colorIndexPercentStep = 100.0f / (colors.size - 1)
                val colorIndex = (relativePercent / colorIndexPercentStep).roundToInt()
                dataLinePaint.setColor(colors[colorIndex])
                canvas.drawArc(viewBounds, angle, angleStepSweep, false, dataLinePaint)
            }

            // Proceed to the next 30 minute step segments
            calendar.add(Calendar.MINUTE, 30)
            currentAngle += angleStepSweep
        }

        // Draw hour label 0 in top center position
        dataLabelPaint.getTextBounds("0", 0, "0".length, textBounds)
        val time0Width = textBounds.width()
        val time0Height = textBounds.height()
        canvas.drawText(
            "0",
            horizontalPadding + diameter / 2.0f - time0Width / 2.0f,
            (verticalPadding + textMargin + strokePadding + time0Height),
            dataLabelPaint
        )

        // Draw hour label 6 in bottom center position
        dataLabelPaint.getTextBounds("6", 0, "6".length, textBounds)
        val time6Width = textBounds.width()
        val time6Height = textBounds.height()
        canvas.drawText(
            "6",
            (width - horizontalPadding - textMargin - time6Width - strokePadding),
            verticalPadding + diameter / 2.0f + time6Height / 2.0f,
            dataLabelPaint
        )

        // Draw hour label 12 in right position
        dataLabelPaint.getTextBounds("12", 0, "12".length, textBounds)
        val time12Width = textBounds.width()
        canvas.drawText(
            "12",
            horizontalPadding + diameter / 2.0f - time12Width / 2.0f,
            (height - verticalPadding - textMargin - strokePadding),
            dataLabelPaint
        )

        // Draw hour label 18 in left position
        dataLabelPaint.getTextBounds("18", 0, "18".length, textBounds)
        val time18Height = textBounds.height()
        canvas.drawText(
            "18",
            (horizontalPadding + textMargin + strokePadding),
            verticalPadding + diameter / 2.0f + time18Height / 2.0f,
            dataLabelPaint
        )
    }

    fun setTimestamps(timestamps: List<Long>) {
        val calendar = Calendar.getInstance()
        val midnight = getMidnightMillis()

        // Go though all timestamps and increase the counter for app openings for
        // the 30 minute segment every timestamp belongs to
        for (timestamp in timestamps) {

            // Get the hour the current timestamp belongs to
            calendar.setTimeInMillis(midnight + timestamp)
            var millis = TimeUnit.HOURS.toMillis(calendar.get(Calendar.HOUR_OF_DAY).toLong())

            // Check if the timestamp is between xx:45 and xx:15 (closer to the full hour), then add 0 min
            // otherwise if the timestamp is between xx:15 and xx:45 (closer to half an hour), then add 30 min.
            // E.g.: 11:53 is between 11:45 and 12:15, therefore 0 minutes will be added.
            //       11:26 is between 11:15 and 11:45, therefore 30 minutes will be added.
            millis += (((calendar.get(Calendar.MINUTE) + 15) % 60) / 30.0f).toLong() * 30 * 60 * 1000

            // Increment the timestamp counter or initialize it at 1
            timestampCounts[millis] = timestampCounts.getOrDefault(millis, 0) + 1
        }

        // Get the max value and update the color shades
        maxValue = Collections.max(timestampCounts.values)
        colors = getColorShades(min(4, maxValue))
    }

    private fun getColorShades(colorAmount: Int): IntArray {
        val colors = IntArray(colorAmount)
        colors[colorAmount - 1] = lineColor
        for (i in 0..<colorAmount - 1) {
            colors[i] = manipulateAlpha(lineColor, (i + 1) * (1.0f / colorAmount))
        }
        return colors
    }
}
