package com.bitflaker.lucidsourcekit.database.dreamjournal.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

@Entity(primaryKeys = { "entryId", "typeId" },
        foreignKeys = {
                @ForeignKey(entity = JournalEntry.class,
                        parentColumns = "entryId",
                        childColumns = "entryId",
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = DreamType.class,
                        parentColumns = "typeId",
                        childColumns = "typeId",
                        onDelete = ForeignKey.CASCADE)
        },
        indices = { @Index("entryId"), @Index("typeId") }
)
public class JournalEntryHasType {
    public int entryId;
    @NonNull
    public String typeId;

    public JournalEntryHasType(int entryId, @NonNull String typeId) {
        this.entryId = entryId;
        this.typeId = typeId;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof JournalEntryHasType hasType &&
                entryId == hasType.entryId &&
                typeId.equals(hasType.typeId);
    }
}
