package com.bitflaker.lucidsourcekit.charts;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.general.Tools;

public class IconOutOf extends View {
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

    public IconOutOf(Context context) {
        super(context);
        setup();
    }

    public IconOutOf(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setup();
        setConfiguredValues(context, attrs);
    }

    public IconOutOf(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup();
        setConfiguredValues(context, attrs);
    }

    private void setConfiguredValues(Context context, @Nullable AttributeSet attrs) {
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.IconOutOf, 0, 0);
        try {
            dataLinePaint.setStrokeWidth(a.getDimension(R.styleable.IconOutOf_lineWidth, Tools.dpToPx(getContext(), 2)));
            dataLineTrackPaint.setStrokeWidth(a.getDimension(R.styleable.IconOutOf_lineWidth, Tools.dpToPx(getContext(), 2)));
            this.diameter = (int) a.getDimension(R.styleable.IconOutOf_diameter, Tools.dpToPx(getContext(), 40));
            this.textMargin = (int) a.getDimension(R.styleable.IconOutOf_textSpacing, Tools.dpToPx(getContext(), 6));
            this.textOfMargin = (int) a.getDimension(R.styleable.IconOutOf_textOfSpacing, Tools.dpToPx(getContext(), 2));
            dataLabelPaint.setTextSize(a.getDimension(R.styleable.IconOutOf_textSizeValue, Tools.spToPx(getContext(), 16)));
            dataLabelPaintOf.setTextSize(a.getDimension(R.styleable.IconOutOf_textSizeValueOf, Tools.spToPx(getContext(), 12)));
            setDescription(a.getString(R.styleable.IconOutOf_description));
            Drawable iconD = a.getDrawable(R.styleable.IconOutOf_icon);
            if(iconD != null) {
                icon = Tools.drawableToBitmap(iconD, mainColor, Tools.dpToPx(getContext(), 18));
            }

            dataLabelPaint.getTextBounds(Integer.toString(this.value), 0, Integer.toString(this.value).length(), textBounds);
            dataLabelPaintOf.getTextBounds("/" + this.maxValue, 0, ("/" + this.maxValue).length(), textBoundsOf);
        } finally {
            a.recycle();
        }
    }

    private void setup() {
        mainColor = getResources().getColor(R.color.pastel_orange, getContext().getTheme());
        @ColorInt int defaultColor = getResources().getColor(R.color.white, getContext().getTheme());
        @ColorInt int trackColor = Tools.getAttrColor(R.attr.backgroundColor, getContext().getTheme());
        @ColorInt int secondaryTextColor = Tools.getAttrColor(R.attr.secondaryTextColor, getContext().getTheme());
        @ColorInt int tertiaryTextColor = Tools.getAttrColor(R.attr.tertiaryTextColor, getContext().getTheme());

        icon = Tools.drawableToBitmap(ResourcesCompat.getDrawable(getContext().getResources(), R.drawable.ic_baseline_local_fire_department_24, getContext().getTheme()), mainColor, Tools.dpToPx(getContext(), 18));

        dataLinePaint.setStrokeCap(Paint.Cap.ROUND);
        dataLinePaint.setAntiAlias(true);
        dataLinePaint.setStrokeWidth(Tools.dpToPx(getContext(), 2));
        dataLinePaint.setStyle(Paint.Style.STROKE);
        dataLinePaint.setColor(mainColor);

        dataLineTrackPaint.setAntiAlias(true);
        dataLineTrackPaint.setStrokeWidth(Tools.dpToPx(getContext(), 2));
        dataLineTrackPaint.setStyle(Paint.Style.STROKE);
        dataLineTrackPaint.setColor(trackColor);

        dataLabelPaint.setColor(Tools.getAttrColor(R.attr.primaryTextColor, getContext().getTheme()));
        dataLabelPaint.setTextAlign(Paint.Align.LEFT);
        dataLabelPaint.setFakeBoldText(true);
        dataLabelPaint.setAntiAlias(true);
        setFontSizeValue(16);

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

        setValue(18);
        setMaxValue(26);
        setDescription("Daily streak");

        if(secondaryTextColor == 0) {
            dataLabelPaint.setColor(defaultColor);
            dataLabelPaintOf.setColor(defaultColor);
            dataLabelPaintDescription.setColor(defaultColor);
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int translateX = Tools.dpToPx(getContext(), -1);
        int translateY = Tools.dpToPx(getContext(), -1);
        int descriptionSpacing = Tools.dpToPx(getContext(), 3);

        canvas.drawArc(padding, padding, diameter, diameter, 270, 360, false, dataLineTrackPaint);
        canvas.drawArc(padding, padding, diameter, diameter, 270, 360 * (value / (float) maxValue), false, dataLinePaint);
        canvas.drawBitmap(icon, padding + (diameter / 2.0f) - (icon.getWidth() / 2.0f) + translateX, padding + (diameter / 2.0f) - (icon.getHeight() / 2.0f) + translateY, dataLinePaint);

        float valueTextPositionY = padding + (diameter / 2.0f) - textBounds.exactCenterY() - (textBoundsDescription.height() / 2.0f) - descriptionSpacing / 2.0f;
        canvas.drawText(Integer.toString(this.value), padding * 2 + diameter + textMargin, valueTextPositionY, dataLabelPaint);
        canvas.drawText("/" + this.maxValue, padding * 2 + diameter + textMargin + textBounds.width() + textOfMargin, valueTextPositionY, dataLabelPaintOf);
        canvas.drawText(description, padding * 2 + diameter + textMargin, valueTextPositionY + textBoundsDescription.height() + descriptionSpacing, dataLabelPaintDescription);
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
    }

    private Rect getMinimumSize() {
        padding = dataLinePaint.getStrokeWidth() / 2.0f;
        int maxTextWidth = Math.max(textBounds.width() + textOfMargin + textBoundsOf.width(), textBoundsDescription.width());
        int endPadding = Tools.dpToPx(getContext(), 4);
        return new Rect(0, 0, (int)Math.ceil(padding + diameter + padding + textMargin + maxTextWidth + endPadding), (int)Math.ceil(padding + diameter));
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

    public void setValue(int value) {
        this.value = value;
        dataLabelPaint.getTextBounds(Integer.toString(this.value), 0, Integer.toString(this.value).length(), textBounds);
    }

    public void setMaxValue(int maxValue) {
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
}
