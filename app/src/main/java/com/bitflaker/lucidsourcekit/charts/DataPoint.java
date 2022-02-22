package com.bitflaker.lucidsourcekit.charts;

public class DataPoint {
    private float val;
    private boolean isSkipDataPoint;

    private DataPoint(boolean skipDataPoint){
        isSkipDataPoint = true;
    }

    public DataPoint(float val) {
        this.val = val;
        isSkipDataPoint = false;
    }

    public float getValue() {
        return !isSkipDataPoint ? val : Float.NaN;
    }

    public static DataPoint SkipDataPoint() {
        return new DataPoint(true);
    }
}
