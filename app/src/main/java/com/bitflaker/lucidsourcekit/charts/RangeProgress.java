package com.bitflaker.lucidsourcekit.charts;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.general.Tools;

public class RangeProgress extends View {
    private final Paint dataLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint dataLinePaint = new Paint();
    private final Paint dataLinePaintBackground = new Paint();
    private float value;
    private float xMax;
    private float textHeight;
    private int iconSize;
    private Bitmap icon;
    private Drawable iconDrawable;
    private String text;
    private String label;
    private int minHeight = 0;
    private final Rect textBounds = new Rect();
    private int[] progressColors;
    private int[] textColors;
    private float[] positions;
    private float percentage;
    private float lastWidth = 0;
    private LinearGradient lineShader;
    private LinearGradient textShader;
    private boolean initialized;

    public RangeProgress(Context context){
        super(context);
        setup();
    }

    public RangeProgress(Context context, AttributeSet attributeSet){
        super(context, attributeSet);
        setup();
    }

    private void setup() {
        dataLinePaintBackground.setColor(Tools.getAttrColor(R.attr.backgroundColor, getContext().getTheme()));
        dataLinePaintBackground.setStrokeCap(Paint.Cap.ROUND);
        dataLinePaintBackground.setAntiAlias(true);
        dataLinePaintBackground.setStrokeWidth(0);
        dataLinePaint.setColor(Tools.getAttrColor(R.attr.colorSecondary, getContext().getTheme()));
        dataLinePaint.setStrokeCap(Paint.Cap.ROUND);
        dataLinePaint.setAntiAlias(true);
        dataLinePaint.setStrokeWidth(0);
        dataLabelPaint.setColor(Tools.getAttrColor(R.attr.primaryTextColor, getContext().getTheme()));
        dataLabelPaint.setTextSize((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14, getResources().getDisplayMetrics()));
        dataLabelPaint.setTextAlign(Paint.Align.LEFT);
        dataLabelPaint.setFakeBoldText(true);
        dataLabelPaint.setAntiAlias(true);
        progressColors = new int[] {Tools.getAttrColor(R.attr.colorSecondary, getContext().getTheme()), Tools.getAttrColor(R.attr.backgroundColor, getContext().getTheme())};
        textColors = new int[] { Tools.getAttrColor(R.attr.colorOnSecondary, getContext().getTheme()), Tools.manipulateAlpha(Tools.getAttrColor(R.attr.secondaryTextColor, getContext().getTheme()), 0.7f) };
        positions = new float[] { 0f, 0f };
    }

    public void setBackgroundAttrColor(int color){
        dataLinePaintBackground.setColor(Tools.getAttrColor(color, getContext().getTheme()));
        progressColors = new int[] {Tools.getAttrColor(R.attr.colorSecondary, getContext().getTheme()), Tools.getAttrColor(color, getContext().getTheme())};
        invalidate();
    }

    public void setData(float maxValue, float value, String label, Drawable icon, String text) {
        this.value = value;
        this.label = label;
        this.text = text;
        if(this.text != null && this.text.equals("NaN")){
            this.text = "-";
        }
        this.xMax = maxValue;
        this.iconDrawable = icon;
        this.textHeight = 0;
        percentage = value / xMax;
        if(maxValue == 0 || Float.isNaN(value)){
            percentage = 0;
            this.text = "-";
        }
        initialized = true;
        invalidate();
    }

    public boolean isInitialized() {
        return initialized;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float minRadMargin = getHeight() / 2.0f;
        float textMargin = 27;

        if(lineShader == null || textShader == null || positions[0] != percentage || positions[1] != percentage || lastWidth != getWidth()){
            positions[0] = percentage;
            positions[1] = percentage;
            lineShader = new LinearGradient(0f, 0f, getWidth(), 0f, progressColors, positions, Shader.TileMode.MIRROR);
            textShader = new LinearGradient(0f, 0f, getWidth(), 0f, textColors, positions, Shader.TileMode.MIRROR);
        }

        if(dataLinePaintBackground.getStrokeWidth() == 0){
            dataLinePaintBackground.setStrokeWidth(Tools.dpToPx(getContext(), Tools.pxToDp(getContext(), getHeight())));
        }

        if(dataLinePaint.getStrokeWidth() == 0){
            dataLinePaint.setStrokeWidth(Tools.dpToPx(getContext(), Tools.pxToDp(getContext(), getHeight())));
        }

        if(icon == null && iconDrawable != null){
            iconSize = (int) (getHeight() / 5.0f * 3.25);
            icon = drawableToBitmap(iconDrawable, iconSize);
        }

        dataLinePaintBackground.setShader(lineShader);
        canvas.drawLine(minRadMargin, getHeight() / 2.0f, getWidth()-minRadMargin, getHeight() / 2.0f, dataLinePaintBackground);

        if(label != null && label.length() > 0) {
            dataLabelPaint.getTextBounds(label, 0, label.length(), textBounds);
            dataLabelPaint.setShader(textShader);
            canvas.drawText(label, textMargin, getHeight() / 2.0f - textBounds.exactCenterY(), dataLabelPaint);
        }

        if(icon != null) {
            float begin = getWidth() - iconSize - (getHeight() - iconSize) / 2.0f - 5; // margin of 5 to the right in order to see the end a bit better if the bar is not totally filled
            float end = begin + iconSize;
            float current = percentage * getWidth();
            float iconFillPercentage = 1;
            if(current > begin && current < end) {
                float within = current - begin;
                iconFillPercentage = within / iconSize;
            }
            else if(current <= begin) {
                iconFillPercentage = 0;
            }
            canvas.drawBitmap(addGradient(icon, iconSize, iconFillPercentage), begin, getHeight()/2.0f-iconSize/2.0f, dataLabelPaint);
        }
        else if(text != null && text.length() > 0) {
            dataLabelPaint.getTextBounds(text, 0, text.length(), textBounds);
            canvas.drawText(text, getWidth() - textMargin - textBounds.width(), getHeight()/2.0f - textBounds.exactCenterY(), dataLabelPaint);
        }

        lastWidth = getWidth();
    }

    public static Bitmap drawableToBitmap (Drawable drawable, int size) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, size, size);
        drawable.draw(canvas);

        return bitmap;
    }

    public Bitmap addGradient(Bitmap src, int size, float pos) {
        Bitmap result = Bitmap.createBitmap(size,size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);

        canvas.drawBitmap(src, 0, 0, null);

        Paint paint = new Paint();
        LinearGradient shader = new LinearGradient(0,0,size,0, textColors, new float[] { pos, pos }, Shader.TileMode.CLAMP);
        paint.setShader(shader);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawRect(0,0,size,size,paint);

        return result;
    }
}