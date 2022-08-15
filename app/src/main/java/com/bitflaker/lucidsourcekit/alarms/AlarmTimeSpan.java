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
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.add(Calendar.HOUR_OF_DAY, days * 24 + hours);
        cal.add(Calendar.MINUTE, minutes);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
//        cal.add(Calendar.SECOND, 60-cal.get(Calendar.SECOND)-1);
//        cal.add(Calendar.MILLISECOND, 1000-cal.get(Calendar.MILLISECOND));
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
