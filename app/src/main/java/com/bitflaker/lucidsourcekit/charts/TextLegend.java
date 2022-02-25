package com.bitflaker.lucidsourcekit.charts;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.ColorInt;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.general.Tools;

import java.util.ArrayList;
import java.util.List;

public class TextLegend extends View {
    private final Paint dataPainter = new Paint();
    private final Paint axisLinePaint = new Paint();
    private final Paint dataLinePaint = new Paint();
    private final Paint progressPainter = new Paint();
    private final Paint dataLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private List<Paint> legendPaints;
    private float xMax;
    private float yMax;
    private String[] labels;
    private RectF currentRectPos = new RectF(-1, -1, -1, -1);

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
        dataLabelPaint.setTextAlign(Paint.Align.CENTER);
        dataLabelPaint.setAntiAlias(true);
        dataLabelPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
    }

    public void setData(String[] labels, int[] colors, int textColor, int textSize) {
        yMax = getHeight();
        xMax = labels.length;
        this.labels = labels;
        dataLabelPaint.setColor(manipulateAlpha(textColor, 0.6f));
        dataLabelPaint.setTextSize((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, textSize, getResources().getDisplayMetrics()));
        legendPaints = new ArrayList<>();
        for (int color : colors) {
            Paint p = new Paint();
            p.setColor(color);
            p.setAntiAlias(true);
            legendPaints.add(p);
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

        for (int i = 0; i < labels.length; i++){
            float xFrom = i/xMax * getWidth();
            float xTo = (i+1)/xMax * getWidth();
            currentRectPos.set(xFrom, 0, xTo, getHeight());
            canvas.drawRect(currentRectPos, legendPaints.get(i % legendPaints.size()));
            Paint.FontMetrics metric = dataLabelPaint.getFontMetrics();
            canvas.drawText(labels[i], currentRectPos.centerX(), currentRectPos.centerY()+metric.descent, dataLabelPaint);
        }
    }

    @ColorInt
    public static int manipulateAlpha(@ColorInt int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }
}

