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
import androidx.core.content.res.ResourcesCompat;

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
    private HashMap<Long, Integer> timestampCounts = new HashMap<>();
    private Rect textBounds;
    private int trackWidth;
    @ColorInt
    private int[] colors;
    @ColorInt
    private int lineColor;
    private int maxValue;
    private Bitmap icon;
    @ColorInt
    private int textColorTertiary;
    @ColorInt
    private int textColorSecondary;
    @ColorInt
    private int textColorPrimary;
    private int labelTextSize;
    private int valueTextSize;

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
        @ColorInt int trackColor = Tools.getAttrColor(R.attr.backgroundColor, getContext().getTheme());
        textColorPrimary = Tools.getAttrColor(R.attr.primaryTextColor, getContext().getTheme());
        textColorSecondary = Tools.getAttrColor(R.attr.secondaryTextColor, getContext().getTheme());
        textColorTertiary = Tools.getAttrColor(R.attr.tertiaryTextColor, getContext().getTheme());
        lineColor = ResourcesCompat.getColor(getResources(), R.color.pastel_orange, getContext().getTheme());
        trackWidth = Tools.dpToPx(getContext(), 12);
        labelTextSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 11, getResources().getDisplayMetrics());
        valueTextSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16, getResources().getDisplayMetrics());
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

        icon = Tools.drawableToBitmap(getContext().getResources().getDrawable(R.drawable.rounded_history_24, getContext().getTheme()), Tools.getAttrColor(R.attr.primaryTextColor, getContext().getTheme()), Tools.dpToPx(getContext(), 24));
        textBounds = new Rect();
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
        dataLabelPaint.getTextBounds("12", 0, "12".length(), textBounds);
        int time12Width = textBounds.width();
        int time12Height = textBounds.height();
        dataLabelPaint.getTextBounds("24", 0, "24".length(), textBounds);
        int time24Width = textBounds.width();
        int time24Height = textBounds.height();
        int textMargin = Tools.dpToPx(getContext(), 4);


        dataLabelPaint.setTextSize(valueTextSize);
        dataLabelPaint.getTextBounds("3.4", 0, "3.4".length(), textBounds);
        dataLabelPaint.setTextSize(labelTextSize);
        int avgWidth = textBounds.width();
        int avgHeight = textBounds.height();
        dataLabelPaint.getTextBounds("Average", 0, "average".length(), textBounds);
        int descriptionL1Width = textBounds.width();
        int descriptionL1Height = textBounds.height();
        dataLabelPaint.getTextBounds("sessions", 0, "sessions".length(), textBounds);
        int descriptionL2Width = textBounds.width();
        int descriptionL2Height = textBounds.height();

        int drawingHeight = getHeight() - strokePadding * 2 - (textMargin + time0Height) * 2;
        int drawingWidth = getWidth() - strokePadding * 2 - (textMargin + time24Width) * 2;

        int diameter = Math.min(drawingHeight, drawingWidth);

        int leftSpacing = (getWidth() - diameter) / 2;
        int topSpacing = (getHeight() - diameter) / 2;

        float hyp = diameter / 2.0f;
        float angleSin = (float) Math.sin(45 * (Math.PI / 180.0f));
        float centerOffset = angleSin * hyp;
        float textMarginHyp = angleSin * textMargin;

        float sideOffset = leftSpacing + diameter / 2.0f - centerOffset;
        float topOffset = topSpacing + diameter / 2.0f - centerOffset;

        dataLinePaint.setColor(colors[Math.max(0, colors.length - 1)]);
        dataLinePaint.setStyle(Paint.Style.FILL);
        dataLinePaint.setStrokeWidth(Tools.dpToPx(getContext(), 6));
        canvas.drawCircle(sideOffset, topOffset, Tools.dpToPx(getContext(), 20), dataLinePaint);
        dataLinePaint.setStrokeWidth(trackWidth);
        dataLinePaint.setStyle(Paint.Style.STROKE);

        dataLabelPaint.setTextSize(valueTextSize);
        dataLabelPaint.setColor(textColorPrimary);
        canvas.drawText("3.4", sideOffset - avgWidth / 2.0f, topOffset + avgHeight / 2.0f, dataLabelPaint);
        dataLabelPaint.setTextSize(labelTextSize);

        dataLabelPaint.setColor(textColorSecondary);
        float diameterDescSpacing = (diameter - descriptionL1Height - textMargin - descriptionL2Height) / 2.0f;
        canvas.drawText("Average", leftSpacing + diameter / 2.0f - descriptionL1Width / 2.0f, topSpacing + diameterDescSpacing + descriptionL1Height, dataLabelPaint);
        canvas.drawText("sessions", leftSpacing + diameter / 2.0f - descriptionL2Width / 2.0f, topSpacing + diameterDescSpacing + descriptionL1Height + textMargin + descriptionL2Height, dataLabelPaint);
        dataLabelPaint.setColor(textColorTertiary);

        // draw track
        canvas.drawArc(leftSpacing, topSpacing, diameter + leftSpacing, diameter + topSpacing, 270, 270, false, dataTrackPaint);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        float angleFragment = 270.0f / (24 * 2);
        float currentAngle = 0.0f;
        while(calendar.get(Calendar.DAY_OF_MONTH) == day) {
            long millis = calendar.get(Calendar.HOUR_OF_DAY) * 60 * 60 * 1000 + calendar.get(Calendar.MINUTE) * 60 * 1000;
            int count = timestampCounts.getOrDefault(millis, 0);
            if(count > 0) {
                int index = Math.round((100 * count / (float) maxValue) / (100.0f / (colors.length - 1)));
                dataLinePaint.setColor(colors[index]);
                canvas.drawArc(leftSpacing, topSpacing, diameter + leftSpacing, diameter + topSpacing, 270 + currentAngle, angleFragment, false, dataLinePaint);
                if(millis == 0) {
                    dataLinePaint.setStyle(Paint.Style.FILL);
                    canvas.drawArc(leftSpacing + diameter / 2.0f - strokePadding, topSpacing - strokePadding, leftSpacing + diameter / 2.0f + strokePadding, topSpacing + strokePadding, 90, 180, false, dataLinePaint);
                    dataLinePaint.setStyle(Paint.Style.STROKE);
                }
                else if(millis == 23 * 60 * 60 * 1000 + 30 * 60 * 1000) {
                    dataLinePaint.setStyle(Paint.Style.FILL);
                    canvas.drawArc(leftSpacing - strokePadding, topSpacing + diameter / 2.0f - strokePadding, leftSpacing + strokePadding, topSpacing + diameter / 2.0f + strokePadding, 180, 180, false, dataLinePaint);
                    dataLinePaint.setStyle(Paint.Style.STROKE);
                }
            }
            calendar.add(Calendar.MINUTE, 30);
            currentAngle += angleFragment;
        }

        // draw hour labels
        canvas.drawText("0",  leftSpacing + diameter / 2.0f - time0Width / 2.0f - strokePadding / 2.0f, topSpacing - textMargin - strokePadding, dataLabelPaint);
        canvas.drawText("12", leftSpacing + diameter / 2.0f + centerOffset + textMarginHyp + strokePadding, topSpacing + diameter / 2.0f + centerOffset + time12Height / 2.0f + textMarginHyp + strokePadding, dataLabelPaint);
        canvas.drawText("24", leftSpacing - textMargin - strokePadding - time24Width, topSpacing + diameter / 2.0f + time24Height / 2.0f - strokePadding / 2.0f, dataLabelPaint);
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
