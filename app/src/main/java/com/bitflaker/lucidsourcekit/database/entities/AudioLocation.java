package com.bitflaker.lucidsourcekit.database.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(foreignKeys = {
        @ForeignKey(entity = JournalEntry.class,
                parentColumns = "entryId",
                childColumns = "entryId",
                onDelete = ForeignKey.CASCADE),
        },
        indices = { @Index("entryId") }
)
public class AudioLocation {
    @NonNull
    @PrimaryKey(autoGenerate = true)
    public int audioId;

    public String audioPath;
    public int entryId;
}
