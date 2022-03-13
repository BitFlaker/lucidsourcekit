package com.bitflaker.lucidsourcekit.database.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class DreamClarity {
    @NonNull
    @PrimaryKey
    public String clarityId;

    public String description;

    public DreamClarity(@NonNull String clarityId, String description) {
        this.clarityId = clarityId;
        this.description = description;
    }

    public static DreamClarity[] populateData(){
        return new DreamClarity[]{
                new DreamClarity("VCL", "VeryCloudy"),
                new DreamClarity("CLD", "Cloudy"),
                new DreamClarity("CLR", "Clear"),
                new DreamClarity("CCL", "CrystalClear"),
                new DreamClarity("", "None")
        };
    }
}
