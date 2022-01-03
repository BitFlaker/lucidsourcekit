package com.bitflaker.lucidsourcekit.main;

import com.bitflaker.lucidsourcekit.charts.DataPoint;

import java.util.List;

public class BinauralBeat {
    private final String title, description, location;
    private final List<DataPoint> dataValues;

    public BinauralBeat(String title, String description, String location, List<DataPoint> dataPoints) {
        this.title = title;
        this.description = description;
        this.location = location;
        this.dataValues = dataPoints;
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

    public List<DataPoint> getDataPoints() {
        return dataValues;
    }
}
