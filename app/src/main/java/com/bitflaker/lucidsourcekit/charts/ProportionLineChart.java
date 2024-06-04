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
import androidx.core.content.res.ResourcesCompat;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.general.Tools;

import java.util.Arrays;

public class ProportionLineChart extends View {
    private final Paint dataLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint dataTrackPaint = new Paint();
    private final Paint dataLinePaint = new Paint();
    private final Paint dataLegendPaint = new Paint();
    private Rect textBounds;
    private int valueGap;
    private boolean drawTrack;
    private int trackWidth;
    private DataPoint[] values;
    private DataPoint[] valuesZeroed;
    private float[] valueTrackWidths;
    private int legendMarginTop;
    private int[] labelWidths;
    private int maxLegendTextHeight;
    private int legendCircleRadius;
    private int legendCircleTextMargin;
    private int legendItemsMargin;
    private int paddingVertical, paddingHorizontal;

    public ProportionLineChart(Context context) {
        super(context);
        setup();
    }

    public ProportionLineChart(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public ProportionLineChart(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup();
    }

    public void setup() {
        @ColorInt int trackColor = Tools.getAttrColor(R.attr.colorSurface, getContext().getTheme());
        valueGap = Tools.dpToPx(getContext(), 4);
        trackWidth = Tools.dpToPx(getContext(), 12);
        legendMarginTop = Tools.dpToPx(getContext(), 8);
        legendCircleRadius = Tools.dpToPx(getContext(), 4);
        legendCircleTextMargin = Tools.dpToPx(getContext(), 4);
        legendItemsMargin = Tools.dpToPx(getContext(), 16);
        paddingVertical = Tools.dpToPx(getContext(), 4);
        paddingHorizontal = Tools.dpToPx(getContext(), 0);

        dataTrackPaint.setAntiAlias(true);
        dataTrackPaint.setStyle(Paint.Style.STROKE);
        dataTrackPaint.setStrokeCap(Paint.Cap.ROUND);
        dataTrackPaint.setColor(trackColor);
        dataTrackPaint.setStrokeWidth(trackWidth);

        dataLinePaint.setAntiAlias(true);
        dataLinePaint.setStyle(Paint.Style.STROKE);
        dataLinePaint.setStrokeCap(Paint.Cap.ROUND);
        dataLinePaint.setColor(trackColor);
        dataLinePaint.setStrokeWidth(trackWidth);

        dataLabelPaint.setColor(Tools.getAttrColor(R.attr.secondaryTextColor, getContext().getTheme()));
        dataLabelPaint.setTextAlign(Paint.Align.LEFT);
        dataLabelPaint.setFakeBoldText(true);
        dataLabelPaint.setAntiAlias(true);
        dataLabelPaint.setTextSize((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12, getResources().getDisplayMetrics()));

        dataLegendPaint.setAntiAlias(true);
        dataLegendPaint.setStyle(Paint.Style.FILL);
        dataLegendPaint.setColor(trackColor);

        textBounds = new Rect();

        values = new DataPoint[] {
                new DataPoint(ResourcesCompat.getColor(getResources(), R.color.lighter_green, getContext().getTheme()), 100, "Val1"),
                new DataPoint(ResourcesCompat.getColor(getResources(), R.color.lighter_orange, getContext().getTheme()), 10, "LongerVal2"),
                new DataPoint(ResourcesCompat.getColor(getResources(), R.color.lighter_red, getContext().getTheme()), 273, "Some3"),
        };
        calculateLegendTextDimensions();
        drawTrack = false;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int capPadding = (int) (dataTrackPaint.getStrokeWidth() / 2);

        drawTrack(canvas, capPadding);
        drawValues(canvas, capPadding);
        drawLegend(canvas, capPadding);
    }

    private void drawLegend(Canvas canvas, int capPadding) {
        int cumulativeMargin = paddingHorizontal;
        for (int i = 0; i < values.length; i++) {
            if(valuesZeroed[i].getValue() == 0) {  // skip values not displayed in chart
                continue;
            }
            DataPoint current = values[i];
            dataLegendPaint.setColor(current.getColor());
            canvas.drawCircle(cumulativeMargin + legendCircleRadius, capPadding * 2 + paddingVertical + legendMarginTop + maxLegendTextHeight - legendCircleRadius, legendCircleRadius, dataLegendPaint);
            canvas.drawText(current.getLabel(), cumulativeMargin + (legendCircleRadius * 2) + legendCircleTextMargin, capPadding * 2 + paddingVertical + legendMarginTop + maxLegendTextHeight, dataLabelPaint);
            cumulativeMargin += (legendCircleRadius * 2) + legendCircleTextMargin + legendItemsMargin + labelWidths[i];
        }
    }

    private void drawValues(@NonNull Canvas canvas, int capPadding) {
        float padLeft = capPadding + paddingHorizontal;
        for (int i = 0; i < valuesZeroed.length; i++) {
            if(valuesZeroed[i].getValue() == 0){
                continue;
            }
            dataLinePaint.setColor(valuesZeroed[i].getColor());
            canvas.drawLine(padLeft, capPadding + paddingVertical, padLeft + valueTrackWidths[i], capPadding + paddingVertical, dataLinePaint);
            padLeft += valueTrackWidths[i] + 2 * capPadding + valueGap;
        }
    }

    private void drawTrack(@NonNull Canvas canvas, int capPadding) {
        if(drawTrack) {
            canvas.drawLine(paddingHorizontal + capPadding, paddingVertical + capPadding, getWidth() - capPadding - paddingHorizontal, capPadding + paddingVertical, dataTrackPaint);
        }
    }

    private void calculateLegendTextDimensions() {
        maxLegendTextHeight = 0;
        labelWidths = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            dataLabelPaint.getTextBounds(values[i].getLabel(), 0, values[i].getLabel().length(), textBounds);
            maxLegendTextHeight = Math.max(maxLegendTextHeight, textBounds.height());
            labelWidths[i] = textBounds.width();
        }
    }

    private void calculateValueTrackWidths(int width) {
        int minIndex;
        valuesZeroed = values.clone();
        int capPadding = (int) (dataTrackPaint.getStrokeWidth() / 2);
        int minValueTrackWidth = capPadding * 2;

        do {
            minIndex = -1;
            int nonZeroCount = (int) Arrays.stream(valuesZeroed).filter(v -> v.getValue() != 0).count();
            float total = (float) Arrays.stream(valuesZeroed).mapToDouble(DataPoint::getValue).sum();
            int trackWidth = width - capPadding * 2 - paddingHorizontal * 2 - (nonZeroCount - 1) * (minValueTrackWidth + valueGap);
            valueTrackWidths = new float[valuesZeroed.length];

            float smallestWidth = trackWidth;

            for (int i = 0; i < valuesZeroed.length; i++) {
                if(valuesZeroed[i].getValue() == 0) {
                    continue;
                }
                float totalProportion = valuesZeroed[i].getValue() / total;
                float valueTrackWidth = totalProportion * trackWidth;
                if (valueTrackWidth < minValueTrackWidth && valueTrackWidth < smallestWidth) {
                    minIndex = i;
                    smallestWidth = valueTrackWidth;
                }
                valueTrackWidths[i] = valueTrackWidth;
            }

            if(minIndex != -1) {
                valuesZeroed[minIndex].setValue(0);
            }
        } while(minIndex != -1);
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

        calculateValueTrackWidths(width);
        invalidate();
    }

    private Rect getMinimumSize() {
        return new Rect(0, 0, paddingHorizontal, trackWidth + legendMarginTop + maxLegendTextHeight + paddingVertical * 2);
    }

    public DataPoint[] getValues() {
        return values;
    }

    public void setValues(DataPoint[] values) {
        this.values = values;
        calculateLegendTextDimensions();
        calculateValueTrackWidths(getWidth());
    }

    public static class DataPoint {
        private float value;
        private String label;
        @ColorInt int color;

        public DataPoint(@ColorInt int color, float value, String label) {
            this.value = value;
            this.label = label;
            this.color = color;
        }

        public float getValue() {
            return value;
        }

        public void setValue(float value) {
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public int getColor() {
            return color;
        }

        public void setColor(int color) {
            this.color = color;
        }
    }
}
