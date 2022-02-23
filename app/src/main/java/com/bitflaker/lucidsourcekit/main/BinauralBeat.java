package com.bitflaker.lucidsourcekit.main;

import com.bitflaker.lucidsourcekit.charts.FrequencyList;

public class BinauralBeat {
    private final String title, description, location;
    private final FrequencyList frequencies;

    public BinauralBeat(String title, String description, String location, FrequencyList frequencies) {
        this.title = title;
        this.description = description;
        this.location = location;
        this.frequencies = frequencies;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getLocation() {
        return location;
    }

    public FrequencyList getFrequencyList() {
        return frequencies;
    }
}
