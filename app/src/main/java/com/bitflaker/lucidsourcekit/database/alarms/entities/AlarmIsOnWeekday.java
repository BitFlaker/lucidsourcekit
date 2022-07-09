package com.bitflaker.lucidsourcekit.database.alarms.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

@Entity(primaryKeys = { "alarmId", "weekdayId" },
        foreignKeys = {
                @ForeignKey(entity = Alarm.class,
                        parentColumns = "alarmId",
                        childColumns = "alarmId",
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Weekdays.class,
                        parentColumns = "weekdayId",
                        childColumns = "weekdayId",
                        onDelete = ForeignKey.CASCADE)
        },
        indices = { @Index(value = {"weekdayId"}) })
public class AlarmIsOnWeekday {
    public int alarmId;
    public int weekdayId;

    public AlarmIsOnWeekday(int alarmId, int weekdayId) {
        this.alarmId = alarmId;
        this.weekdayId = weekdayId;
    }
}
