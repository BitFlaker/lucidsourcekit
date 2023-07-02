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

import java.util.Locale;

public class Speedometer extends View {
    private final Paint dataDescriptionPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint dataLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint dataLabelPaintOf = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint dataLinePaint = new Paint();
    private final Paint dataLinePaintCap = new Paint();
    private float lineWidth;
    private float value;
    private float maxValue;
    private float percentage;
    private float circlePercentage;
    private SweepGradient gradientShader = null;
    private Rect textBounds, textBoundsOf;
    private Rect[] descriptionTextBounds;
    private int fontSizeLarge;
    private int fontSizeMedium;
    private int fontSizeSmall;
    @ColorInt private int primaryTextColor;
    @ColorInt private int secondaryTextColor;
    @ColorInt private int trackColor;
    private String[] description;
    private int decimalPlaces = 1;
    private float descriptionMarginTop = 25;
    private boolean drawProgressOnNoProgress = false;

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
        trackColor = Tools.getAttrColor(R.attr.slightElevated, getContext().getTheme());
        dataLinePaintCap.setColor(Tools.getAttrColor(R.attr.colorSecondary, getContext().getTheme()));
        fontSizeLarge = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 28, getResources().getDisplayMetrics());
        fontSizeMedium = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16, getResources().getDisplayMetrics());
        fontSizeSmall = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12, getResources().getDisplayMetrics());
        dataLabelPaint.setColor(Tools.getAttrColor(R.attr.primaryTextColor, getContext().getTheme()));
        dataLabelPaint.setTextSize(fontSizeLarge);
        dataLabelPaint.setTextAlign(Paint.Align.LEFT);
        dataLabelPaint.setFakeBoldText(true);
        dataLabelPaint.setAntiAlias(true);
        dataDescriptionPaint.setColor(secondaryTextColor);
        dataDescriptionPaint.setTextAlign(Paint.Align.LEFT);
        dataDescriptionPaint.setTextSize(fontSizeSmall);
        dataDescriptionPaint.setFakeBoldText(false);
        dataDescriptionPaint.setAntiAlias(true);
        dataLabelPaintOf.setColor(secondaryTextColor);
        dataLabelPaintOf.setTextSize(fontSizeMedium);
        dataLabelPaintOf.setTextAlign(Paint.Align.LEFT);
        dataLabelPaintOf.setFakeBoldText(false);
        dataLabelPaintOf.setAntiAlias(true);
        textBounds = new Rect();
        descriptionTextBounds = new Rect[0];
        textBoundsOf = new Rect();
        description = new String[0];
    }

    public void setData(float lineWidth, float value, float maxValue) {
        this.lineWidth = Tools.dpToPx(getContext(), lineWidth);
        this.value = value;
        this.maxValue = maxValue;
        dataLinePaint.setStrokeWidth(this.lineWidth);
        percentage = (value-1) / (maxValue-1);
        circlePercentage = percentage / 2.0f + 0.5f;
        gradientShader = null;
        invalidate();
    }

    public void setDescription(String description) {
        this.description = description.replaceAll("\\\\r\\\\n", "\n").split("\n");
        descriptionTextBounds = new Rect[this.description.length];
        for (int i = 0; i < descriptionTextBounds.length; i++) {
            descriptionTextBounds[i] = new Rect();
        }
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
            gradientShader = new SweepGradient(getWidth() / 2.0f, paddedHeight, new int[] { trackColor, Tools.getAttrColor(R.attr.colorSecondary, getContext().getTheme()), Tools.getAttrColor(R.attr.colorSecondary, getContext().getTheme()), trackColor }, new float[] { 0.5f, 0.5f, circlePercentage, circlePercentage });
            dataLinePaint.setShader(gradientShader);
        }

        canvas.drawArc(leftOffset, topOffset, getWidth() - leftOffset, paddedHeight * 2 - topOffset, 180, 180, false, dataLinePaint);

        final float degVal1 = 180.0f * percentage;
        double angle = 180 + degVal1;
        angle = angle * Math.PI / 180.0;
        double xVal = (getWidth()/2.0f) + Math.cos(angle) * (smallestRadius-0);
        double yVal = (paddedHeight) + Math.sin(angle) * (smallestRadius);

        if(this.value != 0 || drawProgressOnNoProgress){
            canvas.drawCircle(leftOffset, paddedHeight, lineWidth/2.0f, dataLinePaintCap);
            canvas.drawCircle((float)xVal, (float)yVal, lineWidth/2.0f, dataLinePaintCap);
        }

        String text = String.format(Locale.ENGLISH, "%." + decimalPlaces + "f", value);   // TODO: , and . as separators have to be taken into consideration
        String textOf = " / " + String.format(Locale.ENGLISH, "%." + decimalPlaces + "f", maxValue);
        int accHeight = 0;

        dataLabelPaint.getTextBounds(text, 0, text.length(), textBounds);
        dataLabelPaintOf.getTextBounds(textOf, 0, textOf.length(), textBoundsOf);

        int descriptionFieldHeight = 0;
        for (int i = 0; i < description.length; i++) {
            dataDescriptionPaint.getTextBounds(description[i], 0, description[i].length(), descriptionTextBounds[i]);
            descriptionFieldHeight += descriptionTextBounds[i].height();
        }

        float bigLabelPos = paddedHeight - getDescriptionMarginTop() - descriptionFieldHeight;
        if(description.length == 0) {
            // If there is no description, change the position of the value to the center
            bigLabelPos -= (paddedHeight - (textBounds.height() * 2) - dataLinePaint.getStrokeWidth()) / 2.0f;
        }
        canvas.drawText(text, getWidth()/2.0f - textBounds.exactCenterX() - textBoundsOf.exactCenterX(), bigLabelPos, dataLabelPaint);
        canvas.drawText(textOf, getWidth()/2.0f + textBounds.exactCenterX() - textBoundsOf.exactCenterX(), bigLabelPos, dataLabelPaintOf);

        accHeight += getDescriptionMarginTop(); //textBounds.height() / 2.0f;
        for (int i = 0; i < description.length; i++) {
            canvas.drawText(description[i], getWidth() / 2.0f - descriptionTextBounds[i].exactCenterX(), bigLabelPos + accHeight + 36, dataDescriptionPaint);
            accHeight += descriptionTextBounds[i].height() + 10;
            System.out.println(descriptionTextBounds[i].height());
        }
    }

    public int getDecimalPlaces() {
        return decimalPlaces;
    }

    public void setDecimalPlaces(int decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
    }

    public float getDescriptionMarginTop() {
        return description.length == 0 ? 0 : descriptionMarginTop;
    }

    public void setDescriptionMarginTop(float descriptionMarginTop) {
        this.descriptionMarginTop = descriptionMarginTop;
    }

    public boolean isDrawProgressOnNoProgress() {
        return drawProgressOnNoProgress;
    }

    public void setDrawProgressOnNoProgress(boolean drawProgressOnNoProgress) {
        this.drawProgressOnNoProgress = drawProgressOnNoProgress;
    }
}
