package com.bitflaker.lucidsourcekit.database.entities;

public class AverageEntryValues {
    private double avgMoods;
    private double avgClarities;
    private double avgQualities;

    public AverageEntryValues(double avgMoods, double avgClarities, double avgQualities) {
        this.avgMoods = avgMoods;
        this.avgClarities = avgClarities;
        this.avgQualities = avgQualities;
    }

    public double getAvgMoods() {
        return avgMoods;
    }

    public double getAvgClarities() {
        return avgClarities;
    }

    public double getAvgQualities() {
        return avgQualities;
    }
}
