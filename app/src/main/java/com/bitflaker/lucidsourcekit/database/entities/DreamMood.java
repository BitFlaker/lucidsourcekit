package com.bitflaker.lucidsourcekit.database.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class DreamMood {
    @NonNull
    @PrimaryKey
    public String moodId;

    public String description;

    public DreamMood(@NonNull String moodId, String description) {
        this.moodId = moodId;
        this.description = description;
    }

    public static DreamMood[] populateData(){
        return new DreamMood[]{
                new DreamMood("TRB", "Terrible"),
                new DreamMood("POR", "Poor"),
                new DreamMood("OKY", "Okay"),
                new DreamMood("GRT", "Great"),
                new DreamMood("OSD", "Outstanding"),
                new DreamMood("", "None")
        };
    }
}
