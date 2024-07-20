package com.bitflaker.lucidsourcekit.database.dreamjournal.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Arrays;

@Entity
public class DreamClarity {
    @NonNull
    @PrimaryKey
    public String clarityId;
    public String description;
    @Ignore
    public int value = 0;

    public DreamClarity(@NonNull String clarityId, String description) {
        this.clarityId = clarityId;
        this.description = description;
    }

    @Ignore
    public DreamClarity(@NonNull String clarityId, String description, int value) {
        this.clarityId = clarityId;
        this.description = description;
        this.value = value;
    }

    @Ignore
    public static final DreamClarity DEFAULT = new DreamClarity("", "None", 0);

    @Ignore
    public static DreamClarity[] defaultData = new DreamClarity[] {
        new DreamClarity("VCL", "VeryCloudy", 0),
        new DreamClarity("CLD", "Cloudy", 1),
        new DreamClarity("CLR", "Clear", 2),
        new DreamClarity("CCL", "CrystalClear", 3),
        DEFAULT
    };

    public static int valueOf(String clarityId) {
        return Arrays.stream(defaultData).filter(x -> x.clarityId.equals(clarityId))
                .findFirst()
                .orElse(DEFAULT)
                .value;
    }

    public static DreamClarity ofValue(int value) {
        return Arrays.stream(defaultData).filter(x -> x.value == value && x != DEFAULT)
                .findFirst()
                .orElse(DEFAULT);
    }
}
