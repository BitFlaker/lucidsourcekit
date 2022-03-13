package com.bitflaker.lucidsourcekit.database.entities;

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

    public static DreamType[] populateData(){
        return new DreamType[]{
                new DreamType("TRB", "Terrible"),
                new DreamType("POR", "Poor"),
                new DreamType("GRT", "Great"),
                new DreamType("OSD", "Outstanding"),
                new DreamType("", "None")
        };
    }
}
