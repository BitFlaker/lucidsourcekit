package com.bitflaker.lucidsourcekit.database.dreamjournal.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Arrays;

@Entity
public class DreamMood {
    @NonNull
    @PrimaryKey
    public String moodId;
    public String description;
    @Ignore
    public int value = 0;

    public DreamMood(@NonNull String moodId, String description) {
        this.moodId = moodId;
        this.description = description;
    }

    @Ignore
    public DreamMood(@NonNull String moodId, String description, int value) {
        this.moodId = moodId;
        this.description = description;
        this.value = value;
    }

    @Ignore
    public static final DreamMood DEFAULT = new DreamMood("", "None", 0);

    @Ignore
    public static DreamMood[] defaultData = new DreamMood[] {
        new DreamMood("TRB", "Terrible", 0),
        new DreamMood("POR", "Poor", 1),
        new DreamMood("OKY", "Okay", 2),
        new DreamMood("GRT", "Great", 3),
        new DreamMood("OSD", "Outstanding", 4),
        DEFAULT
    };

    public static int valueOf(String moodId) {
        return Arrays.stream(defaultData).filter(x -> x.moodId.equals(moodId))
                .findFirst()
                .orElse(DEFAULT)
                .value;
    }

    public static DreamMood ofValue(int value) {
        return Arrays.stream(defaultData).filter(x -> x.value == value && x != DEFAULT)
                .findFirst()
                .orElse(DEFAULT);
    }
}
