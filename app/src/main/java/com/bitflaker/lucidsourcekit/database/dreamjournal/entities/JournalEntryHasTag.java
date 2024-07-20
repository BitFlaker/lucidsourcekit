package com.bitflaker.lucidsourcekit.database.dreamjournal.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;

@Entity(primaryKeys = { "entryId", "tagId" },
        foreignKeys = {
                @ForeignKey(entity = JournalEntry.class,
                        parentColumns = "entryId",
                        childColumns = "entryId",
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = JournalEntryTag.class,
                        parentColumns = "tagId",
                        childColumns = "tagId",
                        onDelete = ForeignKey.CASCADE)
        },
        indices = { @Index("entryId"), @Index("tagId") }
)
public class JournalEntryHasTag {
    public int entryId;
    public int tagId;
    @Ignore public String description;

    public JournalEntryHasTag(int entryId, int tagId) {
        this.entryId = entryId;
        this.tagId = tagId;
    }
}

