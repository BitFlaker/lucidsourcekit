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
import java.util.Arrays;
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
    private float[] positionsBuffer;
    private int[] colors;
    private double progress;
    private float bottomLineSpacing;
    private float gradientOpacity;
    private boolean scalePositions;

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
        progressPainter.setColor(Color.rgb(186, 28, 54));
        progressPainter.setStrokeWidth(Tools.dpToPx(getContext(), 2));
        progressPainter.setStrokeCap(Paint.Cap.ROUND);
        dataLinePaint.setColor(Tools.getAttrColor(R.attr.colorSecondary, getContext().getTheme()));
        dataLinePaint.setStrokeCap(Paint.Cap.ROUND);
        dataLinePaint.setAntiAlias(true);
        polygonPos = new ArrayList<>();
        topLeft = new float[2];
        topRight = new float[2];
        bottomRight = new float[2];
        bottomLeft = new float[2];
        scalePositions = false;
    }

    public void setData(FrequencyList data, float maxValue, float lineWidth, int[] colors, double[] stages) {
        yMax = maxValue;
        xMax = data.getDuration();
        progress = -1;
        this.colors = colors;
        this.data = data;
        dataLinePaint.setStrokeWidth(Tools.dpToPx(getContext(), lineWidth));
        invalidate();

        List<Float> values = new ArrayList<>();
        values.add((float)(maxValue - stages[0])/maxValue);
        for (int i = 0; i < stages.length-1; i++){
            values.add((float)((stages[i] - stages[i+1])/maxValue) + values.get(i));
        }
        positions = new float[values.size()];
        positionsBuffer = new float[values.size()];
        int i = 0;
        for (Float f : values) {
            positions[i++] = (f != null ? f : Float.NaN);
        }
        scalePositions = true;
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

        if(scalePositions){
            float ratio = 1-bottomLineSpacing/getHeight();
            for (int i = 0; i < positions.length; i++){
                if(i == 0){
                    positionsBuffer[i] = positions[i] * ratio;
                }
                else {
                    positionsBuffer[i] = positionsBuffer[i-1] + (positions[i]-positions[i-1]) * ratio;
                }
            }
            positions = Arrays.copyOf(positionsBuffer, positionsBuffer.length);
            scalePositions = false;
        }

        dataLinePaint.setShader(new LinearGradient(0f, 0f, 0f, getHeight(), colors, positionsBuffer, Shader.TileMode.MIRROR));

        float radiusMargin = dataLinePaint.getStrokeWidth()/2.0f;
        float drawAreaHeight = getHeight() - dataLinePaint.getStrokeWidth() - bottomLineSpacing;
        float drawAreaWidth = getWidth() - dataLinePaint.getStrokeWidth();
        boolean drewProgress = false;

        for (int i = 0; i < data.size(); i++) {
            FrequencyData current = data.get(i);
            float realX = data.getDurationUntil(i) / xMax * drawAreaWidth + radiusMargin;
            float realY = (yMax - current.getFrequency()) / yMax * drawAreaHeight + radiusMargin;
            float nextEndX = data.getDurationUntilAfter(i) / xMax * drawAreaWidth + radiusMargin;
            float nextEndY = realY;
            if(!Float.isNaN(current.getFrequencyTo())) {
                nextEndY = (yMax - current.getFrequencyTo()) / yMax * drawAreaHeight + radiusMargin;
            }

            topLeft[0] = realX;
            topLeft[1] = realY;
            topRight[0] = nextEndX;
            topRight[1] = nextEndY;
            bottomRight[0] = topRight[0];
            bottomRight[1] = getHeight();
            bottomLeft[0] = topLeft[0];
            bottomLeft[1] = bottomRight[1];
            if(i == 0){
                topLeft[0] -= radiusMargin;
                bottomLeft[0] = topLeft[0];
            }
            else if (i == data.size()-1){
                topRight[0] += radiusMargin;
                bottomRight[0] = topRight[0];
            }

            polygonPos.clear();
            polygonPos.add(topLeft);
            polygonPos.add(topRight);
            polygonPos.add(bottomRight);
            polygonPos.add(bottomLeft);

            if(drawGradient) {
                drawPoly(canvas, Tools.getAttrColor(R.attr.colorSecondary, getContext().getTheme()), polygonPos);
            }

            float realXProgress = (float)progress / xMax * (getWidth()-radiusMargin);
            if(progress > -1 && !drewProgress && realXProgress >= topLeft[0] && realXProgress <= topRight[0]){
                float realYProgress = (yMax - (float)data.getFrequencyAtDuration(progress)) / yMax * drawAreaHeight + radiusMargin + progressPainter.getStrokeWidth()/4.0f;
                canvas.drawLine(realXProgress, realYProgress, realXProgress, getHeight(), progressPainter);
                drewProgress = true;
            }
            canvas.drawLine(realX, realY, nextEndX, nextEndY, dataLinePaint);
        }
    }

    public void setBottomLineSpacing(float bottomLineSpacing) {
        this.bottomLineSpacing = Tools.dpToPx(getContext(), bottomLineSpacing);
        scalePositions = true;
        invalidate();
    }

    public boolean isDrawingGradient() {
        return drawGradient;
    }

    public void setDrawGradient(boolean drawGradient) {
        this.drawGradient = drawGradient;
    }

    public float getGradientOpacity() {
        return gradientOpacity;
    }

    public void setGradientOpacity(float gradientOpacity) {
        this.gradientOpacity = gradientOpacity;
    }

    private void drawPoly(Canvas canvas, int color, List<float[]> points) {
        if (points.size() < 2) { return; }
        Paint polyPaint = new Paint();
        polyPaint.setColor(color);
        polyPaint.setShader(new LinearGradient(0f, 0f, 0f, getHeight(), manipulateAlphaArray(colors, gradientOpacity), positionsBuffer, Shader.TileMode.MIRROR));
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

    public static int[] manipulateAlphaArray(int[] colors, float factor){
        int[] newColors = new int[colors.length];
        for (int i = 0; i < colors.length; i++) {
            newColors[i] = manipulateAlpha(colors[i], factor);
        }
        return newColors;
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

