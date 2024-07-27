package com.bitflaker.lucidsourcekit.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.utils.Tools;

import java.util.ArrayList;
import java.util.List;

public class QuadraticFunctionCurve extends View {
    private final Paint dataLinePaint = new Paint();
    private float lineWidth;
    private List<PointF> curveData;
    private List<PointF> oldCurveData;
    private float maxY;
    private float a, b, c;
    private int goalCount;
    private Thread curveCalcThread;
    private boolean isZeroMinValue = false;

    public QuadraticFunctionCurve(Context context) {
        super(context);
        setup();
    }

    public QuadraticFunctionCurve(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    private void setup() {
        dataLinePaint.setStrokeCap(Paint.Cap.ROUND);
        dataLinePaint.setAntiAlias(true);
        dataLinePaint.setStrokeWidth(50);
        dataLinePaint.setStyle(Paint.Style.STROKE);
        dataLinePaint.setColor(Tools.getAttrColor(R.attr.colorSecondary, getContext().getTheme()));
    }

    public void setZeroMinValue(boolean isZeroMinValue) {
        this.isZeroMinValue = isZeroMinValue;
    }

    public boolean isZeroMinValue() {
        return isZeroMinValue;
    }

    public void setData(float lineWidth, float maxY, float a, float b, float c, int goalCount) {
        this.maxY = maxY;
        this.a = a;
        this.b = b;
        this.c = c;
        this.goalCount = goalCount;
        this.lineWidth = Tools.dpToPx(getContext(), lineWidth);
        if(curveData != null) {
            oldCurveData = new ArrayList<>();
            oldCurveData.addAll(curveData);
        }
        curveData = null;
        dataLinePaint.setStrokeWidth(this.lineWidth);
        curveCalcThread = new Thread(() -> {
            curveData = new ArrayList<>();
            for (float i = 0; i <= goalCount; i += goalCount / ((float)getWidth()/(lineWidth))) {
                if(curveData == null) { return; }
                float value = (float)(a * Math.pow(i, 2) + b * i + c);
                if (isZeroMinValue) { value = Math.max(0.0f, value); }
                curveData.add(new PointF(i, value));
            }
            invalidate();
        });
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        List<PointF> drawPoints = curveData;

        if(curveData == null) {
            curveCalcThread.start();
            drawPoints = oldCurveData;
        }
        //else {
        if(drawPoints != null){
            float halfWidth = this.lineWidth / 2.0f;
            int j = 0;

            for (int i = 0; i < drawPoints.size(); i++) {
                float xVal = (drawPoints.get(j).x / goalCount) * getWidth() - halfWidth;
                float yVal = getHeight() - ((drawPoints.get(j).y / maxY) * getHeight() + halfWidth);
                canvas.drawCircle(xVal, yVal, halfWidth, dataLinePaint);
                j++;
            }
        }
        //}
    }

    public float getA() {
        return a;
    }

    public float getB() {
        return b;
    }

    public float getC() {
        return c;
    }
}
