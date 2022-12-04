package com.bitflaker.lucidsourcekit.database.alarms.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.bitflaker.lucidsourcekit.alarms.AlarmItem;

@Entity
public class AlarmToneTypes {
    @PrimaryKey
    public int alarmToneTypeId;
    public String description;

    public AlarmToneTypes(int alarmToneTypeId, String description) {
        this.alarmToneTypeId = alarmToneTypeId;
        this.description = description;
    }

    public static AlarmToneTypes[] populateData(){
        return new AlarmToneTypes[] {
                new AlarmToneTypes(AlarmItem.AlarmToneType.RINGTONE.ordinal(), "Ringtone"),
                new AlarmToneTypes(AlarmItem.AlarmToneType.CUSTOM_FILE.ordinal(), "Custom file")
        };
    }
}
