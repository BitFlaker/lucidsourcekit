package com.bitflaker.lucidsourcekit.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.annotation.ColorInt
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.utils.Tools.manipulateAlpha
import com.bitflaker.lucidsourcekit.utils.attrColor
import com.bitflaker.lucidsourcekit.utils.dpToPx
import java.util.Calendar
import java.util.Collections
import kotlin.math.max
import kotlin.math.min

class HeatmapChart : View {
    private val dataLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val dataLinePaint = Paint()
    private val dataLineTrackPaint = Paint()
    private var textBounds: Rect? = null
    private var maxValue = 0

    @ColorInt
    private var mainColor = 0

    @ColorInt
    private var fullTileColor = 0

    @ColorInt
    private var tileColors: IntArray? = null
    private var maxWeekdayLabelWidth = 0
    private var tileSize = 0
    private var tileRadius = 0
    private var gap = 0
    private var weekCount = 0
    private val weekdayLabels: Array<String> = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    private lateinit var calendarWeeks: Array<String>
    private var maxCWTextHeight = 0
    private var dayOfWeekIndex = 0
    private val timestampCounts = HashMap<Long, Int>()
    private var legendTileSize = 0
    private val lowLegendText = "Less"
    private val highLegendText = "More"
    private var legendHeight = 0
    private var lowTextWidth = 0
    private var highTextWidth = 0
    private var legendGap = 0
    private var legendMarginEnd = 0
    private var legendMarginTop = 0
    var onWeekCountCalculatedListener: ((Int) -> Unit)? = null

    constructor(context: Context?) : super(context) {
        setup()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        setup()
        setConfiguredValues(context, attrs)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        setup()
        setConfiguredValues(context, attrs)
    }

    private fun setConfiguredValues(context: Context?, attrs: AttributeSet?) {
//        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.IconOutOf, 0, 0);
//        try {
//            dataLinePaint.setStrokeWidth(a.getDimension(R.styleable.IconOutOf_lineWidth, Tools.dpToPx(getContext(), 2)));
//            dataLineTrackPaint.setStrokeWidth(a.getDimension(R.styleable.IconOutOf_lineWidth, Tools.dpToPx(getContext(), 2)));
//            this.diameter = (int) a.getDimension(R.styleable.IconOutOf_diameter, Tools.dpToPx(getContext(), 40));
//            this.textMargin = (int) a.getDimension(R.styleable.IconOutOf_textSpacing, Tools.dpToPx(getContext(), 6));
//            this.textOfMargin = (int) a.getDimension(R.styleable.IconOutOf_textOfSpacing, Tools.dpToPx(getContext(), 2));
//            dataLabelPaint.setTextSize(a.getDimension(R.styleable.IconOutOf_textSizeValue, Tools.spToPx(getContext(), 16)));
//            dataLabelPaintOf.setTextSize(a.getDimension(R.styleable.IconOutOf_textSizeValueOf, Tools.spToPx(getContext(), 12)));
//            setDescription(a.getString(R.styleable.IconOutOf_description));
//            Drawable iconD = a.getDrawable(R.styleable.IconOutOf_icon);
//            if(iconD != null) {
//                icon = Tools.drawableToBitmap(iconD, mainColor, Tools.dpToPx(getContext(), 24));
//            }
//
//            dataLabelPaint.getTextBounds(Integer.toString(this.value), 0, Integer.toString(this.value).length(), textBounds);
//            dataLabelPaintOf.getTextBounds("/" + this.maxValue, 0, ("/" + this.maxValue).length(), textBoundsOf);
//        } finally {
//            a.recycle();
//        }
    }

    private fun setup() {
        fullTileColor = context.attrColor(R.attr.colorTertiary)
        mainColor = resources.getColor(R.color.lighter_orange, context.theme)
        @ColorInt val defaultColor = resources.getColor(R.color.white, context.theme)
        @ColorInt val trackColor = context.attrColor(R.attr.colorSurfaceContainer)
        @ColorInt val secondaryTextColor = context.attrColor(R.attr.secondaryTextColor)

        dataLinePaint.isAntiAlias = true
        dataLinePaint.style = Paint.Style.FILL
        dataLinePaint.color = mainColor

        dataLineTrackPaint.isAntiAlias = true
        dataLineTrackPaint.style = Paint.Style.FILL
        dataLineTrackPaint.color = trackColor

        dataLabelPaint.color = context.attrColor(R.attr.secondaryTextColor)
        dataLabelPaint.textAlign = Paint.Align.LEFT
        dataLabelPaint.isFakeBoldText = true
        dataLabelPaint.isAntiAlias = true
        setFontSizeValue(11)

        textBounds = Rect()

        if (secondaryTextColor == 0) {
            dataLabelPaint.color = defaultColor
        }

        tileSize = 20.dpToPx
        tileRadius = 2.dpToPx
        gap = 4.dpToPx
        weekCount = 8
        maxWeekdayLabelWidth = 0

        for (weekdayLabel in weekdayLabels) {
            dataLabelPaint.getTextBounds(weekdayLabel, 0, weekdayLabel.length, textBounds)
            maxWeekdayLabelWidth = max(textBounds!!.width(), maxWeekdayLabelWidth)
        }

        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        dayOfWeekIndex = calendar.get(Calendar.DAY_OF_WEEK) - 2
        dayOfWeekIndex = if (dayOfWeekIndex == -1) 6 else dayOfWeekIndex

        maxCWTextHeight = 0
        calculateCalendarWeeks(calendar)

        legendTileSize = tileSize / 2
        legendHeight = legendTileSize
        legendGap = 2.dpToPx
        legendMarginEnd = 8.dpToPx
        legendMarginTop = 4 * gap

        dataLabelPaint.getTextBounds(lowLegendText, 0, lowLegendText.length, textBounds)
        lowTextWidth = textBounds!!.width()
        legendHeight = max(legendHeight, textBounds!!.height())
        dataLabelPaint.getTextBounds(highLegendText, 0, highLegendText.length, textBounds)
        highTextWidth = textBounds!!.width()
        legendHeight = max(legendHeight, textBounds!!.height())
    }

    private fun initTileColors(colorAmount: Int) {
        tileColors = IntArray(colorAmount)
        tileColors!![colorAmount - 1] = fullTileColor
        for (i in 0..<colorAmount - 1) {
            tileColors!![i] = manipulateAlpha(fullTileColor, (i + 1) * (1.0f / colorAmount))
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (tileColors == null) return

        drawDaysOfWeek(canvas)
        drawTiles(canvas)
        drawCalendarWeeks(canvas)
        drawLegend(canvas)
    }

    private fun drawLegend(canvas: Canvas) {
        val legendWidth =
            lowTextWidth + legendGap * 2 + (tileColors!!.size + 1) * (legendTileSize + legendGap) + legendGap + highTextWidth + legendMarginEnd
        val baseLineY =
            gap + maxCWTextHeight + gap + 7 * (tileSize + gap) + legendMarginTop + legendHeight
        canvas.drawText(
            lowLegendText,
            (width - legendWidth).toFloat(),
            baseLineY.toFloat(),
            dataLabelPaint
        )

        val tileStartingPosX = width - legendWidth + lowTextWidth + legendGap * 2
        for (i in -1..<tileColors!!.size) {
            val tilePosX = tileStartingPosX + (i + 1) * (legendTileSize + legendGap)
            val tilePosEnd =
                tileStartingPosX + legendTileSize + (i + 1) * (legendTileSize + legendGap)
            canvas.drawRoundRect(
                tilePosX.toFloat(),
                (baseLineY - legendTileSize).toFloat(),
                tilePosEnd.toFloat(),
                baseLineY.toFloat(),
                tileRadius.toFloat(),
                tileRadius.toFloat(),
                dataLineTrackPaint
            )
            if (i >= 0) {
                dataLinePaint.color = tileColors!![i]
                canvas.drawRoundRect(
                    tilePosX.toFloat(),
                    (baseLineY - legendTileSize).toFloat(),
                    tilePosEnd.toFloat(),
                    baseLineY.toFloat(),
                    tileRadius.toFloat(),
                    tileRadius.toFloat(),
                    dataLinePaint
                )
            }
        }

        canvas.drawText(
            highLegendText,
            (width - highTextWidth - legendMarginEnd).toFloat(),
            baseLineY.toFloat(),
            dataLabelPaint
        )
    }

    private fun drawCalendarWeeks(canvas: Canvas) {
        for (i in weekCount - 1 downTo 0) {
            val text = calendarWeeks[i]
            dataLabelPaint.getTextBounds(text, 0, text.length, textBounds)
            canvas.drawText(
                text,
                maxWeekdayLabelWidth + 2 * gap + (i * (gap + tileSize)) + tileSize / 2.0f - textBounds!!.exactCenterX(),
                (gap + 7 * (tileSize + gap) + maxCWTextHeight).toFloat(),
                dataLabelPaint
            )
        }
    }

    private fun drawTiles(canvas: Canvas) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.HOUR, 24)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        for (x in weekCount - 1 downTo 0) {
            for (y in 6 downTo 0) {
                if (x != weekCount - 1 || y <= dayOfWeekIndex) {  // the tile for the current day was drawn right now
                    calendar.add(Calendar.HOUR, -24)
                    val millis = calendar.timeInMillis
                    val value = timestampCounts.getOrDefault(millis, 0)
                    canvas.drawRoundRect(
                        (maxWeekdayLabelWidth + 2 * gap + (x * (gap + tileSize))).toFloat(),
                        (gap + (y * (gap + tileSize))).toFloat(),
                        (maxWeekdayLabelWidth + 2 * gap + (x * gap) + (x + 1) * tileSize).toFloat(),
                        (gap + (y * gap) + (y + 1) * tileSize).toFloat(),
                        tileRadius.toFloat(),
                        tileRadius.toFloat(),
                        dataLineTrackPaint
                    )
                    if (value > 0) {
                        val index =
                            Math.round((100 * value / maxValue.toFloat()) / (100.0f / (tileColors!!.size - 1)))
                        dataLinePaint.color = tileColors!![index]
                        canvas.drawRoundRect(
                            (maxWeekdayLabelWidth + 2 * gap + (x * (gap + tileSize))).toFloat(),
                            (gap + (y * (gap + tileSize))).toFloat(),
                            (maxWeekdayLabelWidth + 2 * gap + (x * gap) + (x + 1) * tileSize).toFloat(),
                            (gap + (y * gap) + (y + 1) * tileSize).toFloat(),
                            tileRadius.toFloat(),
                            tileRadius.toFloat(),
                            dataLinePaint
                        )
                    }
                } else {
                    val circleRadius = 3.dpToPx
                    canvas.drawCircle(
                        maxWeekdayLabelWidth + 2 * gap + (x * (gap + tileSize)) + tileSize / 2.0f,
                        gap + (y * (gap + tileSize)) + tileSize / 2.0f,
                        circleRadius.toFloat(),
                        dataLineTrackPaint
                    )
                }
            }
        }
    }

    private fun drawDaysOfWeek(canvas: Canvas) {
        for (i in weekdayLabels.indices) {
            canvas.drawText(
                weekdayLabels[i],
                gap.toFloat(),
                gap + tileSize / 2.0f - (textBounds!!.exactCenterY()) + (i * (gap + tileSize)),
                dataLabelPaint
            )
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        var width: Int
        var height: Int

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val minSize = this.minimumSize

        if (widthMode == MeasureSpec.EXACTLY) {
            width = max(widthSize, minSize.width())
        } else if (widthMode == MeasureSpec.AT_MOST) {
            width = min(minSize.width(), widthSize)
        } else {
            width = minSize.width()
        }

        if (heightMode == MeasureSpec.EXACTLY) {
            height = max(heightSize, minSize.height())
        } else if (heightMode == MeasureSpec.AT_MOST) {
            height = min(minSize.height(), heightSize)
        } else {
            height = minSize.height()
        }

        height =
            if (minimumHeight != 0 && minimumHeight > height) minimumHeight else height
        width =
            if (minimumWidth != 0 && minimumWidth > width) minimumWidth else width

        setMeasuredDimension(width, height)

        if (onWeekCountCalculatedListener != null) {
            weekCount = (width - (gap + maxWeekdayLabelWidth + gap)) / (tileSize + gap)
            val calendar = Calendar.getInstance()
            calendar.firstDayOfWeek = Calendar.MONDAY
            calculateCalendarWeeks(calendar)
            onWeekCountCalculatedListener?.invoke(weekCount)
            invalidate()
        }
    }

    private val minimumSize: Rect
        get() = Rect(
            0,
            0,
            gap + maxWeekdayLabelWidth + gap + weekCount * (tileSize + gap),
            gap + tileSize * 7 + gap * 7 + maxCWTextHeight + gap + legendMarginTop + legendHeight + gap
        )

    private fun calculateCalendarWeeks(calendar: Calendar) {
        val days = Array(weekCount) { "" }
        for (i in weekCount - 1 downTo 0) {
            if (i < weekCount - 1) {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
            }
            val weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR)
            val text = weekOfYear.toString()
            days[i] = text
            dataLabelPaint.getTextBounds(text, 0, text.length, textBounds)
            maxCWTextHeight = max(maxCWTextHeight, textBounds!!.height())
        }
        calendarWeeks = days
    }

    fun setValueMax(maxValue: Int) {
        this.maxValue = maxValue
    }

    fun setFontSizeValue(sp: Int) {
        dataLabelPaint.textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            sp.toFloat(),
            resources.displayMetrics
        ).toInt().toFloat()
    }

    fun setTimestamps(timestamps: List<Long>) {
        val calendar = Calendar.getInstance()
        for (timestamp in timestamps) {
            calendar.timeInMillis = timestamp
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val millis = calendar.timeInMillis
            val amount = timestampCounts.getOrDefault(millis, 0) + 1
            timestampCounts[millis] = amount
        }
        setValueMax(if (!timestampCounts.isEmpty()) Collections.max(timestampCounts.values) else 4)
        initTileColors(min(4, this.maxValue))
        invalidate()
    }
}
