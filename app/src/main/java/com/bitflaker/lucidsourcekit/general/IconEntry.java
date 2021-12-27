package com.bitflaker.lucidsourcekit.general;

public class IconEntry {
    private final String text;
    private final int icon;

    public IconEntry(String text, int icon){
        this.text = text;
        this.icon = icon;
    }

    public String getText() {
        return text;
    }

    public int getIcon() {
        return icon;
    }
}
