package com.bitflaker.lucidsourcekit.database.dreamjournal.entities;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(indices = { @Index(value = {"description"}, unique = true) })
public class JournalEntryTag {
    @PrimaryKey(autoGenerate = true)
    public int tagId;
    public String description;

    public JournalEntryTag(String description) {
        this.description = description;
    }
}
