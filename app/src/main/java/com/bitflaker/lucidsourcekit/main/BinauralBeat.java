package com.bitflaker.lucidsourcekit.main;

import com.bitflaker.lucidsourcekit.charts.FrequencyList;

public class BinauralBeat {
    private final String title, description;
    private final FrequencyList frequencies;
    private final float baseFrequency;

    public BinauralBeat(String title, String description, float baseFrequency, FrequencyList frequencies) {
        this.title = title;
        this.description = description;
        this.baseFrequency = baseFrequency;
        this.frequencies = frequencies;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public FrequencyList getFrequencyList() {
        return frequencies;
    }

    public float getBaseFrequency() {
        return baseFrequency;
    }

    public String getBaseFrequencyString() {
        if(baseFrequency == (int)baseFrequency){
            return String.format("%d Hz", (int)baseFrequency);
        }
        return String.format("%s Hz", baseFrequency);
    }
}
