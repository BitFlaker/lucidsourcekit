package com.bitflaker.lucidsourcekit.database.dreamjournal.entities;

import androidx.annotation.Nullable;
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

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof JournalEntryTag tag &&
                tagId == tag.tagId &&
                description.equals(tag.description);
    }
}
