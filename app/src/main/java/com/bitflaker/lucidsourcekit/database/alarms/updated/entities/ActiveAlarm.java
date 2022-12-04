package com.bitflaker.lucidsourcekit.database.alarms.updated.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class ActiveAlarm {
    @PrimaryKey
    public int requestCode;
    public long initialTime;
    public int interval;
    public int patternIndex;

    public ActiveAlarm(int requestCode, long initialTime, int interval, int patternIndex) {
        this.requestCode = requestCode;
        this.initialTime = initialTime;
        this.interval = interval;
        this.patternIndex = patternIndex;
    }

//    @Ignore
//    public ActiveAlarm(int requestCode, long initialTime, int interval, int patternIndex, boolean[] pattern, long storedAlarmId) {
//        this.requestCode = requestCode;
//        this.initialTime = initialTime;
//        this.interval = interval;
//        this.patternIndex = patternIndex;
//    }

    public static ActiveAlarm createUnreferencedAlarm() {
        return new ActiveAlarm(-1, -1, -1, -1);
    }

    @NonNull
    @Override
    public String toString() {
        return "RC: " + requestCode + " | IT: " + initialTime + " | IV: " + interval + " | PI: " + patternIndex;// + " | PTRN: " + Arrays.toString(pattern);
    }
}