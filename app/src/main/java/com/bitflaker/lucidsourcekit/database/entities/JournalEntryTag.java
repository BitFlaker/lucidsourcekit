package com.bitflaker.lucidsourcekit.database.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class JournalEntryTag {
    @NonNull
    @PrimaryKey(autoGenerate = true)
    public int tagId;
    public String description;
}
