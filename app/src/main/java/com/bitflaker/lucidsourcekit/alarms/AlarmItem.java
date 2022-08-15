package com.bitflaker.lucidsourcekit.alarms;

import android.net.Uri;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Calendar;
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

    public AlarmItem(int alarmId, boolean isActive, String title, int bedtimeHour, int bedtimeMinute, int alarmHour, int alarmMinute, List<Integer> alarmRepeatWeekdays, AlarmToneType alarmToneType, Uri alarmUri, int alarmVolume, int alarmVolumeIncreaseMinutes, int alarmVolumeIncreaseSeconds, boolean vibrate, boolean useFlashlight) {
        this.alarmId = alarmId;
        this.isActive = isActive;
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

    public List<Integer> getActiveDaysSorted() {
        alarmRepeatWeekdays.sort((ad1, ad2) -> {
//            if(ad1 == Calendar.SUNDAY){
//                return 1;
//            }
//            else if (ad2 == Calendar.SUNDAY){
//                return -1;
//            } else
            if(ad1 < ad2) {
                return -1;
            }
            else if(ad1 > ad2) {
                return 1;
            }
            return 0;
        });
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

    public List<AlarmTimeSpan> getTimesTo() {
        List<AlarmTimeSpan> timesTo = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        int currDay = cal.get(Calendar.DAY_OF_WEEK);
        int currentHour = cal.get(Calendar.HOUR_OF_DAY);
        int currentMinute = cal.get(Calendar.MINUTE);
        if(alarmRepeatWeekdays.size() > 0) {
            List<Integer> lis = getActiveDaysSorted();
            for (int day : lis) {
                if(day == currDay && (currentHour < alarmHour || (currentHour == alarmHour && currentMinute < alarmMinute))) {
                    int hoursLeft = alarmHour - currentHour;
                    int minutesLeft = alarmMinute - currentMinute;
                    timesTo.add(new AlarmTimeSpan(0, hoursLeft, minutesLeft));
                }
                else {
                    timesTo.add(calcDayDifference(currDay, currentHour, currentMinute, day));
                }
            }
            timesTo.sort((tt1, tt2) -> {
                if(tt1.getMillisTimeStamp() < tt2.getMillisTimeStamp()) { return -1; }
                else if(tt1.getMillisTimeStamp() > tt2.getMillisTimeStamp()) { return 1; }
                return 0;
            });
        }
        else {
            if(currentHour < alarmHour || (currentHour == alarmHour && currentMinute < alarmMinute)) {
                int hoursLeft = alarmHour - currentHour;
                int minutesLeft = alarmMinute - currentMinute;
                timesTo.add(new AlarmTimeSpan(0, hoursLeft, minutesLeft));
            }
            else {
                timesTo.add(getAlarmTimeSpan(currentHour, currentMinute, 0));
            }
        }
        return timesTo;
    }

    private AlarmTimeSpan calcDayDifference(int currDay, int currentHour, int currentMinute, int day) {
        int daysLeft;
        if(day == currDay) { daysLeft = 6; }
        else if (day > currDay) { daysLeft = day - currDay - 1; }
        else { daysLeft = 7 - (Calendar.MONDAY - Calendar.SUNDAY) - 1; }
        return getAlarmTimeSpan(currentHour, currentMinute, daysLeft);
    }

    @NonNull
    private AlarmTimeSpan getAlarmTimeSpan(int currentHour, int currentMinute, int daysLeft) {
        int hoursLeft = 24 - currentHour;
        int minutesLeft = (60 - currentMinute) % 60;
        if(currentMinute != 0) { hoursLeft--; }
        minutesLeft += alarmMinute;
        if(minutesLeft >= 60){
            minutesLeft = minutesLeft % 60;
            hoursLeft++;
        }
        hoursLeft += alarmHour;
        if(hoursLeft >= 24){
            hoursLeft = hoursLeft % 24;
            daysLeft++;
        }

        return new AlarmTimeSpan(daysLeft, hoursLeft, minutesLeft);
    }

    public AlarmItem copy() {
        return new AlarmItem(alarmId, isActive, title, bedtimeHour, bedtimeMinute, alarmHour, alarmMinute, alarmRepeatWeekdays, alarmToneType, alarmUri, alarmVolume, alarmVolumeIncreaseMinutes, alarmVolumeIncreaseSeconds, vibrate, useFlashlight);
    }

    public int getVolumeIncreaseTotalSeconds() {
        return getAlarmVolumeIncreaseMinutes() * 60 + getAlarmVolumeIncreaseSeconds();
    }

    public boolean isIncreaseVolume() {
        return getVolumeIncreaseTotalSeconds() > 0;
    }

    public enum AlarmToneType {
        RINGTONE,
        CUSTOM_FILE
    }
}
