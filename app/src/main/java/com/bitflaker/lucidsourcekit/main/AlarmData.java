package com.bitflaker.lucidsourcekit.main;

import java.util.Calendar;
import java.util.List;

public class AlarmData {
    private String title;
    private Calendar time;
    private List<ActiveDays> activeDays;
    private boolean isActive;

    public AlarmData(String title, Calendar time, List<ActiveDays> activeDays, boolean isActive) {
        this.title = title;
        this.time = time;
        this.activeDays = activeDays;
        this.isActive = isActive;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Calendar getTime() {
        return time;
    }

    public void setTime(Calendar time) {
        this.time = time;
    }

    public List<ActiveDays> getActiveDays() {
        return activeDays;
    }

    public void setActiveDays(List<ActiveDays> activeDays) {
        this.activeDays = activeDays;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public enum ActiveDays {
        MONDAY,
        TUESDAY,
        WEDNESDAY,
        THURSDAY,
        FRIDAY,
        SATURDAY,
        SUNDAY
    }
}
