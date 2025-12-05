package com.bitflaker.lucidsourcekit.database.alarms.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.bitflaker.lucidsourcekit.main.alarms.AlarmToneType;

@Entity
public class AlarmToneTypes {
    @PrimaryKey
    public int alarmToneTypeId;
    public String description;

    public AlarmToneTypes(int alarmToneTypeId, String description) {
        this.alarmToneTypeId = alarmToneTypeId;
        this.description = description;
    }

    public static AlarmToneTypes[] defaultData = new AlarmToneTypes[] {
            new AlarmToneTypes(AlarmToneType.RINGTONE.ordinal(), "Ringtone"),
            new AlarmToneTypes(AlarmToneType.CUSTOM_FILE.ordinal(), "Custom file"),
            new AlarmToneTypes(AlarmToneType.BINAURAL_BEAT.ordinal(), "Binaural Beat")
    };
}
