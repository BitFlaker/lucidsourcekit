package com.bitflaker.lucidsourcekit.database.dreamjournal.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Arrays;

@Entity
public class SleepQuality {
    @NonNull
    @PrimaryKey
    public String qualityId;
    public String description;
    @Ignore
    public int value = 0;

    public SleepQuality(@NonNull String qualityId, String description) {
        this.qualityId = qualityId;
        this.description = description;
    }

    @Ignore
    public SleepQuality(@NonNull String qualityId, String description, int value) {
        this.qualityId = qualityId;
        this.description = description;
        this.value = value;
    }

    @Ignore
    public static final SleepQuality DEFAULT = new SleepQuality("", "None", 0);

    @Ignore
    public static SleepQuality[] defaultData = new SleepQuality[] {
        new SleepQuality("TRB", "Terrible", 0),
        new SleepQuality("POR", "Poor", 1),
        new SleepQuality("GRT", "Great", 2),
        new SleepQuality("OSD", "Outstanding", 3),
        DEFAULT
    };

    public static int valueOf(String qualityId) {
        return Arrays.stream(defaultData).filter(x -> x.qualityId.equals(qualityId))
                .findFirst()
                .orElse(DEFAULT)
                .value;
    }

    public static SleepQuality ofValue(int value) {
        return Arrays.stream(defaultData).filter(x -> x.value == value && x != DEFAULT)
                .findFirst()
                .orElse(DEFAULT);
    }
}
