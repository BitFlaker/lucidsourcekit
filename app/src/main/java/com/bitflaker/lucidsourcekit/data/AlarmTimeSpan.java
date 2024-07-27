package com.bitflaker.lucidsourcekit.data;

import java.util.Calendar;

public class AlarmTimeSpan {
    private final long millisTimeStamp;

    public AlarmTimeSpan(int days, int hours, int minutes) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.add(Calendar.HOUR_OF_DAY, days * 24 + hours);
        cal.add(Calendar.MINUTE, minutes);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        millisTimeStamp = cal.getTimeInMillis();
    }

    public long getMillisTimeStamp() {
        return millisTimeStamp;
    }
}
