package com.bitflaker.lucidsourcekit.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.main.binauralbeats.FrequencyData;
import com.bitflaker.lucidsourcekit.main.binauralbeats.FrequencyList;
import com.bitflaker.lucidsourcekit.utils.Tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LineGraph extends View {
    private FrequencyList data = new FrequencyList();
    private final Paint dataPainter = new Paint();
    private final Paint axisLinePaint = new Paint();
    private final Paint dataLinePaint = new Paint();
    private final Paint progressPainter = new Paint();
    private final Paint polyPaint = new Paint();
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
    private boolean scalePositions, wasScaledOnce;
    private float padding_bottom;
    private boolean drawProgressIndicator, doNotIndicateProgress;
    private LinearGradient lgSurfaceOpacity;
    private LinearGradient lgLineOpacity;
    private double lastSetProgress = 0;
    private Matrix lgradOpacMatrix;
    private Path polyPath;
    private boolean isSetUp = false;

    public LineGraph(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setup();
    }

    public LineGraph(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup();
    }

    public LineGraph(Context context){
        super(context);
        setup();
    }

    public LineGraph(Context context, AttributeSet attributeSet){
        super(context, attributeSet);
        setup();
    }

    private void setup() {
        setLayerType(View. LAYER_TYPE_SOFTWARE, null);
        dataPainter.setColor(Color.BLUE);
        axisLinePaint.setColor(Color.GRAY);
        axisLinePaint.setStrokeWidth(Tools.dpToPx(getContext(), 3));
        progressPainter.setColor(Color.rgb(186, 28, 54));
        progressPainter.setStrokeWidth(Tools.dpToPx(getContext(), 2));
        progressPainter.setStrokeCap(Paint.Cap.ROUND);
        dataLinePaint.setColor(Tools.getAttrColor(R.attr.colorSecondary, getContext().getTheme()));
        dataLinePaint.setStrokeCap(Paint.Cap.ROUND);
        dataLinePaint.setAntiAlias(true);
        polyPaint.setColor(Tools.getAttrColor(R.attr.colorSecondary, getContext().getTheme()));
        polyPaint.setStyle(Paint.Style.FILL);
        polygonPos = new ArrayList<>();
        topLeft = new float[2];
        topRight = new float[2];
        bottomRight = new float[2];
        bottomLeft = new float[2];
        scalePositions = false;
        lgradOpacMatrix = new Matrix();
        polyPath = new Path();
        wasScaledOnce = false;
    }

    public void setData(FrequencyList data, float maxValue, float lineWidth, float padding_bottom, boolean doNotIndicateProgress, int[] colors, double[] stages) {
        // TODO: setting a second time overlays with previous and makes shader opacity 100%
        yMax = maxValue;
        xMax = data.getDuration();
        progress = doNotIndicateProgress ? xMax : 0;
        this.colors = colors;
        this.data = data;
        this.doNotIndicateProgress = doNotIndicateProgress;
        this.padding_bottom = Tools.dpToPx(getContext(), padding_bottom);
        dataLinePaint.setStrokeWidth(Tools.dpToPx(getContext(), lineWidth));

        int[] colorsLineProgress = new int[]{Color.argb(0, 0, 0, 0), Color.argb(255, 24, 24, 24)};
        int[] colorsSurfaceProgress = new int[]{Color.argb(255, 255, 255, 255), Color.argb(0, 0, 0, 0)};
        float[] emptyProgressTwoFields = new float[]{0.0f, 0.0f};

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
        if(!wasScaledOnce && doNotIndicateProgress) { scalePositions = true; }
        else if(!doNotIndicateProgress) { scalePositions = true; }
        lgLineOpacity = new LinearGradient(0f, 0f, 1f, 0f, colorsLineProgress, emptyProgressTwoFields, Shader.TileMode.CLAMP);
        lgSurfaceOpacity = new LinearGradient(0f, 0f, 1f, 0f, colorsSurfaceProgress, emptyProgressTwoFields, Shader.TileMode.CLAMP);
        isSetUp = true;
        invalidate();
    }

    public void updateProgress(double progress) {
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

        if(!isSetUp) {
            return;
        }

        float radiusMargin = dataLinePaint.getStrokeWidth() / 2.0f;

        if(positions == null || colors == null || data == null) {
            return;
        }

        if(scalePositions) {
            float ratio = 1 - bottomLineSpacing / (getHeight()-padding_bottom);
            for (int i = 0; i < positions.length; i++) {
                if(i == 0) {
                    positionsBuffer[i] = positions[i] * ratio;
                }
                else {
                    positionsBuffer[i] = positionsBuffer[i-1] + (positions[i]-positions[i-1]) * ratio;
                }
            }
            positions = Arrays.copyOf(positionsBuffer, positionsBuffer.length);
            LinearGradient lgSurfacePaint = new LinearGradient(0f, 0f, 0f, getHeight() - padding_bottom, manipulateAlphaArray(new int[]{Color.rgb(32, 32, 32), Color.rgb(32, 32, 32)}, gradientOpacity), new float[]{0.0f, 1.0f}, Shader.TileMode.CLAMP);
            LinearGradient lgLinePaint = new LinearGradient(0f, 0f, 0f, getHeight() - padding_bottom, colors, positionsBuffer, Shader.TileMode.MIRROR);
            ComposeShader csDataLine = new ComposeShader(lgLinePaint, lgLineOpacity, PorterDuff.Mode.SRC_ATOP);
            ComposeShader csPolyPaint = new ComposeShader(lgSurfacePaint, lgSurfaceOpacity, PorterDuff.Mode.MULTIPLY);
            dataLinePaint.setShader(csDataLine);
            polyPaint.setShader(csPolyPaint);
            scalePositions = false;
            wasScaledOnce = true;
        }

        float drawAreaHeight = (getHeight() - padding_bottom) - dataLinePaint.getStrokeWidth() - bottomLineSpacing;
        float drawAreaWidth = getWidth() - dataLinePaint.getStrokeWidth();
        boolean drewProgress = false;
        float realXProgress = (float)progress / xMax * (getWidth() - radiusMargin);

        if (progress != lastSetProgress) {
            // TODO: Check why the surface seems to lag behind the line when updating
            lgSurfaceOpacity.getLocalMatrix(lgradOpacMatrix);
            if (!doNotIndicateProgress){ lgradOpacMatrix.postTranslate(((float)(progress-lastSetProgress)/xMax) * getWidth(), 0); }
            else { lgradOpacMatrix.postTranslate(getWidth(), 0); }
            lastSetProgress = progress;
            lgSurfaceOpacity.setLocalMatrix(lgradOpacMatrix);
            lgLineOpacity.setLocalMatrix(lgradOpacMatrix);
        }

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
                drawPoly(canvas, polygonPos);
            }

            if(drawProgressIndicator && progress > -1 && !drewProgress && realXProgress >= topLeft[0] && realXProgress <= topRight[0]){
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
        invalidate();
    }

    public boolean isDrawingProgressIndicator() {
        return drawProgressIndicator;
    }

    public void setDrawProgressIndicator(boolean drawProgressIndicator) {
        this.drawProgressIndicator = drawProgressIndicator;
        invalidate();
    }

    public float getGradientOpacity() {
        return gradientOpacity;
    }

    public void setGradientOpacity(float gradientOpacity) {
        this.gradientOpacity = gradientOpacity;
        invalidate();
    }

    private void drawPoly(Canvas canvas, List<float[]> points) {
        if (points.size() < 2) { return; }
        polyPath.reset();
        polyPath.moveTo(points.get(0)[0], points.get(0)[1]);
        int i, size;
        size = points.size();
        for (i = 0; i < size; i++) {
            polyPath.lineTo(points.get(i)[0], points.get(i)[1]);
        }
        polyPath.lineTo(points.get(0)[0], points.get(0)[1]);
        canvas.drawPath(polyPath, polyPaint);
    }

    public static int[] manipulateAlphaArray(int[] colors, float factor){
        int[] newColors = new int[colors.length];
        for (int i = 0; i < colors.length; i++) {
            newColors[i] = Tools.manipulateAlpha(colors[i], factor);
        }
        return newColors;
    }

    public void setBottomLinePadding(int padding) {
        this.padding_bottom = padding;
        invalidate();
    }

    public void resetProgress() {
        if (lgSurfaceOpacity == null) return;
        lgSurfaceOpacity.getLocalMatrix(lgradOpacMatrix);
        if (!doNotIndicateProgress) {
            lgradOpacMatrix.postTranslate(-(float)((lastSetProgress/xMax) * getWidth()), 0);
        }
        lgSurfaceOpacity.setLocalMatrix(lgradOpacMatrix);
        lastSetProgress = progress = 0;
        invalidate();
    }

    public float getDurationProgress() {
        return xMax;
    }
}

