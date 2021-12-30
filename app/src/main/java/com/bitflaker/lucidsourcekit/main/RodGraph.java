package com.bitflaker.lucidsourcekit.main;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.general.Tools;

import java.util.ArrayList;
import java.util.List;

public class RodGraph extends View {
    private List<DataValue> data = new ArrayList<>();
    private final Paint axisLinePaint = new Paint();
    private final Paint dataLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint dataLinePaint = new Paint();
    private final Paint dataLinePaintBackground = new Paint();
    private float xMax;
    private float yMax;
    private float textHeight;
    private int iconSize;
    private Bitmap[] icons;
    private int minHeight = 0;

    public RodGraph(Context context){
        super(context);
        axisLinePaint.setColor(Color.GRAY);
        dataLinePaintBackground.setColor(Tools.getAttrColor(R.attr.secondColor, getContext().getTheme()));
        dataLinePaintBackground.setStrokeCap(Paint.Cap.ROUND);
        dataLinePaintBackground.setAntiAlias(true);
        dataLinePaint.setColor(Tools.getAttrColor(R.attr.highlight_success, getContext().getTheme()));
        dataLinePaint.setStrokeCap(Paint.Cap.ROUND);
        dataLinePaint.setAntiAlias(true);
        dataLabelPaint.setColor(Tools.getAttrColor(R.attr.darker_text_color, getContext().getTheme()));
        dataLabelPaint.setTextSize((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14, getResources().getDisplayMetrics()));
        dataLabelPaint.setTextAlign(Paint.Align.CENTER);
        dataLabelPaint.setAntiAlias(true);
        axisLinePaint.setStrokeWidth(Tools.dpToPx(getContext(), 3));
    }

    public void setData(List<DataValue> data, float lineWidth, int iconSize, Drawable[] icons) {
        this.data = data;
        this.iconSize = iconSize;
        this.icons = getBitmapsFromDrawableArray(icons);
        this.textHeight = 0;
        yMax = icons.length - 1;
        xMax = data.size();
        dataLinePaintBackground.setStrokeWidth(Tools.dpToPx(getContext(), lineWidth));
        dataLinePaint.setStrokeWidth(Tools.dpToPx(getContext(), lineWidth));
        invalidate();
    }

    private Bitmap[] getBitmapsFromDrawableArray(Drawable[] icons) {
        Bitmap[] bitmaps = new Bitmap[icons.length];
        for (int i = 0; i < bitmaps.length; i++){
            bitmaps[i] = drawableToBitmap(icons[i], iconSize);
        }
        return bitmaps;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float marginForRodRadius = 0.0f;
        float bottomPointWithTextMargin = 0.0f;

        for (int i = 0; i < data.size(); i++) {
            DataValue current = data.get(i);
            int minRadMargin = Tools.dpToPx(getContext(), 4);
            marginForRodRadius = Math.max(iconSize / 2.0f, minRadMargin);
            float xPos = i / xMax * (getWidth() - iconSize) + ((getWidth() - iconSize) / xMax) / 2 + iconSize;
            float bottomPoint = getHeight() - marginForRodRadius;

            int yText = drawTextIfAvailable(canvas, current, xPos);
            if(this.textHeight == 0) { this.textHeight = yText; }

            bottomPointWithTextMargin = drawRodBackground(canvas, marginForRodRadius, xPos, bottomPoint);

            if(current.val > -1){
                float progressHeight = (bottomPointWithTextMargin - (bottomPointWithTextMargin / yMax * current.val)) + ((marginForRodRadius / yMax) * current.val);
                canvas.drawLine(xPos, bottomPointWithTextMargin, xPos, progressHeight, dataLinePaint);
            }
        }

        drawIcons(canvas, marginForRodRadius, bottomPointWithTextMargin);
        clacMinHeight();
    }

    private void clacMinHeight() {
        if(minHeight == 0){
            minHeight = (int)Math.ceil(iconSize * icons.length + this.textHeight);
            setMinimumHeight(minHeight);
        }
    }

    private void drawIcons(Canvas canvas, float marginForRodRadius, float bottomPointWithTextMargin) {
        for (int j = 0; j < icons.length; j++){
            float yPos = (bottomPointWithTextMargin - (bottomPointWithTextMargin / yMax * j)) + ((marginForRodRadius / yMax) * j) - iconSize / 2f;
            canvas.drawBitmap(icons[j], 0, yPos, dataLinePaintBackground);
        }
    }

    private float drawRodBackground(Canvas canvas, float marginForRodRadius, float xPos, float bottomPoint) {
        float bottomPointWithTextMargin = bottomPoint - this.textHeight - Tools.dpToPx(getContext(), 1);
        canvas.drawLine(xPos, bottomPointWithTextMargin, xPos, marginForRodRadius, dataLinePaintBackground);
        return bottomPointWithTextMargin;
    }

    private int drawTextIfAvailable(Canvas canvas, DataValue dataPoint, float xPos) {
        Paint.FontMetrics metric = dataLabelPaint.getFontMetrics();
        int textHeight = (int) Math.ceil(metric.descent - metric.ascent);
        String[] lines = dataPoint.label.split("\n");
        int yText = 0;
        if(dataPoint.label.length() > 0){
            yText = (int)metric.descent;
            for (int j = lines.length - 1; j >= 0; j--){
                canvas.drawText(lines[j], xPos, getHeight() - yText, dataLabelPaint);
                yText += textHeight;
            }
        }
        return yText;
    }

    public static Bitmap drawableToBitmap (Drawable drawable, int size) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, size, size);
        drawable.draw(canvas);

        return bitmap;
    }
}

class DataValue {
    int val;
    String label;

    public DataValue(int val, String label){
        this.val = val;
        this.label = label;
    }
}
