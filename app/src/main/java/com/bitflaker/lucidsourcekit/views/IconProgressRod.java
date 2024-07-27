package com.bitflaker.lucidsourcekit.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.utils.Tools;

public class IconProgressRod extends View {
    private final Paint dataLinePaint = new Paint();
    private final Paint iconBackgroundPaint = new Paint();
    private final Paint dataLinePaintBackground = new Paint();
    private float max;
    private float value;
    private Bitmap icon;
    private int iconSize;
    private int minHeight = 0;
    private float iconCircleWidth;
    private int iconPadding;
    private int iconOutlineStrokeWidth;

    public IconProgressRod(Context context){
        super(context);
        setup();
    }

    public IconProgressRod(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setup();
    }

    private void setup() {
        max = 4;
        value = 4;
        iconSize = Tools.dpToPx(getContext(), 28);
        iconPadding = Tools.dpToPx(getContext(), 16);
        iconOutlineStrokeWidth = Tools.dpToPx(getContext(), 3);
        icon = Tools.drawableToBitmap(getContext().getResources().getDrawable(R.drawable.ic_baseline_local_fire_department_24, getContext().getTheme()), Tools.getAttrColor(R.attr.colorOnSecondary, getContext().getTheme()), iconSize);
        iconCircleWidth = iconSize + Tools.dpToPx(getContext(), Tools.dpToPx(getContext(), 3));

        iconBackgroundPaint.setAntiAlias(true);
        iconBackgroundPaint.setStrokeCap(Paint.Cap.ROUND);
        iconBackgroundPaint.setColor(Tools.getAttrColor(R.attr.colorSecondary, getContext().getTheme()));
        iconBackgroundPaint.setStrokeWidth(iconOutlineStrokeWidth);
        iconBackgroundPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        dataLinePaint.setAntiAlias(true);
        dataLinePaint.setStrokeCap(Paint.Cap.ROUND);
        dataLinePaint.setColor(Tools.getAttrColor(R.attr.colorSecondary, getContext().getTheme()));
        dataLinePaint.setStrokeWidth(Tools.dpToPx(getContext(), Tools.dpToPx(getContext(), 4)));
        dataLinePaintBackground.setAntiAlias(true);
        dataLinePaintBackground.setStrokeCap(Paint.Cap.ROUND);
        dataLinePaintBackground.setColor(Tools.getAttrColor(R.attr.colorSurface, getContext().getTheme()));
        dataLinePaintBackground.setStrokeWidth(dataLinePaint.getStrokeWidth() / 3.0f * 2);
    }

    public void setData(float max, float value) {
        this.max = max;
        this.value = value;
        invalidate();
    }

    public void setIcon(Drawable icon, int iconSize) {
        this.iconSize = iconSize;
        this.icon = Tools.drawableToBitmap(icon, iconSize);
        invalidate();
    }

    public void setLineWidth(float lineWidth) {
        dataLinePaint.setStrokeWidth(Tools.dpToPx(getContext(), lineWidth));
        dataLinePaintBackground.setStrokeWidth(dataLinePaint.getStrokeWidth() / 3.0f * 2);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float lineRad = dataLinePaint.getStrokeWidth() / 2.0f;
        float heightRad = getHeight() / 2.0f;
        float maxDrawWidth = getWidth() - lineRad;
        maxDrawWidth -= iconCircleWidth + iconOutlineStrokeWidth + iconPadding;

        float iconOutlineRadius = iconCircleWidth / 2.0f;
        if(value == max) {
            canvas.drawCircle(getWidth() - iconOutlineRadius - iconOutlineStrokeWidth, heightRad, iconOutlineRadius, iconBackgroundPaint);
        }
        canvas.drawBitmap(icon, getWidth() - iconOutlineRadius - iconOutlineStrokeWidth - iconSize / 2.0f, (getHeight() - iconSize) / 2.0f, dataLinePaint);

        canvas.drawLine(lineRad, heightRad, maxDrawWidth, heightRad, dataLinePaintBackground);
        float drawTo = maxDrawWidth * value / max;


        if(value >= 0) {
            canvas.drawLine(lineRad, heightRad, Math.max(drawTo, lineRad), heightRad, dataLinePaint);
        }

    }

    private void clacMinHeight() {
        if (minHeight == 0) {
            minHeight = (int) Math.ceil(Math.max(iconCircleWidth + 2 * iconOutlineStrokeWidth, dataLinePaint.getStrokeWidth()));
            setMinimumHeight(minHeight);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        clacMinHeight();
        setMeasuredDimension(widthMeasureSpec, minHeight);
    }
}