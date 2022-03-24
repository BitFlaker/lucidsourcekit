package com.bitflaker.lucidsourcekit.database.dreamjournal.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class SleepQuality {
    @NonNull
    @PrimaryKey
    public String qualityId;

    public String description;

    public SleepQuality(String qualityId, String description) {
        this.qualityId = qualityId;
        this.description = description;
    }

    public static SleepQuality[] populateData() {
        return new SleepQuality[]{
                new SleepQuality("TRB", "Terrible"),
                new SleepQuality("POR", "Poor"),
                new SleepQuality("GRT", "Great"),
                new SleepQuality("OSD", "Outstanding"),
                new SleepQuality("", "None")
        };
    }
}
