package com.bitflaker.lucidsourcekit.database.alarms.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Calendar;

@Entity
public class Weekdays {
    @PrimaryKey
    public int weekdayId;
    public String description;

    public Weekdays(int weekdayId, String description) {
        this.weekdayId = weekdayId;
        this.description = description;
    }

    public static Weekdays[] populateData(){
        return new Weekdays[] {
                new Weekdays(Calendar.MONDAY, "Monday"),
                new Weekdays(Calendar.TUESDAY, "Tuesday"),
                new Weekdays(Calendar.WEDNESDAY, "Wednesday"),
                new Weekdays(Calendar.THURSDAY, "Thursday"),
                new Weekdays(Calendar.FRIDAY, "Friday"),
                new Weekdays(Calendar.SATURDAY, "Saturday"),
                new Weekdays(Calendar.SUNDAY, "Sunday")
        };
    }
}
