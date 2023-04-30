package com.bitflaker.lucidsourcekit.charts;

public class FrequencyData {
    private final float frequency;
    private final float frequencyTo;
    private final float duration;

    public FrequencyData(float frequency, float duration) {
        this.frequency = frequency;
        this.frequencyTo = Float.NaN;
        this.duration = duration;
    }

    public FrequencyData(float frequencyFrom, float frequencyTo, float duration) {
        this.frequency = frequencyFrom;
        this.frequencyTo = frequencyTo;
        this.duration = duration;
    }

    public float getDuration() {
        return duration;
    }

    public float getFrequency() {
        return frequency;
    }

    public float getFrequencyTo() {
        return Float.isNaN(frequencyTo) ? frequency : frequencyTo;
    }

    public float getFrequencyStepSize(int sampleRate) {
        return (getFrequencyTo() - frequency) / (duration * sampleRate);
    }
}
