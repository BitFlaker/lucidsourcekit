package com.bitflaker.lucidsourcekit.main;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;

public class GoalAdvice {
    private final String title;
    private final String heading;
    private final String description;
    private final @ColorInt int color;
    private final @DrawableRes int icon;
    private final OnAdviceSelectedListener onAdviceSelectedListener;

    public GoalAdvice(String title, String heading, String description, @DrawableRes int icon, @ColorInt int color, OnAdviceSelectedListener onAdviceSelectedListener) {
        this.title = title;
        this.heading = heading;
        this.description = description;
        this.icon = icon;
        this.color = color;
        this.onAdviceSelectedListener = onAdviceSelectedListener;
    }

    public String getTitle() {
        return title;
    }

    public String getHeading() {
        return heading;
    }

    public String getDescription() {
        return description;
    }

    @DrawableRes
    public int getIcon() {
        return icon;
    }

    public int getColor() {
        return color;
    }

    public OnAdviceSelectedListener getOnAdviceSelectedListener() {
        return onAdviceSelectedListener;
    }

    public interface OnAdviceSelectedListener
    {
        void adviceSelected(GoalAdvice advice);
    }
}
