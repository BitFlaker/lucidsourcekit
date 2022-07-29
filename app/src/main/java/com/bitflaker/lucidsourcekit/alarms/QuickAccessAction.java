package com.bitflaker.lucidsourcekit.alarms;

import android.graphics.drawable.Drawable;

public class QuickAccessAction {
    private final String title;
    private final String description;
    private final Drawable primaryIcon;
    private final Drawable secondaryIcon;
    private final OnSelected mListener;

    public QuickAccessAction(String title, String description, Drawable primaryIcon, Drawable secondaryIcon, OnSelected onSelectedListener) {
        this.title = title;
        this.description = description;
        this.primaryIcon = primaryIcon;
        this.secondaryIcon = secondaryIcon;
        this.mListener = onSelectedListener;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Drawable getPrimaryIcon() {
        return primaryIcon;
    }

    public Drawable getSecondaryIcon() {
        return secondaryIcon;
    }

    public OnSelected getOnSelectedListener() {
        return mListener;
    }

    public interface OnSelected {
        void onEvent();
    }
}
