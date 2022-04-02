package com.bitflaker.lucidsourcekit.charts;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.general.Tools;

public class Speedometer extends View {
    private final Paint dataLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint dataLinePaint = new Paint();
    private final Paint dataLinePaintCap = new Paint();
    private float lineWidth;
    private float value;
    private float maxValue;
    private float percentage;
    private float circlePercentage;
    private SweepGradient gradientShader = null;
    private Rect textBounds;
    private int fontSizeLarge;
    private int fontSizeMedium;
    private int fontSizeSmall;
    @ColorInt private int primaryTextColor;
    @ColorInt private int secondaryTextColor;

    public Speedometer(Context context) {
        super(context);
        setup();
    }

    public Speedometer(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    private void setup() {
        dataLinePaint.setStrokeCap(Paint.Cap.ROUND);
        dataLinePaint.setAntiAlias(true);
        dataLinePaint.setStrokeWidth(50);
        dataLinePaint.setStyle(Paint.Style.STROKE);
        dataLinePaintCap.setAntiAlias(true);
        dataLinePaintCap.setStyle(Paint.Style.FILL);
        primaryTextColor = Tools.getAttrColor(R.attr.primaryTextColor, getContext().getTheme());
        secondaryTextColor = Tools.getAttrColor(R.attr.secondaryTextColor, getContext().getTheme());
        dataLinePaintCap.setColor(Tools.getAttrColor(R.attr.colorSecondary, getContext().getTheme()));
        dataLabelPaint.setColor(Tools.getAttrColor(R.attr.primaryTextColor, getContext().getTheme()));
        fontSizeLarge = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 26, getResources().getDisplayMetrics());
        fontSizeMedium = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16, getResources().getDisplayMetrics());
        fontSizeSmall = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12, getResources().getDisplayMetrics());
        dataLabelPaint.setTextSize(fontSizeLarge);
        dataLabelPaint.setTextAlign(Paint.Align.LEFT);
        dataLabelPaint.setFakeBoldText(true);
        dataLabelPaint.setAntiAlias(true);
        textBounds = new Rect();
    }

    public void setData(float lineWidth, float value, float maxValue) {
        this.lineWidth = Tools.dpToPx(getContext(), lineWidth);
        this.value = value;
        this.maxValue = maxValue;
        dataLinePaint.setStrokeWidth(this.lineWidth);
        percentage = value / maxValue;
        circlePercentage = percentage / 2.0f + 0.5f;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float halfStroke = dataLinePaint.getStrokeWidth() / 2.0f;
        float paddedHeight = getHeight() - halfStroke;
        float smallestRadius = Math.min(getWidth() / 2.0f, paddedHeight) - halfStroke;
        float leftOffset = (getWidth() / 2.0f - smallestRadius);
        float topOffset = (paddedHeight - smallestRadius);

        if(gradientShader == null) {
            gradientShader = new SweepGradient(getWidth() / 2.0f, paddedHeight, new int[] { Tools.getAttrColor(R.attr.slightElevated, getContext().getTheme()), Tools.getAttrColor(R.attr.colorSecondary, getContext().getTheme()), Tools.getAttrColor(R.attr.colorSecondary, getContext().getTheme()), Tools.getAttrColor(R.attr.slightElevated, getContext().getTheme()) }, new float[] { 0.5f, 0.5f, circlePercentage, circlePercentage });
            dataLinePaint.setShader(gradientShader);
        }

        canvas.drawArc(leftOffset, topOffset, getWidth() - leftOffset, paddedHeight * 2 - topOffset, 180, 180, false, dataLinePaint);

        final float degVal1 = 180.0f * percentage;
        double angle = 180 + degVal1;
        angle = angle * Math.PI / 180.0;
        double xVal = (getWidth()/2.0f) + Math.cos(angle) * (smallestRadius-0);
        double yVal = (paddedHeight) + Math.sin(angle) * (smallestRadius);

        canvas.drawCircle(leftOffset, paddedHeight, lineWidth/2.0f, dataLinePaintCap);
        canvas.drawCircle((float)xVal, (float)yVal, lineWidth/2.0f, dataLinePaintCap);

        String text = value + " / " + maxValue;
        String[] descriptions = "Today's goals combined\ndifficulty rating".split("\n");    // TODO: extract string resource
        int accHeight = 0;

        dataLabelPaint.setTextSize(fontSizeLarge);
        dataLabelPaint.setFakeBoldText(true);
        dataLabelPaint.setColor(primaryTextColor);
        dataLabelPaint.getTextBounds(text, 0, text.length(), textBounds);

        float bigLabelPos = paddedHeight - textBounds.height() - 30;
        canvas.drawText(text, getWidth()/2.0f - textBounds.exactCenterX(), bigLabelPos, dataLabelPaint);

        dataLabelPaint.setTextSize(fontSizeSmall);
        dataLabelPaint.setFakeBoldText(false);
        dataLabelPaint.setColor(secondaryTextColor);

        accHeight += textBounds.height() / 2.0f;
        for (String description : descriptions) {
            dataLabelPaint.getTextBounds(description, 0, description.length(), textBounds);
            canvas.drawText(description, getWidth() / 2.0f - textBounds.exactCenterX(), bigLabelPos + accHeight + 28, dataLabelPaint);
            accHeight += textBounds.height() + 10;
        }
    }
}
