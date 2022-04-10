package com.bitflaker.lucidsourcekit.charts;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.general.Tools;

import java.util.ArrayList;
import java.util.List;

public class QuadraticFunctionCurve extends View {
    private final Paint dataLinePaint = new Paint();
    private float lineWidth;
    private List<PointF> curveData;
    private float maxY;
    private float a, b, c;
    private int goalCount;
    private Thread curveCalcThread;

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

    public void setData(float lineWidth, float maxY, float a, float b, float c, int goalCount) {
        this.maxY = maxY;
        this.a = a;
        this.b = b;
        this.c = c;
        this.goalCount = goalCount;
        this.lineWidth = Tools.dpToPx(getContext(), lineWidth);
        curveData = null;
        dataLinePaint.setStrokeWidth(this.lineWidth);
        curveCalcThread = new Thread(() -> {
            curveData = new ArrayList<>();
            for (float i = 0; i <= goalCount; i += goalCount / ((float)getWidth()/(lineWidth*2))) {
                curveData.add(new PointF(i, (float)(a * Math.pow(i, 2) + b * i + c)));
            }
            invalidate();
        });
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(curveData == null) {
            curveCalcThread.start();
        }
        else {
            float halfWidth = this.lineWidth / 2.0f;
            int j = 0;

            for (int i = 0; i < curveData.size(); i++) {
                float xVal = (curveData.get(j).x / goalCount) * getWidth() - halfWidth;
                float yVal = getHeight() - ((curveData.get(j).y / maxY) * getHeight() + halfWidth);
                canvas.drawCircle(xVal, yVal, halfWidth, dataLinePaint);
                j++;
            }
        }
    }
}
