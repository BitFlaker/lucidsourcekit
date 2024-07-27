package com.bitflaker.lucidsourcekit.data;

public class BackgroundNoise {
    private final String name;
    private final int icon;
    private float volume;
    private boolean paused;

    public BackgroundNoise(String name, int icon, float volume) {
        this.name = name;
        this.icon = icon;
        this.volume = volume;
        this.paused = false;
    }

    public String getName() {
        return name;
    }

    public int getIcon() {
        return icon;
    }

    public float getVolume() {
        return volume;
    }

    public void setVolume(float value) {
        volume = value;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }
}
