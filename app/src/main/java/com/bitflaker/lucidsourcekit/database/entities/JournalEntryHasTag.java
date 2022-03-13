package com.bitflaker.lucidsourcekit.database.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
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
    @NonNull
    public int entryId;
    @NonNull
    public int tagId;
}


