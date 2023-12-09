package com.bitflaker.lucidsourcekit.charts;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.general.Tools;

public class CircleGraph extends View {
    private int val1;
    private int val2;
    private float lineWidth;
    private final Paint dataLinePaint = new Paint();
    private final Paint dataLinePaint2 = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint dataLinePaintBackgroundEraser = new Paint();
    private final Paint dataLinePaintSpaceEraser = new Paint();
    private final RectF rf = new RectF();

    public CircleGraph(Context context, AttributeSet as){
        super(context, as);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        dataLinePaint.setColor(Tools.getAttrColor(R.attr.colorSecondary, getContext().getTheme()));
        dataLinePaint.setAntiAlias(true);
        dataLinePaint2.setColor(Tools.getAttrColor(R.attr.colorPrimary, getContext().getTheme()));
        dataLinePaint2.setAntiAlias(true);
        dataLinePaintBackgroundEraser.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        dataLinePaintBackgroundEraser.setAntiAlias(true);
        dataLinePaintSpaceEraser.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        dataLinePaintSpaceEraser.setAntiAlias(true);
        val1 = 2;
        val2 = 3;
        lineWidth = Tools.dpToPx(context, 15);
        dataLinePaintSpaceEraser.setStrokeWidth(Tools.dpToPx(getContext(), Tools.dpToPx(context, 1.25)));
    }

    public void setData(int val1, int val2, float lineWidth, float space) {
        this.val1 = val1;
        this.val2 = val2;
        this.lineWidth = lineWidth;
        dataLinePaintSpaceEraser.setStrokeWidth(Tools.dpToPx(getContext(), space));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final float height = getHeight();
        final float width = getWidth();
        final float radius = width > height ? height * 0.5f : width * 0.5f;
        final float centerX = getWidth() * 0.5f;
        final float centerY = getHeight() * 0.5f;
        final float degVal1 = (360.0f * val1) / (val1 + val2);

        rf.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);

        canvas.drawArc(rf, 270, degVal1, true, dataLinePaint);
        canvas.drawArc(rf, 270 + degVal1, 360 - degVal1, true, dataLinePaint2);

        if(val1 != 0 && val2 != 0){
            double angle = 270 + degVal1;       // degrees
            angle = angle * Math.PI / 180.0;    // radians
            double xVal = centerX + Math.cos(angle) * centerX;
            double yVal = centerY + Math.sin(angle) * centerY;
            canvas.drawLine(centerX, centerY, centerX, 0, dataLinePaintSpaceEraser);
            canvas.drawLine(centerX, centerY, (float)xVal, (float)yVal, dataLinePaintSpaceEraser);
        }

        canvas.drawCircle(centerX, centerY, radius - lineWidth, dataLinePaintBackgroundEraser);
    }
}