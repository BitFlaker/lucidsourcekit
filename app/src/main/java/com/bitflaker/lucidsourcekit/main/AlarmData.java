package com.bitflaker.lucidsourcekit.main;

import java.util.Calendar;
import java.util.List;

public class AlarmData {
    private int alarmId;
    private String title;
    private Calendar time;
    private List<ActiveDays> activeDays;
    private boolean isActive;
    private boolean isSelected;

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

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public int getAlarmId() {
        return alarmId;
    }

    public void setAlarmId(int alarmId) {
        this.alarmId = alarmId;
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
