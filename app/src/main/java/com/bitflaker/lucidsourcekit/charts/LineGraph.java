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
    private FrequencyList data = new FrequencyList();
    private final Paint dataPainter = new Paint();
    private final Paint axisLinePaint = new Paint();
    private final Paint dataLinePaint = new Paint();
    private final Paint progressPainter = new Paint();
    private List<float[]> polygonPos;
    private float[] topLeft;
    private float[] topRight;
    private float[] bottomRight;
    private float[] bottomLeft;
    private float xMax;
    private float yMax;
    private boolean drawGradient;
    private float[] positions;
    private int[] colors;
    private double progress;

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
        axisLinePaint.setStrokeWidth(Tools.dpToPx(getContext(), 3));
        dataLinePaint.setColor(Tools.getAttrColor(R.attr.colorSecondary, getContext().getTheme()));
        progressPainter.setColor(Color.rgb(186, 28, 54));
        progressPainter.setStrokeWidth(Tools.dpToPx(getContext(), 2));
        dataLinePaint.setStrokeCap(Paint.Cap.ROUND);
        dataLinePaint.setAntiAlias(true);
        polygonPos = new ArrayList<>();
        topLeft = new float[2];
        topRight = new float[2];
        bottomRight = new float[2];
        bottomLeft = new float[2];
    }

    public void setData(FrequencyList data, float maxValue, float lineWidth, boolean drawGradient, int[] colors, double[] stages) {
        yMax = maxValue;
        xMax = data.getDuration();
        progress = -1;
        // TODO remove test data
        progress = xMax/3.0f;
        this.colors = colors;
        this.data = data;
        this.drawGradient = drawGradient;
        dataLinePaint.setStrokeWidth(Tools.dpToPx(getContext(), lineWidth));
        invalidate();

        List<Float> values = new ArrayList<>();
        values.add((float)(maxValue - stages[0])/maxValue);
        for (int i = 0; i < stages.length-1; i++){
            values.add((float)((stages[i] - stages[i+1])/maxValue) + values.get(i));
        }
        positions = new float[values.size()];
        int i = 0;
        for (Float f : values) {
            positions[i++] = (f != null ? f : Float.NaN);
        }
    }

    public void updateProgress(double progress){
        this.progress = progress;
        invalidate();
    }

    public void changeProgressIndicator(int progressIndicatorColor, float progressIndicatorWidth){
        progressPainter.setColor(progressIndicatorColor);
        progressPainter.setStrokeWidth(Tools.dpToPx(getContext(), progressIndicatorWidth));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        dataLinePaint.setShader(new LinearGradient(0f, 0f, 0f, getHeight(), colors, positions, Shader.TileMode.MIRROR));

        if(progress > -1){
            float realX = (float)progress / xMax * getWidth();
            float realY = (yMax - (float)data.getFrequencyAtDuration(progress)) / yMax * (getHeight() - dataLinePaint.getStrokeWidth()*1.5f) + dataLinePaint.getStrokeWidth();
            canvas.drawLine(realX, realY, realX, getHeight(), progressPainter);
        }

        for (int i = 0; i < data.size(); i++) {
            FrequencyData current = data.get(i);
            float realX = (data.getDurationUntil(i)) / xMax * getWidth() + dataLinePaint.getStrokeWidth()/2.0f;
            float realY = (yMax - current.getFrequency()) / yMax * (getHeight() - dataLinePaint.getStrokeWidth()*1.5f) + dataLinePaint.getStrokeWidth();
            float nextEndX = (data.getDurationUntilAfter(i)) / xMax * getWidth() + dataLinePaint.getStrokeWidth()/2.0f;
            float nextEndY = realY;
            if(!Float.isNaN(current.getFrequencyTo())) {
                nextEndY = (yMax - current.getFrequencyTo()) / yMax * (getHeight() - dataLinePaint.getStrokeWidth()*1.5f) + dataLinePaint.getStrokeWidth();
            }

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

