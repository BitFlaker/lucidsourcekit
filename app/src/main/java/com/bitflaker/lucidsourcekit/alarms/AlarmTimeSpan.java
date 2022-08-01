package com.bitflaker.lucidsourcekit.alarms;

import java.util.Calendar;

public class AlarmTimeSpan {
    private final int days;
    private final int hours;
    private final int minutes;
    private final long millisTimeStamp;

    public AlarmTimeSpan(int days, int hours, int minutes) {
        this.days = days;
        this.hours = hours;
        this.minutes = minutes;
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR_OF_DAY, days * 24 + hours);
        cal.add(Calendar.MINUTE, minutes-1);
        cal.add(Calendar.SECOND, 60-cal.get(Calendar.SECOND));
        millisTimeStamp = cal.getTimeInMillis();
    }

    public int getDays() {
        return days;
    }

    public int getHours() {
        return hours;
    }

    public int getMinutes() {
        return minutes;
    }

    public long getMillisTimeStamp() {
        return millisTimeStamp;
    }
}
