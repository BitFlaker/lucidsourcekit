package com.bitflaker.lucidsourcekit.database.alarms.entities;

import androidx.annotation.NonNull;

import java.util.Arrays;

public class ActiveAlarmDetails {
    public int requestCode;
    public long initialTime;
    public int interval;
    public int patternIndex;
    public boolean[] pattern;
    public long storedAlarmId;

    public ActiveAlarmDetails(int requestCode, long initialTime, int interval, int patternIndex, boolean[] pattern, long storedAlarmId) {
        this.storedAlarmId = storedAlarmId;
        this.requestCode = requestCode;
        this.initialTime = initialTime;
        this.interval = interval;
        this.patternIndex = patternIndex;
        this.pattern = pattern;
    }

    @NonNull
    @Override
    public String toString() {
        return "AID: " + storedAlarmId + " | RC: " + requestCode + " | IT: " + initialTime + " | IV: " + interval + " | PI: " + patternIndex + " | PTRN: " + Arrays.toString(pattern);
    }
}