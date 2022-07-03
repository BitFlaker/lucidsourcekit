package com.bitflaker.lucidsourcekit.clock;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Pair;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.res.ResourcesCompat;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.charts.Brainwaves;
import com.bitflaker.lucidsourcekit.general.Tools;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

public class SleepClock extends View {
    private float lineWidth;
    private float innerRadiusOffset = 20;
    private float timeSetterButtonRadius = Tools.dpToPx(getContext(), 18);
    private float digitToButtonSpace = Tools.dpToPx(getContext(), 5);
    private final Paint dataLinePaint = new Paint();
    private final Paint dataLinePaintREM = new Paint();
    private final Paint dataLinePaintFocus = new Paint();
    private final Paint dataLinePaintButton = new Paint();
    private final Paint dataHandPaintMinute = new Paint();
    private final Paint dataHandPaintHour = new Paint();
    private final Paint dataHandPaintSeconds = new Paint();
    private final Paint dataDigitPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint dataHourPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final RectF rf = new RectF();
    private boolean drawHours = false;
    private boolean drawAnalogClock = false;
    private boolean drawTimeSetterButtons = false;
    private Calendar currentTime;
    private Timer clockTimer = new Timer();
    private Rect textBounds = new Rect();
    private float angleBedtime = 0.0f;
    private float angleAlarm = (float)(1.2 * Math.PI);
    private float xBedtime;
    private float yBedtime;
    private float xAlarm;
    private float yAlarm;
    private int movingButtonId = -1;;

    public SleepClock(Context context, AttributeSet as){
        super(context, as);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        dataLinePaint.setColor(Tools.getAttrColor(R.attr.slightElevated, getContext().getTheme()));
        dataLinePaint.setAntiAlias(true);
        dataLinePaint.setStyle(Paint.Style.STROKE);
        dataLinePaint.setStrokeWidth(Tools.dpToPx(getContext(), 2));
        dataLinePaintFocus.setColor(Tools.getAttrColor(R.attr.colorPrimary, getContext().getTheme()));
        dataLinePaintFocus.setAntiAlias(true);
        dataLinePaintFocus.setStyle(Paint.Style.STROKE);
        dataLinePaintFocus.setStrokeWidth(Tools.dpToPx(getContext(), 2));
        dataLinePaintButton.setColor(Tools.getAttrColor(R.attr.colorPrimary, getContext().getTheme()));
        dataLinePaintButton.setAntiAlias(true);
        dataLinePaintButton.setStyle(Paint.Style.FILL);
        dataHandPaintMinute.setColor(Tools.getAttrColor(R.attr.primaryTextColor, getContext().getTheme()));
        dataHandPaintMinute.setAntiAlias(true);
        dataHandPaintMinute.setStrokeWidth(Tools.dpToPx(getContext(), 3));
        dataHandPaintMinute.setStrokeCap(Paint.Cap.ROUND);
        dataHandPaintHour.setColor(Tools.getAttrColor(R.attr.primaryTextColor, getContext().getTheme()));
        dataHandPaintHour.setAntiAlias(true);
        dataHandPaintHour.setStrokeWidth(Tools.dpToPx(getContext(), 2));
        dataHandPaintHour.setStrokeCap(Paint.Cap.ROUND);
        dataHandPaintSeconds.setColor(Tools.getAttrColor(R.attr.colorSecondary, getContext().getTheme()));
        dataHandPaintSeconds.setAntiAlias(true);
        dataHandPaintSeconds.setStrokeWidth(Tools.dpToPx(getContext(), 1));
        dataLinePaintREM.setColor(Tools.getAttrColor(R.attr.colorSecondary, getContext().getTheme()));
        dataLinePaintREM.setAntiAlias(true);
        dataLinePaintREM.setStyle(Paint.Style.STROKE);
        dataLinePaintREM.setStrokeCap(Paint.Cap.ROUND);
        dataLinePaintREM.setStrokeWidth(Tools.dpToPx(getContext(), 4));
        dataDigitPaint.setColor(Tools.getAttrColor(R.attr.primaryTextColor, getContext().getTheme()));
        dataDigitPaint.setTextSize((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12, getResources().getDisplayMetrics()));
        dataDigitPaint.setTextAlign(Paint.Align.CENTER);
        dataDigitPaint.setAntiAlias(true);
        dataHourPaint.setColor(Tools.getAttrColor(R.attr.primaryTextColor, getContext().getTheme()));
        dataHourPaint.setTextSize((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 24, getResources().getDisplayMetrics()));
        dataHourPaint.setTextAlign(Paint.Align.CENTER);
        dataHourPaint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(getWidth() > getHeight()) {
            setMinimumWidth((int) getWidth());
            setMinimumHeight((int) getWidth());
        }
        else {
            setMinimumWidth((int) getHeight());
            setMinimumHeight((int) getHeight());
        }
        if(drawTimeSetterButtons) { innerRadiusOffset = 25; }
        
        final float radius = getWidth() * 0.5f;
        final double angleOffset = (360 / 12.0) * Math.PI / 180.0;
        final float startAngle = (float)(-3 * angleOffset);
        float offset = dataLinePaint.getStrokeWidth();
        if(drawTimeSetterButtons) {
            offset += timeSetterButtonRadius;
        }

        rf.set(offset, offset, getWidth()-offset, getHeight()-offset);

        canvas.drawOval(rf, dataLinePaint);
        float innerRadius = radius - Tools.dpToPx(getContext(), innerRadiusOffset);
        if(drawTimeSetterButtons) {
            innerRadius -= timeSetterButtonRadius + digitToButtonSpace;
        }
        float totalAngle = (float)(startAngle + angleOffset);
        for (int i = 1; i < 13; i++) {
            float xPos = (float)(Math.cos(totalAngle) * innerRadius);
            float yPos = (float)(Math.sin(totalAngle) * innerRadius);
            String currentNumber = Integer.toString(i);
            dataDigitPaint.getTextBounds(currentNumber, 0, currentNumber.length(), textBounds);
            canvas.drawText(currentNumber, radius + xPos, radius + yPos - textBounds.exactCenterY(), dataDigitPaint);
            totalAngle += angleOffset;
        }

        if(drawHours) {
            dataHourPaint.getTextBounds("22h 45m", 0, "22h 45m".length(), textBounds);
            canvas.drawText("22h 45m", radius, radius - textBounds.exactCenterY(), dataHourPaint);
        }

        if(drawAnalogClock && currentTime != null) {
            float minuteHandRadius = radius - Tools.dpToPx(getContext(), innerRadiusOffset) - Tools.dpToPx(getContext(), 16);
            float hourHandRadius = radius - Tools.dpToPx(getContext(), innerRadiusOffset) - Tools.dpToPx(getContext(), 38);
            float secondsHandRadius = radius - Tools.dpToPx(getContext(), innerRadiusOffset) - Tools.dpToPx(getContext(), 5);

            float angleMinute = (float)(startAngle + ((currentTime.get(Calendar.MINUTE)/5.0) * angleOffset));
            float angleHour = (float)(startAngle + (currentTime.get(Calendar.HOUR) + (currentTime.get(Calendar.MINUTE)/60.0)) * angleOffset);
            float angleSeconds = (float)(startAngle + ((currentTime.get(Calendar.SECOND)/5.0) * angleOffset));
            canvas.drawLine(radius, radius, radius + (float)Math.cos(angleMinute) * minuteHandRadius, radius + (float)Math.sin(angleMinute) * minuteHandRadius, dataHandPaintMinute);
            canvas.drawLine(radius, radius, radius + (float)Math.cos(angleHour) * hourHandRadius, radius + (float)Math.sin(angleHour) * hourHandRadius, dataHandPaintHour);
            canvas.drawLine(radius, radius, radius + (float)Math.cos(angleSeconds) * secondsHandRadius, radius + (float)Math.sin(angleSeconds) * secondsHandRadius, dataHandPaintSeconds);
            float radiusCenter = Tools.dpToPx(getContext(), 5);
            canvas.drawCircle(radius, radius, radiusCenter, dataHandPaintHour);
        }

        if(drawTimeSetterButtons) {
            xBedtime = radius + (float)Math.cos(startAngle + angleBedtime) * (radius - timeSetterButtonRadius);
            yBedtime = radius + (float)Math.sin(startAngle + angleBedtime) * (radius - timeSetterButtonRadius);
            xAlarm = radius + (float)Math.cos(startAngle + angleAlarm) * (radius - timeSetterButtonRadius);
            yAlarm = radius + (float)Math.sin(startAngle + angleAlarm) * (radius - timeSetterButtonRadius);
            float startDegree = (float)((startAngle + angleBedtime) * (180.0f / Math.PI));
            float sweepDegree = (float)((angleBedtime < angleAlarm ? angleAlarm - angleBedtime : 2*Math.PI + (angleAlarm - angleBedtime)) * (180.0f / Math.PI));
            canvas.drawArc(rf, startDegree, sweepDegree, false, dataLinePaintFocus);
            for (int i = 0; i < Brainwaves.remAfterMinutes.length; i++) {
                Pair<Integer, Integer> pair = Brainwaves.getRemStageAfterAndDuration(i);
                float angleHour = (float)(startAngle + angleBedtime + ((pair.first/60.0d) * angleOffset));
                float periodDuration = (float)(((pair.second/60.0d*angleOffset) * (180.0f / Math.PI))/2.0);
                float remSeparationSpace = 2.5f;
                canvas.drawArc(rf, (float)(angleHour * (180.0f / Math.PI))-(periodDuration/2.0f)-remSeparationSpace, periodDuration+2*remSeparationSpace, false, dataLinePaint);
                canvas.drawArc(rf, (float)(angleHour * (180.0f / Math.PI))-(periodDuration/2.0f), periodDuration, false, dataLinePaintREM);
            }
            canvas.drawCircle(xBedtime, yBedtime, timeSetterButtonRadius, dataLinePaintButton);
            canvas.drawCircle(xAlarm, yAlarm, timeSetterButtonRadius, dataLinePaintButton);
            Bitmap icon = Tools.drawableToBitmap(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_airline_seat_individual_suite_24, getContext().getTheme()), Color.WHITE, Tools.dpToPx(getContext(), 16));
            canvas.drawBitmap(icon, xBedtime-icon.getWidth()/2.0f, yBedtime-icon.getHeight()/2.0f, dataLinePaint);
            Bitmap alarm = Tools.drawableToBitmap(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_access_alarm_24, getContext().getTheme()), Color.WHITE, Tools.dpToPx(getContext(), 16));
            canvas.drawBitmap(alarm, xAlarm-alarm.getWidth()/2.0f, yAlarm-alarm.getHeight()/2.0f, dataLinePaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        double radius = getWidth() / 2.0d;
        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                movingButtonId = isOnButtonId(event.getX(), event.getY());
            case MotionEvent.ACTION_MOVE:
                setButtonPosition(event, radius);
                break;
            case MotionEvent.ACTION_UP:
                setButtonPosition(event, radius);
                movingButtonId = -1;
                break;
        }
        return true;
    }

    private void setButtonPosition(MotionEvent event, double radius) {
        if(movingButtonId != -1){
            double topX = 0;
            double topY = -10;
            double cPX = event.getX() - radius;
            double cPY = event.getY() - radius;
            double angle = Math.acos((topX * cPX + topY * cPY) / (Math.sqrt(Math.pow(topX, 2)+Math.pow(topY, 2)) * Math.sqrt(Math.pow(cPX, 2)+Math.pow(cPY, 2))));
            if(event.getX() < radius){
                angle = Math.PI - angle + Math.PI;
            }
            if(movingButtonId == 0) { angleBedtime = (float)angle; }
            else if (movingButtonId == 1) { angleAlarm = (float)angle; }
            invalidate();
        }
    }

    private int isOnButtonId(float x, float y) {
        if(Math.pow(x - xBedtime, 2) + Math.pow(y - yBedtime, 2) <= Math.pow(timeSetterButtonRadius, 2)){
            return 0;
        }
        else if(Math.pow(x - xAlarm, 2) + Math.pow(y - yAlarm, 2) <= Math.pow(timeSetterButtonRadius, 2)){
            return 1;
        }
        return -1;
    }

    public void startClock() {
        if(!drawAnalogClock) {
            drawAnalogClock = true;
            currentTime = Calendar.getInstance();
            invalidate();

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.SECOND, cal.get(Calendar.SECOND) + 1);
            cal.set(Calendar.MILLISECOND, 0);

            clockTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    currentTime = Calendar.getInstance();
                    invalidate();
                }
            }, cal.getTimeInMillis() - Calendar.getInstance().getTimeInMillis(), 1000);
        }
    }

    public void stopClock() {
        if(drawAnalogClock) {
            drawAnalogClock = false;
            clockTimer.cancel();
            invalidate();
        }
    }

    public void setData(int innerRadiusOffset) {
        this.innerRadiusOffset = innerRadiusOffset;
        this.lineWidth = 10;
        dataLinePaint.setStrokeWidth(Tools.dpToPx(getContext(), lineWidth));
        invalidate();
    }

    public boolean isDrawingHours() {
        return drawHours;
    }

    public void setDrawHours(boolean drawHours) {
        stopClock();
        this.drawHours = drawHours;
        invalidate();
    }

    public boolean isDrawTimeSetterButtons() {
        return drawTimeSetterButtons;
    }

    public void setDrawTimeSetterButtons(boolean drawTimeSetterButtons) {
        this.drawTimeSetterButtons = drawTimeSetterButtons;
    }
}