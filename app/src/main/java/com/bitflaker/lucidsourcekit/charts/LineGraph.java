package com.bitflaker.lucidsourcekit.charts;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.ColorInt;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.general.Tools;

import java.util.ArrayList;
import java.util.List;

public class LineGraph extends View {
    private List<DataPoint> data = new ArrayList<>();
    private final Paint dataPainter = new Paint();
    private final Paint axisLinePaint = new Paint();
    private final Paint dataLinePaint = new Paint();
    private List<float[]> polygonPos;
    private float[] topLeft;
    private float[] topRight;
    private float[] bottomRight;
    private float[] bottomLeft;
    private float xMax;
    private float yMax;
    private boolean drawGradient;
    private float[] positions = null;
    private int[] colors = {
            Color.rgb(54, 153, 234), // beta = awake
            Color.rgb(87, 97, 224), // alpha = relaxed
            Color.rgb(121, 74, 209), // theta = dreams
            Color.rgb(108, 45, 142) // delta = deep sleep
    };

    public LineGraph(Context context){
        super(context);
        setup();
    }

    public LineGraph(Context context, AttributeSet attributeSet){
        super(context, attributeSet);
        setup();
    }

    private void setup() {
        dataPainter.setColor(Color.BLUE);
        axisLinePaint.setColor(Color.GRAY);
        dataLinePaint.setColor(Tools.getAttrColor(R.attr.colorSecondary, getContext().getTheme()));
        dataLinePaint.setStrokeCap(Paint.Cap.ROUND);
        dataLinePaint.setAntiAlias(true);
        axisLinePaint.setStrokeWidth(Tools.dpToPx(getContext(), 3));
        polygonPos = new ArrayList<>();
        topLeft = new float[2];
        topRight = new float[2];
        bottomRight = new float[2];
        bottomLeft = new float[2];
    }

    public void setData(List<DataPoint> data, float maxValue, float lineWidth, boolean drawGradient) {
        yMax = maxValue;
        xMax = data.size();
        this.data = data;
        this.drawGradient = drawGradient;
        dataLinePaint.setStrokeWidth(Tools.dpToPx(getContext(), lineWidth));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        dataLinePaint.setShader(new LinearGradient(0f, 0f, 0f, getHeight(), colors, positions, Shader.TileMode.MIRROR));

        for (int i = 0; i < data.size(); i++) {
            float realX = i / xMax * getWidth()+dataLinePaint.getStrokeWidth()/2.0f;
            float realY = (yMax - data.get(i).getValue()) / yMax * (getHeight() - dataLinePaint.getStrokeWidth()*1.5f) + dataLinePaint.getStrokeWidth();
            if(i < data.size() - 1) {
                DataPoint nextPoint = data.get(i + 1);
                int incr = 0;
                float val = nextPoint.getValue();
                while(Float.isNaN(nextPoint.getValue())){
                    incr++;
                    nextPoint = data.get(i + 1 + incr);
                    val = nextPoint.getValue();
                }
                i += incr;
                float nextEndX = (i+1) / xMax * getWidth()+dataLinePaint.getStrokeWidth()/2.0f;
                float nextEndY = (yMax - val) / yMax * (getHeight() - dataLinePaint.getStrokeWidth()*1.5f) + dataLinePaint.getStrokeWidth();

                topLeft[0] = realX;
                topLeft[1] = realY;
                topRight[0] = nextEndX;
                topRight[1] = nextEndY;
                bottomRight[0] = nextEndX;
                bottomRight[1] = getHeight();
                bottomLeft[0] = realX;
                bottomLeft[1] = getHeight();

                polygonPos.clear();
                polygonPos.add(topLeft);
                polygonPos.add(topRight);
                polygonPos.add(bottomRight);
                polygonPos.add(bottomLeft);
                if(drawGradient) {
                    drawPoly(canvas, Tools.getAttrColor(R.attr.colorSecondary, getContext().getTheme()), polygonPos);
                }

                canvas.drawLine(realX, realY, nextEndX, nextEndY, dataLinePaint);
            }
            //canvas.drawCircle(realX, realY, 10f, dataPainter);
        }
        // Axis
        //canvas.drawLine(0f, 0f, 0f, getHeight(), axisLinePaint);
        //canvas.drawLine(0f, getHeight(), getWidth(), getHeight(), axisLinePaint);
    }

    private void drawPoly(Canvas canvas, int color, List<float[]> points) {
        if (points.size() < 2) { return; }
        Paint polyPaint = new Paint();
        polyPaint.setColor(color);
        polyPaint.setShader(new LinearGradient(0, 0, 0, getHeight(), manipulateAlpha(color, 0.15f), Color.TRANSPARENT, Shader.TileMode.MIRROR));
        polyPaint.setStyle(Paint.Style.FILL);
        Path p = new Path();
        p.moveTo(points.get(0)[0], points.get(0)[1]);
        int i, size;
        size = points.size();
        for (i = 0; i < size; i++) {
            p.lineTo(points.get(i)[0], points.get(i)[1]);
        }
        p.lineTo(points.get(0)[0], points.get(0)[1]);
        canvas.drawPath(p, polyPaint);
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

