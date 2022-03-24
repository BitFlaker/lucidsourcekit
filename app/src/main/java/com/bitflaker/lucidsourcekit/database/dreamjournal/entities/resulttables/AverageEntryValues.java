package com.bitflaker.lucidsourcekit.database.dreamjournal.entities.resulttables;

public class AverageEntryValues {
    private double avgMoods;
    private double avgClarities;
    private double avgQualities;
    private double dreamCount;

    public AverageEntryValues(double avgMoods, double avgClarities, double avgQualities, double dreamCount) {
        this.avgMoods = avgMoods;
        this.avgClarities = avgClarities;
        this.avgQualities = avgQualities;
        this.dreamCount = dreamCount;
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

    public double getDreamCount() {
        return dreamCount;
    }
}
