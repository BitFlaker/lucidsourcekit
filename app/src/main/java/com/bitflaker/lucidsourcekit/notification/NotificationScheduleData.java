package com.bitflaker.lucidsourcekit.notification;

import com.bitflaker.lucidsourcekit.general.Tools;

import java.util.Calendar;

public class NotificationScheduleData {
    private String id;
    private long timestamp;
    private boolean isNextDay;

    public NotificationScheduleData(long timestamp, String id) {
        this.id = id;
        this.timestamp = timestamp;
        this.isNextDay = false;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isNextDay() {
        return isNextDay;
    }

    public void setNextDay(boolean nextDay) {
        isNextDay = nextDay;
    }

    public long getScheduleTime() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(Tools.getMidnightTime() + getTimestamp());
        if(isNextDay) {
            cal.add(Calendar.HOUR_OF_DAY, 24);
        }
        return cal.getTimeInMillis();
    }
}
