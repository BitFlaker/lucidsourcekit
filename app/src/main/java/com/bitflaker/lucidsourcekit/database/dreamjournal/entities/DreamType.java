package com.bitflaker.lucidsourcekit.database.dreamjournal.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class DreamType {
    @NonNull
    @PrimaryKey
    public String typeId;

    public String description;

    public DreamType(@NonNull String typeId, String description) {
        this.typeId = typeId;
        this.description = description;
    }

    public static DreamType[] populateData() {
        return new DreamType[] {
                new DreamType("NTM", "Nightmare"),
                new DreamType("SPL", "SleepParalysis"),
                new DreamType("FAW", "FalseAwakening"),
                new DreamType("LCD", "Lucid"),
                new DreamType("REC", "Recurring"),
                new DreamType("", "None")
        };
    }
}
