package com.bitflaker.lucidsourcekit.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.data.records.DataValue;
import com.bitflaker.lucidsourcekit.utils.Tools;

import java.util.ArrayList;
import java.util.List;

public class RodGraph extends View {
    private List<DataValue> data = new ArrayList<>();
    private final Paint axisLinePaint = new Paint();
    private final Paint dataLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint dataLinePaint = new Paint();
    private final Paint dataLinePaintBackground = new Paint();
    private float xMax;
    private double yMax;
    private float textHeight;
    private int iconSize;
    private Bitmap[] icons;
    private int minHeight = 0;
    private int extraSpacing;
    private boolean invertedOrder = false;

    public RodGraph(Context context){
        super(context);
        setup();
    }

    public RodGraph(Context context, AttributeSet attributeSet){
        super(context, attributeSet);
        setup();
    }

    private void setup() {
        axisLinePaint.setColor(Color.GRAY);
        dataLinePaintBackground.setColor(Tools.getAttrColor(R.attr.colorSurfaceContainer, getContext().getTheme()));
        dataLinePaintBackground.setStrokeCap(Paint.Cap.ROUND);
        dataLinePaintBackground.setAntiAlias(true);
        dataLinePaint.setColor(Tools.getAttrColor(R.attr.colorTertiary, getContext().getTheme()));
        dataLinePaint.setStrokeCap(Paint.Cap.ROUND);
        dataLinePaint.setAntiAlias(true);
        dataLabelPaint.setColor(Tools.getAttrColor(R.attr.secondaryTextColor, getContext().getTheme()));
        dataLabelPaint.setTextSize((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14, getResources().getDisplayMetrics()));
        dataLabelPaint.setTextAlign(Paint.Align.CENTER);
        dataLabelPaint.setAntiAlias(true);
        axisLinePaint.setStrokeWidth(Tools.dpToPx(getContext(), 3));
        extraSpacing = Tools.dpToPx(getContext(), 28);
    }

    public void setData(List<DataValue> data, float lineWidth, int iconSize, Drawable[] icons) {
        this.data = data;
        this.iconSize = iconSize;
        this.icons = getBitmapsFromDrawableArray(icons);
        this.textHeight = 0;
        if(icons != null){ yMax = icons.length - 1; }
        else { yMax = getMaxValueFrom(data); }
        xMax = data.size();
        dataLinePaintBackground.setStrokeWidth(Tools.dpToPx(getContext(), lineWidth / 3.0f * 2));
        dataLinePaint.setStrokeWidth(Tools.dpToPx(getContext(), lineWidth));
        invalidate();
    }

    private double getMaxValueFrom(List<DataValue> data) {
        double maxVal = 0;
        for (DataValue dv : data) {
            if(dv.value() > maxVal) {
                maxVal = dv.value();
            }
        }
        return maxVal;
    }

    private Bitmap[] getBitmapsFromDrawableArray(Drawable[] icons) {
        if(icons == null){ return null; }
        Bitmap[] bitmaps = new Bitmap[icons.length];
        for (int i = 0; i < bitmaps.length; i++){
            bitmaps[i] = Tools.drawableToBitmap(icons[i], iconSize);
        }
        return bitmaps;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float marginForRodRadius = 0.0f;
        float bottomPointWithTextMargin = 0.0f;
        float rodPaddingRight = Tools.dpToPx(getContext(), 10);

        for (int i = 0; i < data.size(); i++) {
            DataValue current = data.get(invertedOrder ? data.size() - 1 - i : i);
            int minRadMargin = Tools.dpToPx(getContext(), 4);
            int iconPlaceholder = icons != null ? iconSize + Tools.dpToPx(getContext(), 5) : 0;
            marginForRodRadius = Math.max(iconPlaceholder / 2.0f, minRadMargin);
            float xPos = i / xMax * (getWidth() - iconPlaceholder - rodPaddingRight) + ((getWidth() - iconPlaceholder - rodPaddingRight) / xMax) / 2 + iconPlaceholder;
            float bottomPoint = getHeight() - marginForRodRadius;

            int yText = drawTextIfAvailable(canvas, current, xPos);
            if(this.textHeight == 0) { this.textHeight = yText; }

            bottomPointWithTextMargin = drawRodBackground(canvas, marginForRodRadius, xPos, bottomPoint);

            if(current.value() > -1){
                double progressHeight = (bottomPointWithTextMargin - (bottomPointWithTextMargin / yMax * current.value())) + ((marginForRodRadius / yMax) * current.value());
                canvas.drawLine(xPos, bottomPointWithTextMargin, xPos, (float)progressHeight, dataLinePaint);
            }
        }

        drawIcons(canvas, marginForRodRadius, bottomPointWithTextMargin);
    }

    private void clacMinHeight() {
        if(minHeight == 0) {
            if(icons != null) {
                minHeight = (int)Math.ceil(iconSize * icons.length + this.textHeight);
                if(this.textHeight == 0) {
                    minHeight += iconSize;
                }
            }
            else {
                minHeight = iconSize * 5;
            }
            setMinimumHeight(minHeight);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        clacMinHeight();
        setMeasuredDimension(widthMeasureSpec, minHeight + extraSpacing);
    }

    private void drawIcons(Canvas canvas, float marginForRodRadius, float bottomPointWithTextMargin) {
        if(icons == null){ return; }
        for (int j = 0; j < icons.length; j++){
            double yPos = (bottomPointWithTextMargin - (bottomPointWithTextMargin / yMax * j)) + ((marginForRodRadius / yMax) * j) - iconSize / 2f;
            canvas.drawBitmap(icons[j], 0, (float)yPos, dataLinePaintBackground);
        }
    }

    private float drawRodBackground(Canvas canvas, float marginForRodRadius, float xPos, float bottomPoint) {
        float bottomPointWithTextMargin = bottomPoint - this.textHeight - Tools.dpToPx(getContext(), 1);
        canvas.drawLine(xPos, bottomPointWithTextMargin, xPos, marginForRodRadius, dataLinePaintBackground);
        return bottomPointWithTextMargin;
    }

    private int drawTextIfAvailable(Canvas canvas, DataValue dataPoint, float xPos) {
        Paint.FontMetrics metric = dataLabelPaint.getFontMetrics();
        int textHeight = (int) Math.ceil(metric.descent - metric.ascent);
        int yText = 0;
        if(dataPoint.label() != null && !dataPoint.label().isEmpty()){
            String[] lines = dataPoint.label().split("\n");
            yText = (int)metric.descent;
            for (int j = lines.length - 1; j >= 0; j--){
                canvas.drawText(lines[j], xPos, getHeight() - yText, dataLabelPaint);
                yText += textHeight;
            }
        }
        return yText;
    }

    public int getExtraSpacing() {
        return extraSpacing;
    }

    public void setExtraSpacing(int extraSpacing) {
        this.extraSpacing = extraSpacing;
    }

    public boolean isInvertedOrder() {
        return invertedOrder;
    }

    public void setInvertedOrder(boolean invertedOrder) {
        this.invertedOrder = invertedOrder;
    }

    public int getMinHeight() {
        return minHeight;
    }
}