package com.bitflaker.lucidsourcekit.database.alarms.updated.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.bitflaker.lucidsourcekit.database.alarms.entities.AlarmToneTypes;

@Entity(foreignKeys = {
            @ForeignKey(entity = AlarmToneTypes.class,
                parentColumns = "alarmToneTypeId",
                childColumns = "alarmToneTypeId",
                onDelete = ForeignKey.CASCADE),
            @ForeignKey(entity = ActiveAlarm.class,
                parentColumns = "requestCode",
                childColumns = "requestCodeActiveAlarm",
                onDelete = ForeignKey.SET_DEFAULT),
        },
        indices = {
            @Index(value = {"alarmToneTypeId"}),
            @Index(value = {"requestCodeActiveAlarm"})
        }
)
public class StoredAlarm {
    @PrimaryKey(autoGenerate = true)
    public long alarmId;
    @NonNull
    @ColumnInfo(defaultValue = "Unnamed Alarm")
    public String title;
    public long bedtimeTimestamp;
    public long alarmTimestamp;
    @NonNull
    public boolean[] pattern;
    public int alarmToneTypeId;
    @NonNull
    public String alarmUri;
    public float alarmVolume;
    public long alarmVolumeIncreaseTimestamp;
    public boolean isVibrationActive;
    public boolean isFlashlightActive;
    public boolean isAlarmActive;
    @ColumnInfo(defaultValue = "-1")
    public int requestCodeActiveAlarm;

    public StoredAlarm(long alarmId, @NonNull String title, long bedtimeTimestamp, long alarmTimestamp, @NonNull boolean[] pattern, int alarmToneTypeId, @NonNull String alarmUri, float alarmVolume, long alarmVolumeIncreaseTimestamp, boolean isVibrationActive, boolean isFlashlightActive, boolean isAlarmActive, int requestCodeActiveAlarm) {
        this.alarmId = alarmId;
        this.title = title;
        this.bedtimeTimestamp = bedtimeTimestamp;
        this.alarmTimestamp = alarmTimestamp;
        this.pattern = pattern;
        this.alarmToneTypeId = alarmToneTypeId;
        this.alarmUri = alarmUri;
        this.alarmVolume = alarmVolume;
        this.alarmVolumeIncreaseTimestamp = alarmVolumeIncreaseTimestamp;
        this.isVibrationActive = isVibrationActive;
        this.isFlashlightActive = isFlashlightActive;
        this.isAlarmActive = isAlarmActive;
        this.requestCodeActiveAlarm = requestCodeActiveAlarm;
    }

    @Ignore
    public StoredAlarm() {

    }

    @Ignore
    public StoredAlarm(@NonNull String title, long bedtimeTimestamp, long alarmTimestamp, @NonNull boolean[] pattern, int alarmToneTypeId, @NonNull String alarmUri, float alarmVolume, long alarmVolumeIncreaseTimestamp, boolean isVibrationActive, boolean isFlashlightActive, boolean isAlarmActive, int requestCodeActiveAlarm) {
        this.title = title;
        this.bedtimeTimestamp = bedtimeTimestamp;
        this.alarmTimestamp = alarmTimestamp;
        this.pattern = pattern;
        this.alarmToneTypeId = alarmToneTypeId;
        this.alarmUri = alarmUri;
        this.alarmVolume = alarmVolume;
        this.alarmVolumeIncreaseTimestamp = alarmVolumeIncreaseTimestamp;
        this.isVibrationActive = isVibrationActive;
        this.isFlashlightActive = isFlashlightActive;
        this.isAlarmActive = isAlarmActive;
        this.requestCodeActiveAlarm = requestCodeActiveAlarm;
    }

    @Ignore
    @NonNull
    @Override
    public String toString() {
        return "Alarm with id " + alarmId + " and title \"" + title + "\"";
    }
}
