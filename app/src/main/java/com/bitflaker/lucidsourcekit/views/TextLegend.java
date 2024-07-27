package com.bitflaker.lucidsourcekit.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.utils.Tools;

import java.util.ArrayList;
import java.util.List;

public class TextLegend extends View {
    private final Paint randomBackgroundPaint = new Paint();
    private final Paint dataPainter = new Paint();
    private final Paint axisLinePaint = new Paint();
    private final Paint dataLinePaint = new Paint();
    private final Paint progressPainter = new Paint();
    private LinearGradient backgroundGradient;
    private final Paint dataLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private List<Paint> legendPaints, legendBackgroundPaints;
    private int secondaryTextColor, primaryTextColor;
    private float xMax;
    private float yMax;
    private int[] colors;
    private String[] labels;
//    private RectF currentRectPos = new RectF(-1, -1, -1, -1);
    private RectF[] symbolRectPos;
    private Rect textBounds;
    private int currentSelectedIndex = -1;

    public TextLegend(Context context) {
        super(context);
        setup();
    }

    public TextLegend(Context context, AttributeSet attributeSet){
        super(context, attributeSet);
        setup();
    }

    private void setup() {
        dataPainter.setColor(Color.BLUE);
        axisLinePaint.setColor(Color.GRAY);
        axisLinePaint.setStrokeWidth(Tools.dpToPx(getContext(), 3));
        progressPainter.setColor(Color.rgb(186, 28, 54));
        progressPainter.setStrokeWidth(Tools.dpToPx(getContext(), 2));
        progressPainter.setStrokeCap(Paint.Cap.ROUND);
        dataLinePaint.setColor(Tools.getAttrColor(R.attr.colorSecondary, getContext().getTheme()));
        dataLinePaint.setStrokeCap(Paint.Cap.ROUND);
        dataLinePaint.setAntiAlias(true);
        randomBackgroundPaint.setColor(Tools.getAttrColor(R.attr.colorSecondary, getContext().getTheme()));
//        randomBackgroundPaint.setStrokeCap(Paint.Cap.SQUARE);
        randomBackgroundPaint.setAntiAlias(true);
        dataLabelPaint.setTextAlign(Paint.Align.CENTER);
        dataLabelPaint.setAntiAlias(true);
        dataLabelPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textBounds = new Rect();
    }

    public void setData(String[] labels, int[] colors, int primaryTextColor, int secondaryTextColor, int textSize) {
        yMax = getHeight();
        xMax = labels.length;
        this.colors = colors;
        this.labels = labels;
        this.primaryTextColor = primaryTextColor;
        this.secondaryTextColor = secondaryTextColor;
        dataLabelPaint.setTextSize((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, textSize, getResources().getDisplayMetrics()));
        legendPaints = new ArrayList<>();
        for (int color : colors) {
            Paint p = new Paint();
            p.setColor(color);
            p.setAntiAlias(true);
            legendPaints.add(p);
        }
        legendBackgroundPaints = new ArrayList<>();
        for (int color : colors) {
            Paint p = new Paint();
            p.setColor(Tools.manipulateAlpha(color, 0.3f));
            p.setAntiAlias(true);
            legendBackgroundPaints.add(p);
        }
        invalidate();
    }

    public void setTextBold(boolean boldText){
        if(boldText){
            dataLabelPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        }
        else {
            dataLabelPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(backgroundGradient == null){
            backgroundGradient = new LinearGradient(0f, 0f, getWidth(), 0f, LineGraph.manipulateAlphaArray(colors, 0.3f), new float[] { 0.0f, 0.33f, 0.66f, 1f }, Shader.TileMode.CLAMP);
            randomBackgroundPaint.setShader(backgroundGradient);
        }
        canvas.drawRect(0, 0, getWidth(), getHeight(), randomBackgroundPaint);

        dataLabelPaint.setColor(secondaryTextColor);

        if(symbolRectPos == null) {
            symbolRectPos = new RectF[labels.length];
            for (int i = 0; i < labels.length; i++) {
                float xFrom = i / xMax * getWidth();
                float xTo = (i + 1) / xMax * getWidth();
                symbolRectPos[i] = new RectF();
                symbolRectPos[i].set(xFrom, 0, xTo, getHeight());
            }
        }

        for (int i = 0; i < labels.length; i++) {
            if(i != currentSelectedIndex) {
                dataLabelPaint.getTextBounds(labels[i], 0, labels[i].length(), textBounds);
                canvas.drawText(labels[i], symbolRectPos[i].centerX(), symbolRectPos[i].centerY() - textBounds.exactCenterY(), dataLabelPaint);
            }
            else {
                dataLabelPaint.setColor(primaryTextColor);
                float rad = getHeight()/2.0f;
                canvas.drawRoundRect(symbolRectPos[i], rad, rad, legendPaints.get(i % legendPaints.size()));
                dataLabelPaint.getTextBounds(labels[i], 0, labels[i].length(), textBounds);
                canvas.drawText(labels[i], symbolRectPos[i].centerX(), symbolRectPos[i].centerY() - textBounds.exactCenterY(), dataLabelPaint);
                dataLabelPaint.setColor(secondaryTextColor);
            }
        }
    }

    public int getCurrentSelectedIndex() {
        return currentSelectedIndex;
    }

    public void setCurrentSelectedIndex(int currentSelectedIndex) {
        this.currentSelectedIndex = currentSelectedIndex;
        invalidate();
    }
}

