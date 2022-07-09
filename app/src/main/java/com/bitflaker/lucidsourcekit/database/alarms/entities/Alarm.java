package com.bitflaker.lucidsourcekit.database.alarms.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(foreignKeys = {
        @ForeignKey(entity = AlarmToneTypes.class,
                parentColumns = "alarmToneTypeId",
                childColumns = "alarmToneType",
                onDelete = ForeignKey.CASCADE)
        },
        indices = { @Index(value = {"alarmToneType"}) })
public class Alarm {
    @PrimaryKey(autoGenerate = true)
    public int alarmId;
    public int bedtimeHour;
    public int bedtimeMinute;
    public int alarmHour;
    public int alarmMinute;
    public int alarmToneType;
    @NonNull
    public String alarmUri;
    public int alarmVolume;
    public int alarmVolumeIncreaseMinutes;
    public int alarmVolumeIncreaseSeconds;
    public boolean vibrate;
    public boolean useFlashlight;
    public boolean isActive;

    public Alarm(int alarmId, int bedtimeHour, int bedtimeMinute, int alarmHour, int alarmMinute, int alarmToneType, @NonNull String alarmUri, int alarmVolume, int alarmVolumeIncreaseMinutes, int alarmVolumeIncreaseSeconds, boolean vibrate, boolean useFlashlight, boolean isActive) {
        this.alarmId = alarmId;
        this.bedtimeHour = bedtimeHour;
        this.bedtimeMinute = bedtimeMinute;
        this.alarmHour = alarmHour;
        this.alarmMinute = alarmMinute;
        this.alarmToneType = alarmToneType;
        this.alarmUri = alarmUri;
        this.alarmVolume = alarmVolume;
        this.alarmVolumeIncreaseMinutes = alarmVolumeIncreaseMinutes;
        this.alarmVolumeIncreaseSeconds = alarmVolumeIncreaseSeconds;
        this.vibrate = vibrate;
        this.useFlashlight = useFlashlight;
        this.isActive = isActive;
    }

    @Ignore
    public Alarm(int bedtimeHour, int bedtimeMinute, int alarmHour, int alarmMinute, int alarmToneType, @NonNull String alarmUri, int alarmVolume, int alarmVolumeIncreaseMinutes, int alarmVolumeIncreaseSeconds, boolean vibrate, boolean useFlashlight, boolean isActive) {
        this.bedtimeHour = bedtimeHour;
        this.bedtimeMinute = bedtimeMinute;
        this.alarmHour = alarmHour;
        this.alarmMinute = alarmMinute;
        this.alarmToneType = alarmToneType;
        this.alarmUri = alarmUri;
        this.alarmVolume = alarmVolume;
        this.alarmVolumeIncreaseMinutes = alarmVolumeIncreaseMinutes;
        this.alarmVolumeIncreaseSeconds = alarmVolumeIncreaseSeconds;
        this.vibrate = vibrate;
        this.useFlashlight = useFlashlight;
        this.isActive = isActive;
    }

    @Ignore
    public void setAlarmId(int alarmId) {
        this.alarmId = alarmId;
    }
}
