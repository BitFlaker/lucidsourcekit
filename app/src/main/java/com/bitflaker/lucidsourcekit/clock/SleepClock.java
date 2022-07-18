package com.bitflaker.lucidsourcekit.clock;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.os.AsyncTask;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Pair;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.charts.Brainwaves;
import com.bitflaker.lucidsourcekit.general.Tools;

import java.util.Calendar;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class SleepClock extends View {
    private float lineWidth;
    private float innerRadiusOffset = 20;
    private float timeSetterButtonRadius = Tools.dpToPx(getContext(), 18);
    private float digitToButtonSpace = Tools.dpToPx(getContext(), 5);
    private final Paint dataLinePaint = new Paint();
    private final Paint dataLinePaintStroker = new Paint();
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
    private float angleAlarm = (float)(0.5 * Math.PI);
    private float xBedtime;
    private float yBedtime;
    private float xAlarm;
    private float yAlarm;
    private int movingButtonId = -1;
    private SweepGradient gradientShaderREM = null;
    private SweepGradient gradientShader = null;
    private Matrix matrixSweepRotationShader = null;
    private int slightElevated2x;
    private int colorSecondary;
    private int colorPrimary;
    private int[] colorsREMRotator;
    private int[] colorsRotator;
    private Vibrator vib;
    private boolean lastStateWasInREM = true;
    private int hoursToSleep = 0;
    private int minutesToSleep = 0;
    private int minutesToFallAsleep = 10;
    private final double angleOffset;
    private final float startAngle;
    private int minutesToBedTime;
    private int hoursToBedTime;
    private int minutesToAlarm;
    private int hoursToAlarm;
    private OnBedtimeChanged mBedtimeChangedListener;
    private OnAlarmTimeChanged mAlarmTimeChangedListener;

    public SleepClock(Context context, AttributeSet as){
        super(context, as);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        slightElevated2x = Tools.getAttrColor(R.attr.slightElevated2x, getContext().getTheme());
        colorSecondary = Tools.getAttrColor(R.attr.colorSecondary, getContext().getTheme());
        colorPrimary = Tools.getAttrColor(R.attr.colorPrimary, getContext().getTheme());
        vib = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        dataLinePaint.setColor(Tools.getAttrColor(R.attr.slightElevated, getContext().getTheme()));
        dataLinePaint.setAntiAlias(true);
        dataLinePaint.setStyle(Paint.Style.STROKE);
        dataLinePaint.setStrokeWidth(Tools.dpToPx(getContext(), 2));
        dataLinePaintStroker.setColor(Tools.getAttrColor(R.attr.slightElevated, getContext().getTheme()));
        dataLinePaintStroker.setAntiAlias(true);
        dataLinePaintStroker.setStyle(Paint.Style.STROKE);
        dataLinePaintStroker.setStrokeWidth(Tools.dpToPx(getContext(), 2));
        dataLinePaintFocus.setColor(colorPrimary);
        dataLinePaintFocus.setAntiAlias(true);
        dataLinePaintFocus.setStyle(Paint.Style.STROKE);
        dataLinePaintFocus.setStrokeWidth(Tools.dpToPx(getContext(), 2));
        dataLinePaintButton.setColor(colorPrimary);
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
        dataHandPaintSeconds.setColor(colorSecondary);
        dataHandPaintSeconds.setAntiAlias(true);
        dataHandPaintSeconds.setStrokeWidth(Tools.dpToPx(getContext(), 1));
        dataLinePaintREM.setColor(colorSecondary);
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
        colorsREMRotator = new int[] { slightElevated2x, colorSecondary, colorSecondary, slightElevated2x };
        colorsRotator = new int[] { slightElevated2x, colorPrimary, colorPrimary, slightElevated2x };
        angleOffset = (360 / 12.0) * Math.PI / 180.0;
        startAngle = (float)(-3 * angleOffset);
        this.setOnTouchListener(preventScrollOnTouch());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float currentAngleOffset = drawTimeSetterButtons ? (float)(angleOffset/2.0f) : (float)angleOffset;

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
        float offset = dataLinePaint.getStrokeWidth();
        if(drawTimeSetterButtons) {
            offset += timeSetterButtonRadius;
        }

        if(drawTimeSetterButtons) {
            if (matrixSweepRotationShader == null) {
                matrixSweepRotationShader = new Matrix();
                float zeroRot = (float) ((90 * Math.PI) / 180.0);
                matrixSweepRotationShader.postRotate((int) ((angleBedtime - zeroRot) * (180 / Math.PI)), getWidth() / 2.0f, getHeight() / 2.0f);
            }

            if (gradientShaderREM == null) {
                float sweepRad = (float) (angleBedtime < angleAlarm ? angleAlarm - angleBedtime : 2 * Math.PI + (angleAlarm - angleBedtime));
                float endPerc = (float) (sweepRad / (2.0 * Math.PI));
                calculateTimes();
//                float hours = (float) Math.max(0, ((24 * sweepRad) / (2 * Math.PI)) - (minutesToFallAsleep / 60.0f));
//                hours -= (((hours % ((int)hours)) * 60) % ((int)((hours % ((int)hours)) * 60)))/60.0f;
//                minutesToSleep = (int) (hours > 1 ? ((hours % ((int) hours)) * 60) : hours * 60);
//                hoursToSleep = (int) hours;
                gradientShaderREM = new SweepGradient(getWidth() / 2.0f, getHeight() / 2.0f, colorsREMRotator, new float[]{0.0f, 0.0f, endPerc, endPerc});
                gradientShader = new SweepGradient(getWidth() / 2.0f, getHeight() / 2.0f, colorsRotator, new float[]{0.0f, 0.0f, endPerc, endPerc});
                gradientShaderREM.setLocalMatrix(matrixSweepRotationShader);
                gradientShader.setLocalMatrix(matrixSweepRotationShader);
                dataLinePaintREM.setShader(gradientShaderREM);
                dataLinePaint.setShader(gradientShader);
            }
        }

        rf.set(offset, offset, getWidth()-offset, getHeight()-offset);

        canvas.drawOval(rf, dataLinePaint);
        float innerRadius = radius - Tools.dpToPx(getContext(), innerRadiusOffset);
        if(drawTimeSetterButtons) {
            innerRadius -= timeSetterButtonRadius + digitToButtonSpace;
        }
        float totalAngle = (float)(startAngle + currentAngleOffset);
        int hCount = drawTimeSetterButtons ? 25 : 13;
        for (int i = 1; i < hCount; i++) {
            float xPos = (float)(Math.cos(totalAngle) * innerRadius);
            float yPos = (float)(Math.sin(totalAngle) * innerRadius);
            String currentNumber = Integer.toString(i);
            dataDigitPaint.getTextBounds(currentNumber, 0, currentNumber.length(), textBounds);
            canvas.drawText(currentNumber, radius + xPos, radius + yPos - textBounds.exactCenterY(), dataDigitPaint);
            totalAngle += currentAngleOffset;
        }

        if(drawHours) {
            String sleepDuration = String.format(Locale.ENGLISH, "%02dh %02dm", hoursToSleep, minutesToSleep);
            dataHourPaint.getTextBounds(sleepDuration, 0, sleepDuration.length(), textBounds);
            canvas.drawText(sleepDuration, radius, radius - textBounds.exactCenterY(), dataHourPaint);
        }

        if(drawAnalogClock && currentTime != null) {
            float minuteHandRadius = radius - Tools.dpToPx(getContext(), innerRadiusOffset) - Tools.dpToPx(getContext(), 9);
            float hourHandRadius = radius - Tools.dpToPx(getContext(), innerRadiusOffset) - Tools.dpToPx(getContext(), 25);
            float secondsHandRadius = radius - Tools.dpToPx(getContext(), innerRadiusOffset) - Tools.dpToPx(getContext(), 5);

            float angleMinute = (float)(startAngle + ((currentTime.get(Calendar.MINUTE)/5.0) * currentAngleOffset));
            float angleHour = (float)(startAngle + (currentTime.get(Calendar.HOUR) + (currentTime.get(Calendar.MINUTE)/60.0)) * currentAngleOffset);
            float angleSeconds = (float)(startAngle + ((currentTime.get(Calendar.SECOND)/5.0) * currentAngleOffset));
            canvas.drawLine(radius, radius, radius + (float)Math.cos(angleMinute) * minuteHandRadius, radius + (float)Math.sin(angleMinute) * minuteHandRadius, dataHandPaintHour);
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
            boolean isAlarmWithinREM = false;
            for (int i = 0; i < Brainwaves.remAfterMinutes.length; i++) {
                Pair<Integer, Integer> pair = Brainwaves.getRemStageAfterAndDuration(i);
                float angleHour = (float)(startAngle + angleBedtime + ((pair.first/60.0d) * currentAngleOffset));
                float periodDurationRad = (float)((pair.second/60.0d*currentAngleOffset));
                float periodDuration = (float)(periodDurationRad * (180.0f / Math.PI));
                float remSeparationSpace = 2.0f;
                float calcAngleAlarm = angleAlarm < angleBedtime ? (float)(angleAlarm + Math.PI * 2) : angleAlarm;
                if(angleHour-(periodDurationRad/2.0f) <= startAngle + calcAngleAlarm && angleHour+(periodDurationRad/2.0f) >= startAngle + calcAngleAlarm){
                    isAlarmWithinREM = true;
                }
                canvas.drawArc(rf, (float)(angleHour * (180.0f / Math.PI))-(periodDuration/2.0f)-remSeparationSpace, periodDuration+2*remSeparationSpace, false, dataLinePaintStroker);
                canvas.drawArc(rf, (float)(angleHour * (180.0f / Math.PI))-(periodDuration/2.0f), periodDuration, false, dataLinePaintREM);
            }
            if(isAlarmWithinREM && !lastStateWasInREM) {
                AsyncTask.execute(() -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vib.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        vib.vibrate(100);
                    }
                });
            }
            lastStateWasInREM = isAlarmWithinREM;
            canvas.drawCircle(xBedtime, yBedtime, timeSetterButtonRadius, dataLinePaintButton);
            Bitmap icon = Tools.drawableToBitmap(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_airline_seat_individual_suite_24, getContext().getTheme()), Color.WHITE, Tools.dpToPx(getContext(), 16));
            canvas.drawBitmap(icon, xBedtime-icon.getWidth()/2.0f, yBedtime-icon.getHeight()/2.0f, dataLinePaint);
            canvas.drawCircle(xAlarm, yAlarm, timeSetterButtonRadius, isAlarmWithinREM ? dataHandPaintSeconds : dataLinePaintButton);
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

    @NonNull
    private View.OnTouchListener preventScrollOnTouch() {
        return (v, event) -> {
            int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    break;
                case MotionEvent.ACTION_UP:
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                    break;
            }

            v.onTouchEvent(event);
            return true;
        };
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
            rotateButtons(angle);
        }
    }

    private void rotateButtons(double angle) {
//            float hours = (float) Math.max(0, ((24*sweepRad)/(2*Math.PI)) - (minutesToFallAsleep / 60.0f));
//            hours -= (((hours % ((int)hours)) * 60) % ((int)((hours % ((int)hours)) * 60)))/60.0f;
//            minutesToSleep = (int) (hours > 1 ? ((hours % ((int)hours)) * 60) : hours * 60);
//            hoursToSleep = (int)hours;
        if(movingButtonId == 0) {
            angleBedtime = (float) angle;
        }
        else if (movingButtonId == 1) {
            angleAlarm = (float) angle;
        }
        float sweepRad = (float)(angleBedtime < angleAlarm ? angleAlarm - angleBedtime : 2*Math.PI + (angleAlarm - angleBedtime));
        float endPerc = (float)(sweepRad / (2.0 * Math.PI));

        calculateTimes();

        if(movingButtonId == 0) {
            float zeroRot = (float)((90 * Math.PI) / 180.0);
            matrixSweepRotationShader.setRotate(0);
            matrixSweepRotationShader.postRotate((int)((angleBedtime -zeroRot)*(180/Math.PI)), getWidth()/2.0f, getHeight()/2.0f);

            gradientShaderREM = new SweepGradient(getWidth() / 2.0f, getHeight() / 2.0f, colorsREMRotator, new float[] { 0.0f, 0.0f, endPerc, endPerc });
            gradientShader = new SweepGradient(getWidth() / 2.0f, getHeight() / 2.0f, colorsRotator, new float[] { 0.0f, 0.0f, endPerc, endPerc });
        }
        else if (movingButtonId == 1) {
            gradientShaderREM = new SweepGradient(getWidth() / 2.0f, getHeight() / 2.0f, colorsREMRotator, new float[] { 0.0f, 0.0f, endPerc, endPerc });
            gradientShader = new SweepGradient(getWidth() / 2.0f, getHeight() / 2.0f, colorsRotator, new float[] { 0.0f, 0.0f, endPerc, endPerc });
        }
        gradientShaderREM.setLocalMatrix(matrixSweepRotationShader);
        gradientShader.setLocalMatrix(matrixSweepRotationShader);
        dataLinePaintREM.setShader(gradientShaderREM);
        dataLinePaint.setShader(gradientShader);
        invalidate();
    }

    public void calculateTimes() {
        float hoursToGoToBed = (float)((24 * angleBedtime) / (2 * Math.PI));
        minutesToBedTime = (int) Math.round(hoursToGoToBed > 1 ? ((hoursToGoToBed - ((int) hoursToGoToBed)) * 60) : hoursToGoToBed * 60);
        hoursToBedTime = (int) hoursToGoToBed;
        float hoursToWakeWithAlarm = (float)((24 * angleAlarm) / (2 * Math.PI));
        minutesToAlarm = (int) Math.round(hoursToWakeWithAlarm > 1 ? ((hoursToWakeWithAlarm - ((int) hoursToWakeWithAlarm)) * 60) : hoursToWakeWithAlarm * 60);
        hoursToAlarm = (int) hoursToWakeWithAlarm;
        int totalMinToBed = hoursToBedTime*60+minutesToBedTime;
        int totalMinToAlarm = hoursToAlarm*60+minutesToAlarm;
        int totalMinutes = (totalMinToAlarm - totalMinToBed);
        if(totalMinToBed > totalMinToAlarm) {
            totalMinutes = totalMinToAlarm + (24*60-totalMinToBed);
        }
        totalMinutes = Math.max(0, totalMinutes - minutesToFallAsleep);
        hoursToSleep = (int)(totalMinutes / 60.0f);
        minutesToSleep = (int)Math.round(hoursToSleep > 0 ? (((totalMinutes/60.0) - hoursToSleep) * 60) : totalMinutes);
        if(minutesToAlarm == 60) {
            minutesToAlarm = 0;
            hoursToAlarm++;
            if(hoursToAlarm == 24) {
                hoursToAlarm = 0;
            }
        }
        if(minutesToBedTime == 60) {
            minutesToBedTime = 0;
            hoursToBedTime++;
            if(hoursToBedTime == 24) {
                hoursToBedTime = 0;
            }
        }
        mBedtimeChangedListener.onEvent(hoursToBedTime, minutesToBedTime);
        mAlarmTimeChangedListener.onEvent(hoursToAlarm, minutesToAlarm);
//        System.out.println("BEDTIME: " + hoursToBedTime + ":" + minutesToBedTime + "  ~~~~~~~~~  " + "ALARM: " + hoursToAlarm + ":" + minutesToAlarm + "  ~~~~~~~~  SLEEP TIME: " + hTime + ":" + mTime);
    }

    private int isOnButtonId(float x, float y) {
        if(Math.pow(x - xAlarm, 2) + Math.pow(y - yAlarm, 2) <= Math.pow(timeSetterButtonRadius, 2)){
            return 1;
        }
        else if(Math.pow(x - xBedtime, 2) + Math.pow(y - yBedtime, 2) <= Math.pow(timeSetterButtonRadius, 2)){
            return 0;
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

    public void setBedTime(int hours, int minutes) {
        angleBedtime = (float)(((hours + (minutes / 60.0)) * (2 * Math.PI)) / 24.0);
        calculateTimes();
        movingButtonId = 0;
        rotateButtons(angleBedtime);
        movingButtonId = -1;
//        invalidate();
    }

    public void setAlarmTime(int hours, int minutes) {
        angleAlarm = (float)(((hours + (minutes / 60.0)) * (2 * Math.PI)) / 24.0);
        calculateTimes();
        movingButtonId = 1;
        rotateButtons(angleAlarm);
        movingButtonId = -1;
//        invalidate();
    }

    public int getMinutesToBedTime() {
        return minutesToBedTime;
    }

    public int getHoursToBedTime() {
        return hoursToBedTime;
    }

    public int getMinutesToAlarm() {
        return minutesToAlarm;
    }

    public int getHoursToAlarm() {
        return hoursToAlarm;
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

    public interface OnBedtimeChanged {
        void onEvent(int hours, int minutes);
    }

    public void setOnBedtimeChangedListener(OnBedtimeChanged eventListener) {
        mBedtimeChangedListener = eventListener;
    }

    public interface OnAlarmTimeChanged {
        void onEvent(int hours, int minutes);
    }

    public void setOnAlarmTimeChangedListener(OnAlarmTimeChanged eventListener) {
        mAlarmTimeChangedListener = eventListener;
    }
}