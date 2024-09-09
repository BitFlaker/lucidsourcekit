package com.bitflaker.lucidsourcekit.data.records;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;

public record GoalAdvice(
        String title,
        String heading,
        String description,
        @DrawableRes int icon,
        @ColorInt int color,
        OnAdviceSelectedListener onAdviceSelectedListener) {

    public interface OnAdviceSelectedListener {
        void adviceSelected(GoalAdvice advice);
    }
}
