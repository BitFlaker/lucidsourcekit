package com.bitflaker.lucidsourcekit.alarms;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.core.content.res.ResourcesCompat;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.general.Tools;

public class OutsideSlider extends View {
    private final Paint indicatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint dataLinePaint = new Paint();
    private final Paint dataLinePaintBackground = new Paint();
    private Bitmap leftIcon, rightIcon, leftArrow, rightArrow;
    private OnLeftSideSelected mLeftSideSelectedListener;
    private OnRightSideSelected mRightSideSelectedListener;
    private OnFadedAway mFadedAwayListener;
    private boolean hasToSwipe = false;
    private boolean isHoldSwiping = false;
    private float pressPointOffset = 0;
    private float buttonSelectorRadius;
    private float xButtonSelector = -1;
    private float sidePadding = Tools.dpToPx(getContext(), 32);
    private float currentArrowMargin;
    private int primColor = Tools.getAttrColor(R.attr.primaryTextColor, getContext().getTheme());
    private ValueAnimator valAnim;
    private int indicatorMoveTo = Tools.dpToPx(getContext(), 50);
    private boolean isSwiping = false;

    public OutsideSlider(Context context){
        super(context);
        setup();
    }

    public OutsideSlider(Context context, AttributeSet attributeSet){
        super(context, attributeSet);
        setup();
    }

    private void setup() {
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
        primColor = Tools.getAttrColor(R.attr.primaryTextColor, getContext().getTheme());
        dataLinePaintBackground.setColor(Tools.getAttrColor(R.attr.backgroundColor, getContext().getTheme()));
        dataLinePaintBackground.setStrokeCap(Paint.Cap.ROUND);
        dataLinePaintBackground.setAntiAlias(true);
        dataLinePaintBackground.setStrokeWidth(0);
        dataLinePaint.setColor(Tools.getAttrColor(R.attr.primaryTextColor, getContext().getTheme()));
        dataLinePaint.setStrokeWidth(Tools.dpToPx(getContext(), 2));
        dataLinePaint.setStyle(Paint.Style.STROKE);
        dataLinePaint.setAntiAlias(true);
        indicatorPaint.setColor(primColor);
        indicatorPaint.setAntiAlias(true);
        buttonSelectorRadius = Tools.dpToPx(getContext(), 24);

        this.leftIcon = Tools.drawableToBitmap(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_check_24, getContext().getTheme()), primColor, Tools.dpToPx(getContext(), 32));
        this.rightIcon = Tools.drawableToBitmap(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_snooze_24, getContext().getTheme()), primColor, Tools.dpToPx(getContext(), 32));
        this.leftArrow = Tools.drawableToBitmap(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_round_keyboard_double_arrow_left_24, getContext().getTheme()), Tools.dpToPx(getContext(), 24));
        this.rightArrow = Tools.drawableToBitmap(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_round_keyboard_double_arrow_right_24, getContext().getTheme()), Tools.dpToPx(getContext(), 24));
    }

    public void setData(Drawable leftIcon, Drawable rightIcon) {
        this.leftIcon = Tools.drawableToBitmap(leftIcon, primColor, Tools.dpToPx(getContext(), 32));
        this.rightIcon = Tools.drawableToBitmap(rightIcon, primColor, Tools.dpToPx(getContext(), 32));
        this.leftArrow = Tools.drawableToBitmap(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_round_keyboard_double_arrow_left_24, getContext().getTheme()), Tools.dpToPx(getContext(), 24));
        this.rightArrow = Tools.drawableToBitmap(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_round_keyboard_double_arrow_right_24, getContext().getTheme()), Tools.dpToPx(getContext(), 24));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(xButtonSelector == -1){
            xButtonSelector = getWidth() / 2.0f;
            valAnim = ValueAnimator.ofInt(0, indicatorMoveTo);
            valAnim.setDuration(1300);
            valAnim.setInterpolator(new LinearInterpolator());
            valAnim.addUpdateListener((valueAnimator) -> {
                currentArrowMargin = (int) valueAnimator.getAnimatedValue();
                invalidate();
            });
            valAnim.setRepeatCount(-1);
            valAnim.setRepeatMode(ValueAnimator.RESTART);
            valAnim.start();
        }
        indicatorPaint.setColor(Tools.manipulateAlpha(primColor, getAlphaValue()));
        canvas.drawBitmap(leftArrow, getWidth()/2.0f - sidePadding / 2.0f - leftArrow.getWidth() - currentArrowMargin, (getHeight() - leftArrow.getHeight())/2.0f, indicatorPaint);
        canvas.drawBitmap(rightArrow, getWidth()/2.0f + sidePadding / 2.0f + currentArrowMargin, (getHeight() - leftArrow.getHeight())/2.0f, indicatorPaint);
        canvas.drawBitmap(leftIcon, sidePadding, (getHeight() - leftIcon.getHeight())/2.0f, dataLinePaint);
        canvas.drawBitmap(rightIcon, getWidth() - rightIcon.getWidth() - sidePadding, (getHeight() - rightIcon.getHeight())/2.0f, dataLinePaint);
        canvas.drawCircle(xButtonSelector, getHeight()/2.0f, buttonSelectorRadius, dataLinePaint);
    }

    private float getAlphaValue() {
        float pos = currentArrowMargin / (float)indicatorMoveTo;
        float finalVal = 0.0f;
        if(hasToSwipe && !isHoldSwiping || !hasToSwipe && !isSwiping){
            finalVal = (float) Math.max(Math.min(7.48164*Math.pow(pos, 3)-15.36474*Math.pow(pos, 2)+7.88309*pos, 1), 0);
        }
        return finalVal;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                checkIfIsSwiping(event);
                isSwiping = true;
            case MotionEvent.ACTION_MOVE:
                setButtonPosition(event);
                break;
            case MotionEvent.ACTION_UP:
                setButtonPosition(event);
                isHoldSwiping = false;
                isSwiping = false;
                switch(getHoverOverSide()) {
                    case NONE:
                        resetButtonPos();
                        break;
                    case LEFT:
                        isHoldSwiping = true; // just not to get the arrows again
                        isSwiping = true; // just not to get the arrows again
                        if(mLeftSideSelectedListener != null){
                            mLeftSideSelectedListener.onEvent();
                        }
                        fadeOut();
                        break;
                    case RIGHT:
                        isHoldSwiping = true; // just not to get the arrows again
                        isSwiping = true; // just not to get the arrows again
                        if(mRightSideSelectedListener != null){
                            mRightSideSelectedListener.onEvent();
                        }
                        fadeOut();
                        break;
                }
                break;
        }
        invalidate();
        return true;
    }

    private void fadeOut() {
        ValueAnimator opacityAnim = ValueAnimator.ofFloat(1, 0);
        opacityAnim.setDuration(300);
        opacityAnim.setInterpolator(new LinearInterpolator());
        opacityAnim.addUpdateListener((valueAnimator) -> {
            setAlpha((float)valueAnimator.getAnimatedValue());
            if((float)valueAnimator.getAnimatedValue() == 0.0f){
                if(mFadedAwayListener != null){
                    mFadedAwayListener.onEvent();
                }
            }
        });
        opacityAnim.start();
    }

    private void checkIfIsSwiping(MotionEvent event) {
        boolean isOnX = event.getX() >= xButtonSelector - buttonSelectorRadius && event.getX() <= xButtonSelector + buttonSelectorRadius;
        float ySpace = (getHeight() - 2*buttonSelectorRadius)/2.0f;
        boolean isOnY = event.getY() >= ySpace && event.getY() <= getHeight() - ySpace;
        isHoldSwiping = isOnX && isOnY;
    }

    private void resetButtonPos() {
        xButtonSelector = getWidth() / 2.0f;
        valAnim.cancel();
        valAnim.start();
    }

    private void setButtonPosition(MotionEvent event) {
        if(!hasToSwipe || isHoldSwiping){
            xButtonSelector = Math.min(Math.max(event.getX(), sidePadding + leftIcon.getWidth()/2.0f), getWidth()-sidePadding-rightIcon.getWidth()/2.0f);
        }
    }

    private HoveringOverSide getHoverOverSide() {
        if(xButtonSelector <= sidePadding + leftIcon.getWidth()){
            return HoveringOverSide.LEFT;
        }
        else if(xButtonSelector >= getWidth() - sidePadding - rightIcon.getWidth()){
            return HoveringOverSide.RIGHT;
        }
        return HoveringOverSide.NONE;
    }

//    public static Bitmap drawableToBitmap (Drawable drawable, int size) {
//        if (drawable instanceof BitmapDrawable) {
//            return ((BitmapDrawable)drawable).getBitmap();
//        }
//
//        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
//        Canvas canvas = new Canvas(bitmap);
//        drawable.setBounds(0, 0, size, size);
//        drawable.draw(canvas);
//
//        return bitmap;
//    }

    public boolean isHasToSwipe() {
        return hasToSwipe;
    }

    public void setHasToSwipe(boolean hasToSwipe) {
        this.hasToSwipe = hasToSwipe;
    }

    enum HoveringOverSide {
        NONE,
        LEFT,
        RIGHT
    }

    public interface OnLeftSideSelected {
        void onEvent();
    }

    public void setOnLeftSideSelectedListener(OnLeftSideSelected eventListener) {
        mLeftSideSelectedListener = eventListener;
    }

    public interface OnRightSideSelected {
        void onEvent();
    }

    public void setOnRightSideSelectedListener(OnRightSideSelected eventListener) {
        mRightSideSelectedListener = eventListener;
    }

    public interface OnFadedAway {
        void onEvent();
    }

    public void setOnFadedAwayListener(OnFadedAway eventListener) {
        mFadedAwayListener = eventListener;
    }
}