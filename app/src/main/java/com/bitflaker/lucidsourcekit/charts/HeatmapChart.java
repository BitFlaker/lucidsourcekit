package com.bitflaker.lucidsourcekit.charts;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.general.Tools;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

public class HeatmapChart extends View {
    private final Paint dataLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint dataLabelPaintOf = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint dataLabelPaintDescription = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint dataLinePaint = new Paint();
    private final Paint dataLineTrackPaint = new Paint();
    private String description = "";
    private Rect textBounds, textBoundsOf, textBoundsDescription;
    private Bitmap icon;
    private int diameter = 0;
    private int textMargin = 0;
    private int textOfMargin = 0;
    private float padding = 0;
    private int value = 0;
    private int maxValue = 0;
    @ColorInt
    private int mainColor;
    @ColorInt
    private int fullTileColor;
    @ColorInt
    private int[] tileColors;
    private int maxWeekdayLabelWidth;
    private int tileSize;
    private int tileRadius;
    private int gap;
    private int weekCount;
    private String[] weekdayLabels;
    private String[] calendarWeeks;
    private int maxCWTextHeight;
    private int dayOfWeekIndex;
    private HashMap<Long, Integer> timestampCounts = new HashMap<>();
    private OnWeekCountCalculated mListener;

    public HeatmapChart(Context context) {
        super(context);
        setup();
    }

    public HeatmapChart(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setup();
        setConfiguredValues(context, attrs);
    }

    public HeatmapChart(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup();
        setConfiguredValues(context, attrs);
    }

    private void setConfiguredValues(Context context, @Nullable AttributeSet attrs) {
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

    private void setup() {
        fullTileColor = Tools.getAttrColor(R.attr.colorPrimary, getContext().getTheme());
        initTileColors(4);
        mainColor = getResources().getColor(R.color.lighter_orange, getContext().getTheme());
        @ColorInt int defaultColor = getResources().getColor(R.color.white, getContext().getTheme());
        @ColorInt int trackColor = Tools.getAttrColor(R.attr.backgroundColor, getContext().getTheme());
        @ColorInt int secondaryTextColor = Tools.getAttrColor(R.attr.secondaryTextColor, getContext().getTheme());
        @ColorInt int tertiaryTextColor = Tools.getAttrColor(R.attr.tertiaryTextColor, getContext().getTheme());

//        icon = Tools.drawableToBitmap(ResourcesCompat.getDrawable(getContext().getResources(), R.drawable.ic_baseline_local_fire_department_24, getContext().getTheme()), mainColor, Tools.dpToPx(getContext(), 24));

        dataLinePaint.setAntiAlias(true);
        dataLinePaint.setStyle(Paint.Style.FILL);
        dataLinePaint.setColor(mainColor);

        dataLineTrackPaint.setAntiAlias(true);
        dataLineTrackPaint.setStyle(Paint.Style.FILL);
        dataLineTrackPaint.setColor(trackColor);

        dataLabelPaint.setColor(Tools.getAttrColor(R.attr.secondaryTextColor, getContext().getTheme()));
        dataLabelPaint.setTextAlign(Paint.Align.LEFT);
        dataLabelPaint.setFakeBoldText(true);
        dataLabelPaint.setAntiAlias(true);
        setFontSizeValue(11);

        dataLabelPaintOf.setColor(secondaryTextColor);
        dataLabelPaintOf.setTextAlign(Paint.Align.LEFT);
        dataLabelPaintOf.setFakeBoldText(false);
        dataLabelPaintOf.setAntiAlias(true);
        setFontSizeValueOf(12);

        dataLabelPaintDescription.setColor(secondaryTextColor);
        dataLabelPaintDescription.setTextAlign(Paint.Align.LEFT);
        dataLabelPaintDescription.setFakeBoldText(false);
        dataLabelPaintDescription.setAntiAlias(true);
        setFontSizeDescription(12);

        textBounds = new Rect();
        textBoundsOf = new Rect();
        textBoundsDescription = new Rect();

        setDiameter(40);
        setTextMargin(6);
        setTextOfMargin(2);

        setTextValue(18);
        setTextValueOf(26);
        setDescription("Daily streak");

        if(secondaryTextColor == 0) {
            dataLabelPaint.setColor(defaultColor);
            dataLabelPaintOf.setColor(defaultColor);
            dataLabelPaintDescription.setColor(defaultColor);
        }

        tileSize = Tools.dpToPx(getContext(), 20);
        tileRadius = Tools.dpToPx(getContext(), 2);
        gap = Tools.dpToPx(getContext(), 4);
        weekCount = 8;
        maxWeekdayLabelWidth = 0;
        weekdayLabels = new String[] { "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun" };

        for (String weekdayLabel : weekdayLabels) {
            dataLabelPaint.getTextBounds(weekdayLabel, 0, weekdayLabel.length(), textBounds);
            maxWeekdayLabelWidth = Math.max(textBounds.width(), maxWeekdayLabelWidth);
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        dayOfWeekIndex = calendar.get(Calendar.DAY_OF_WEEK) - 2;
        dayOfWeekIndex = dayOfWeekIndex == -1 ? 6 : dayOfWeekIndex;

        maxCWTextHeight = 0;
        calculateCalendarWeeks(calendar);
    }

    private void initTileColors(int colorAmount) {
        tileColors = new int[colorAmount];
        tileColors[colorAmount - 1] = fullTileColor;
        for (int i = 0; i < colorAmount - 1; i++) {
            tileColors[i] = Tools.manipulateAlpha(fullTileColor, (i + 1) * (1.0f / colorAmount));
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        drawDaysOfWeek(canvas);
        drawTiles(canvas);
        drawCalendarWeeks(canvas);
        drawLegend(canvas);
    }

    private void drawLegend(@NonNull Canvas canvas) {
        int legendTileSize = tileSize / 2;
        String lowLegendText = "Less";
        String highLegendText = "More";
        int maxHeight = legendTileSize;
        int lowTextWidth;
        int highTextWidth;
        int legendGap = Tools.dpToPx(getContext(), 2);
        int legendMarginEnd = Tools.dpToPx(getContext(), 8);

        dataLabelPaint.getTextBounds(lowLegendText, 0, lowLegendText.length(), textBounds);
        lowTextWidth = textBounds.width();
        maxHeight = Math.max(maxHeight, textBounds.height());
        dataLabelPaint.getTextBounds(highLegendText, 0, highLegendText.length(), textBounds);
        highTextWidth = textBounds.width();
        maxHeight = Math.max(maxHeight, textBounds.height());

        int legendWidth = lowTextWidth + legendGap * 2 + (tileColors.length + 1) * (legendTileSize + legendGap) + legendGap + highTextWidth + legendMarginEnd;
        int baseLineY = gap + maxCWTextHeight + gap + 7 * (tileSize + gap) + 2 * gap + maxHeight;
        canvas.drawText(lowLegendText, getWidth() - legendWidth, baseLineY, dataLabelPaint);

        int tileStartingPosX = getWidth() - legendWidth + lowTextWidth + legendGap * 2;
        for (int i = -1; i < tileColors.length; i++) {
            int tilePosX = tileStartingPosX + (i + 1) * (legendTileSize + legendGap);
            int tilePosEnd = tileStartingPosX + legendTileSize + (i + 1) * (legendTileSize + legendGap);
            canvas.drawRoundRect(tilePosX, baseLineY - legendTileSize, tilePosEnd, baseLineY, tileRadius, tileRadius, dataLineTrackPaint);
            if(i >= 0) {
                dataLinePaint.setColor(tileColors[i]);
                canvas.drawRoundRect(tilePosX, baseLineY - legendTileSize, tilePosEnd, baseLineY, tileRadius, tileRadius, dataLinePaint);
            }
        }

        canvas.drawText(highLegendText, getWidth() - highTextWidth - legendMarginEnd, baseLineY, dataLabelPaint);
    }

    private void drawCalendarWeeks(@NonNull Canvas canvas) {
        for (int i = weekCount - 1; i >= 0; i--) {
            String text = calendarWeeks[i];
            dataLabelPaint.getTextBounds(text, 0, text.length(), textBounds);
            canvas.drawText(text, maxWeekdayLabelWidth + 2 * gap + (i * (gap + tileSize)) + tileSize / 2.0f - textBounds.exactCenterX(), gap + maxCWTextHeight, dataLabelPaint);
        }
    }

    private void drawTiles(@NonNull Canvas canvas) {
//        int maxValue = 4;
//        Random rnd = new Random();

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, 24);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        for (int x = weekCount - 1; x >= 0; x--) {
            for (int y = 6; y >= 0; y--) {
                if(x == weekCount - 1 && y > dayOfWeekIndex) {  // the tile for the current day was drawn right now
                    continue;
                }
                calendar.add(Calendar.HOUR, -24);
                long millis = calendar.getTimeInMillis();
                int value = timestampCounts.getOrDefault(millis, 0);
                canvas.drawRoundRect(maxWeekdayLabelWidth + 2 * gap + (x * (gap + tileSize)), gap + maxCWTextHeight + gap + (y * (gap + tileSize)), maxWeekdayLabelWidth + 2 * gap + (x * gap) + (x + 1) * tileSize, gap + maxCWTextHeight + gap + (y * gap) + (y + 1) * tileSize, tileRadius, tileRadius, dataLineTrackPaint);
                if(value > 0) {
                    int index = (int) Math.round((100 * value / (float) maxValue) / (100.0f / (tileColors.length - 1)));
                    dataLinePaint.setColor(tileColors[index]);
                    canvas.drawRoundRect(maxWeekdayLabelWidth + 2 * gap + (x * (gap + tileSize)), gap + maxCWTextHeight + gap + (y * (gap + tileSize)), maxWeekdayLabelWidth + 2 * gap + (x * gap) + (x + 1) * tileSize, gap + maxCWTextHeight + gap + (y * gap) + (y + 1) * tileSize, tileRadius, tileRadius, dataLinePaint);
                }
            }
        }
    }

    private void drawDaysOfWeek(@NonNull Canvas canvas) {
        for (int i = 0; i < weekdayLabels.length; i++) {
            canvas.drawText(weekdayLabels[i], gap, gap + maxCWTextHeight + gap + tileSize / 2.0f - (textBounds.exactCenterY()) + (i * (gap + tileSize)), dataLabelPaint);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width;
        int height;

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        Rect minSize = getMinimumSize();

        if (widthMode == MeasureSpec.EXACTLY) {
            width = Math.max(widthSize, minSize.width());
        } else if (widthMode == MeasureSpec.AT_MOST) {
            width = Math.min(minSize.width(), widthSize);
        } else {
            width = minSize.width();
        }

        if (heightMode == MeasureSpec.EXACTLY) {
            height = Math.max(heightSize, minSize.height());
        } else if (heightMode == MeasureSpec.AT_MOST) {
            height = Math.min(minSize.height(), heightSize);
        } else {
            height = minSize.height();
        }

        height = getMinimumHeight() != 0 && getMinimumHeight() > height ? getMinimumHeight() : height;
        width = getMinimumWidth() != 0 && getMinimumWidth() > width ? getMinimumWidth() : width;

        setMeasuredDimension(width, height);

        if(mListener != null) {
            weekCount = (width - (gap + maxWeekdayLabelWidth + gap)) / (tileSize + gap);
            Calendar calendar = Calendar.getInstance();
            calendar.setFirstDayOfWeek(Calendar.MONDAY);
            calculateCalendarWeeks(calendar);
            mListener.onEvent(weekCount);
            invalidate();
        }
    }

    private void calculateCalendarWeeks(Calendar calendar) {
        calendarWeeks = new String[weekCount];
        for (int i = weekCount - 1; i >= 0; i--) {
            if(i < weekCount - 1) {
                calendar.add(Calendar.DAY_OF_YEAR, -7);
            }
            int weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR);
            String text = Integer.toString(weekOfYear);
            calendarWeeks[i] = text;
            dataLabelPaint.getTextBounds(text, 0, text.length(), textBounds);
            maxCWTextHeight = Math.max(maxCWTextHeight, textBounds.height());
        }
    }

    private Rect getMinimumSize() {
        return new Rect(0, 0, gap + maxWeekdayLabelWidth + gap + weekCount * (tileSize + gap), gap + tileSize * 7 + gap * 7 + maxCWTextHeight + gap);
    }

    public void setDiameter(int dp) {
        this.diameter = Tools.dpToPx(getContext(), dp);
    }

    public void setTextMargin(int dp) {
        this.textMargin = Tools.dpToPx(getContext(), dp);
    }

    public void setTextOfMargin(int dp) {
        this.textOfMargin = Tools.dpToPx(getContext(), dp);
    }

    public void setTextValue(int value) {
        this.value = value;
        dataLabelPaint.getTextBounds(Integer.toString(this.value), 0, Integer.toString(this.value).length(), textBounds);
    }

    public void setTextValueOf(int maxValue) {
        this.maxValue = maxValue;
        dataLabelPaintOf.getTextBounds("/" + this.maxValue, 0, ("/" + this.maxValue).length(), textBoundsOf);
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description;
        dataLabelPaintDescription.getTextBounds(this.description, 0, this.description.length(), textBoundsDescription);
    }

    public void setFontSizeValue(int sp) {
        dataLabelPaint.setTextSize((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics()));
    }

    public void setFontSizeValueOf(int sp) {
        dataLabelPaintOf.setTextSize((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics()));
    }

    public void setFontSizeDescription(int sp) {
        dataLabelPaintDescription.setTextSize((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics()));
    }

    public void setTimestamps(List<Long> timestamps) {
        Calendar calendar = Calendar.getInstance();
        for (Long timestamp : timestamps) {
            calendar.setTimeInMillis(timestamp);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            long millis = calendar.getTimeInMillis();
            int amount = timestampCounts.getOrDefault(millis, 0) + 1;
            timestampCounts.put(millis, amount);
        }
    }

    public interface OnWeekCountCalculated {
        void onEvent(int weekCount);
    }

    public void setOnWeekCountCalculatedListener(OnWeekCountCalculated listener) {
        this.mListener = listener;
    }
}
