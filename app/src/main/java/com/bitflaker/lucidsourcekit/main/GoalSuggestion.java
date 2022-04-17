package com.bitflaker.lucidsourcekit.main;

public class GoalSuggestion {
    private int icon;
    private String text;

    public GoalSuggestion(int icon, String text) {
        this.icon = icon;
        this.text = text;
    }

    public int getIcon() {
        return icon;
    }

    public String getText() {
        return text;
    }
}
