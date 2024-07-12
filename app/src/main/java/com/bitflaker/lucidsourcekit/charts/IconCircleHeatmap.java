package com.bitflaker.lucidsourcekit.charts;

import android.content.Context;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class IconCircleHeatmap extends View {
    private final Paint dataLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint dataTrackPaint = new Paint();
    private final Paint dataLinePaint = new Paint();
    private final HashMap<Long, Integer> timestampCounts = new HashMap<>();
    private Rect textBounds;
    @ColorInt
    private int[] colors;
    @ColorInt
    private int lineColor;
    private int maxValue;
    @ColorInt
    private int textColorTertiary;
    @ColorInt
    private int textColorSecondary;
    @ColorInt
    private int labelTextSize;
    private int textMargin;

    public IconCircleHeatmap(Context context) {
        super(context);
        setup();
    }

    public IconCircleHeatmap(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public IconCircleHeatmap(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup();
    }

    public void setup() {
        @ColorInt int trackColor = Tools.getAttrColor(R.attr.colorSurface, getContext().getTheme());
        textColorSecondary = Tools.getAttrColor(R.attr.secondaryTextColor, getContext().getTheme());
        textColorTertiary = Tools.getAttrColor(R.attr.tertiaryTextColor, getContext().getTheme());
        lineColor = Tools.getAttrColor(R.attr.colorSecondary, getContext().getTheme());
        int trackWidth = Tools.dpToPx(getContext(), 12);
        labelTextSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 11, getResources().getDisplayMetrics());
        initColors(4);

        dataTrackPaint.setAntiAlias(true);
        dataTrackPaint.setStyle(Paint.Style.STROKE);
        dataTrackPaint.setStrokeCap(Paint.Cap.ROUND);
        dataTrackPaint.setColor(trackColor);
        dataTrackPaint.setStrokeWidth(trackWidth);

        dataLinePaint.setAntiAlias(true);
        dataLinePaint.setStyle(Paint.Style.STROKE);
        dataLinePaint.setStrokeCap(Paint.Cap.BUTT);
        dataLinePaint.setColor(lineColor);
        dataLinePaint.setStrokeWidth(trackWidth);

        dataLabelPaint.setColor(textColorTertiary);
        dataLabelPaint.setTextAlign(Paint.Align.LEFT);
        dataLabelPaint.setFakeBoldText(true);
        dataLabelPaint.setAntiAlias(true);
        dataLabelPaint.setTextSize(labelTextSize);

        textBounds = new Rect();
        textMargin = Tools.dpToPx(getContext(), 4);
    }

    private void initColors(int colorAmount) {
        colors = new int[colorAmount];
        colors[colorAmount - 1] = lineColor;
        for (int i = 0; i < colorAmount - 1; i++) {
            colors[i] = Tools.manipulateAlpha(lineColor, (i + 1) * (1.0f / colorAmount));
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int strokePadding = (int) (dataTrackPaint.getStrokeWidth() / 2);

        dataLabelPaint.getTextBounds("0", 0, "0".length(), textBounds);
        int time0Width = textBounds.width();
        int time0Height = textBounds.height();
        dataLabelPaint.getTextBounds("6", 0, "6".length(), textBounds);
        int time6Width = textBounds.width();
        int time6Height = textBounds.height();
        dataLabelPaint.getTextBounds("12", 0, "12".length(), textBounds);
        int time12Width = textBounds.width();
        int time12Height = textBounds.height();
        dataLabelPaint.getTextBounds("18", 0, "18".length(), textBounds);
        int time18Width = textBounds.width();
        int time18Height = textBounds.height();

        dataLabelPaint.getTextBounds("Session", 0, "Session".length(), textBounds);
        int descriptionL1Width = textBounds.width();
        int descriptionL1Height = textBounds.height();
        dataLabelPaint.getTextBounds("timeline", 0, "timeline".length(), textBounds);
        int descriptionL2Width = textBounds.width();
        int descriptionL2Height = textBounds.height();

        int drawingHeight = getHeight() - strokePadding * 2;
        int drawingWidth = getWidth() - strokePadding * 2;

        int diameter = Math.min(drawingHeight, drawingWidth);

        int horizontalSpacing = (getWidth() - diameter) / 2;
        int verticalSpacing = (getHeight() - diameter) / 2;

        dataLabelPaint.setColor(textColorSecondary);
        float diameterDescSpacing = (diameter - descriptionL1Height - textMargin - descriptionL2Height) / 2.0f;
        canvas.drawText("Session", horizontalSpacing + diameter / 2.0f - descriptionL1Width / 2.0f, verticalSpacing + diameterDescSpacing + descriptionL1Height, dataLabelPaint);
        canvas.drawText("timeline", horizontalSpacing + diameter / 2.0f - descriptionL2Width / 2.0f, verticalSpacing + diameterDescSpacing + descriptionL1Height + textMargin + descriptionL2Height, dataLabelPaint);
        dataLabelPaint.setColor(textColorTertiary);

        // draw track
        canvas.drawArc(horizontalSpacing, verticalSpacing, diameter + horizontalSpacing, diameter + verticalSpacing, 270, 360, false, dataTrackPaint);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        float angleFragment = 360.0f / (24 * 2);
        float currentAngle = 0.0f;
        while(calendar.get(Calendar.DAY_OF_MONTH) == day) {
            long millis = calendar.get(Calendar.HOUR_OF_DAY) * 60 * 60 * 1000 + calendar.get(Calendar.MINUTE) * 60 * 1000;
            int count = timestampCounts.getOrDefault(millis, 0);
            if(count > 0) {
                int index = Math.round((100 * count / (float) maxValue) / (100.0f / (colors.length - 1)));
                dataLinePaint.setColor(colors[index]);
                canvas.drawArc(horizontalSpacing, verticalSpacing, diameter + horizontalSpacing, diameter + verticalSpacing, 270 + currentAngle, angleFragment, false, dataLinePaint);
            }
            calendar.add(Calendar.MINUTE, 30);
            currentAngle += angleFragment;
        }

        // draw hour labels inside
        canvas.drawText("0",  horizontalSpacing + diameter / 2.0f - time0Width / 2.0f, verticalSpacing + textMargin + strokePadding + time0Height, dataLabelPaint);
        canvas.drawText("6", getWidth() - horizontalSpacing - textMargin - time6Width - strokePadding, verticalSpacing + diameter / 2.0f + time6Height / 2.0f, dataLabelPaint);
        canvas.drawText("12", horizontalSpacing + diameter / 2.0f - time12Width / 2.0f, getHeight() - verticalSpacing - textMargin - strokePadding, dataLabelPaint);
        canvas.drawText("18", horizontalSpacing + textMargin + strokePadding, verticalSpacing + diameter / 2.0f + time18Height / 2.0f, dataLabelPaint);
    }

    public void setTimestamps(List<Long> timestamps) {
        Calendar calendar = Calendar.getInstance();
        long midnight = Tools.getMidnightTime();
        for (Long timestamp : timestamps) {
            calendar.setTimeInMillis(midnight + timestamp);
            long millis = calendar.get(Calendar.HOUR_OF_DAY) * 60 * 60 * 1000;
            long countedTowardsMinute = ((long) (((calendar.get(Calendar.MINUTE) + 15) % 60) / 30.0f)) * 30;
            millis += countedTowardsMinute * 60 * 1000;
            int amount = timestampCounts.getOrDefault(millis, 0) + 1;
            timestampCounts.put(millis, amount);
        }
        maxValue = Collections.max(timestampCounts.values());
        initColors(Math.min(4, maxValue));
    }
}
