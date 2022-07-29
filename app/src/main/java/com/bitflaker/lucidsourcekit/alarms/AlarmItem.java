package com.bitflaker.lucidsourcekit.alarms;

import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

public class AlarmItem {
    private int alarmId;
    private String title;

    private int bedtimeHour;
    private int bedtimeMinute;
    private int alarmHour;
    private int alarmMinute;

    private List<Integer> alarmRepeatWeekdays;
    private AlarmToneType alarmToneType;

    private Uri alarmUri;
    private int alarmVolume;
    private int alarmVolumeIncreaseMinutes;
    private int alarmVolumeIncreaseSeconds;
    private boolean vibrate;
    private boolean useFlashlight;
    private boolean isActive;

    public AlarmItem(String title, int bedtimeHour, int bedtimeMinute, int alarmHour, int alarmMinute, List<Integer> alarmRepeatWeekdays, AlarmToneType alarmToneType, Uri alarmUri, int alarmVolume, int alarmVolumeIncreaseMinutes, int alarmVolumeIncreaseSeconds, boolean vibrate, boolean useFlashlight) {
        this.title = title;
        this.bedtimeHour = bedtimeHour;
        this.bedtimeMinute = bedtimeMinute;
        this.alarmHour = alarmHour;
        this.alarmMinute = alarmMinute;
        this.alarmRepeatWeekdays = alarmRepeatWeekdays;
        this.alarmToneType = alarmToneType;
        this.alarmVolume = alarmVolume;
        this.alarmVolumeIncreaseMinutes = alarmVolumeIncreaseMinutes;
        this.alarmVolumeIncreaseSeconds = alarmVolumeIncreaseSeconds;
        this.vibrate = vibrate;
        this.useFlashlight = useFlashlight;
        this.alarmUri = alarmUri;
    }

    public AlarmItem() {
        this.alarmId = -1;
        this.title = "";
        this.bedtimeHour = 0;
        this.bedtimeMinute = 0;
        this.alarmHour = 6;
        this.alarmMinute = 0;
        this.alarmRepeatWeekdays = new ArrayList<>();
        this.alarmToneType = AlarmToneType.RINGTONE;
        this.alarmUri = null;
        this.alarmVolume = 100;
        this.alarmVolumeIncreaseMinutes = 2;
        this.alarmVolumeIncreaseSeconds = 30;
        this.vibrate = true;
        this.useFlashlight = false;
    }

    public int getBedtimeHour() {
        return bedtimeHour;
    }

    public void setBedtimeHour(int bedtimeHour) {
        this.bedtimeHour = bedtimeHour;
    }

    public int getBedtimeMinute() {
        return bedtimeMinute;
    }

    public void setBedtimeMinute(int bedtimeMinute) {
        this.bedtimeMinute = bedtimeMinute;
    }

    public int getAlarmHour() {
        return alarmHour;
    }

    public void setAlarmHour(int alarmHour) {
        this.alarmHour = alarmHour;
    }

    public int getAlarmMinute() {
        return alarmMinute;
    }

    public void setAlarmMinute(int alarmMinute) {
        this.alarmMinute = alarmMinute;
    }

    public List<Integer> getAlarmRepeatWeekdays() {
        return alarmRepeatWeekdays;
    }

    public void setAlarmRepeatWeekdays(List<Integer> alarmRepeatWeekdays) {
        this.alarmRepeatWeekdays = alarmRepeatWeekdays;
    }

    public void addAlarmRepeatWeekdays(Integer alarmRepeatWeekday) {
        this.alarmRepeatWeekdays.add(alarmRepeatWeekday);
    }

    public void removeAlarmRepeatWeekdays(Integer alarmRepeatWeekday) {
        this.alarmRepeatWeekdays.remove(alarmRepeatWeekday);
    }

    public AlarmToneType getAlarmToneType() {
        return alarmToneType;
    }

    public void setAlarmToneType(AlarmToneType alarmToneType) {
        this.alarmToneType = alarmToneType;
    }

    public int getAlarmVolume() {
        return alarmVolume;
    }

    public void setAlarmVolume(int alarmVolume) {
        this.alarmVolume = alarmVolume;
    }

    public int getAlarmVolumeIncreaseMinutes() {
        return alarmVolumeIncreaseMinutes;
    }

    public void setAlarmVolumeIncreaseMinutes(int alarmVolumeIncreaseMinutes) {
        this.alarmVolumeIncreaseMinutes = alarmVolumeIncreaseMinutes;
    }

    public int getAlarmVolumeIncreaseSeconds() {
        return alarmVolumeIncreaseSeconds;
    }

    public void setAlarmVolumeIncreaseSeconds(int alarmVolumeIncreaseSeconds) {
        this.alarmVolumeIncreaseSeconds = alarmVolumeIncreaseSeconds;
    }

    public boolean isVibrate() {
        return vibrate;
    }

    public void setVibrate(boolean vibrate) {
        this.vibrate = vibrate;
    }

    public boolean isUseFlashlight() {
        return useFlashlight;
    }

    public void setUseFlashlight(boolean useFlashlight) {
        this.useFlashlight = useFlashlight;
    }

    public Uri getAlarmUri() {
        return alarmUri;
    }

    public void setAlarmUri(Uri alarmUri) {
        this.alarmUri = alarmUri;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public int getAlarmId() {
        return alarmId;
    }

    public void setAlarmId(int alarmId) {
        this.alarmId = alarmId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public enum AlarmToneType {
        RINGTONE,
        CUSTOM_FILE
    }
}
