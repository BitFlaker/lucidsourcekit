package com.bitflaker.lucidsourcekit.main;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;

public class GoalAdvice {
    private final String title;
    private final String heading;
    private final String description;
    private final @ColorInt int color;
    private final @DrawableRes int icon;

    public GoalAdvice(String title, String heading, String description, @DrawableRes int icon, @ColorInt int color) {
        this.title = title;
        this.heading = heading;
        this.description = description;
        this.icon = icon;
        this.color = color;
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
}
