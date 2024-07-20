package com.bitflaker.lucidsourcekit.database.dreamjournal.entities;

import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Calendar;

@Entity(foreignKeys = {
        @ForeignKey(entity = SleepQuality.class,
                parentColumns = "qualityId",
                childColumns = "qualityId",
                onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = DreamClarity.class,
                parentColumns = "clarityId",
                childColumns = "clarityId",
                onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = DreamMood.class,
                parentColumns = "moodId",
                childColumns = "moodId",
                onDelete = ForeignKey.CASCADE),
        },
        indices = {
                @Index("qualityId"),
                @Index("clarityId"),
                @Index("moodId")
        }
)
public class JournalEntry {
    @PrimaryKey(autoGenerate = true)
    public int entryId;
    public long timeStamp;
    public String title;
    public String description;
    public String qualityId;
    public String clarityId;
    public String moodId;

    public JournalEntry(long timeStamp, String title, String description, String qualityId, String clarityId, String moodId) {
        this.timeStamp = timeStamp;
        this.title = title;
        this.description = description;
        this.qualityId = qualityId;
        this.clarityId = clarityId;
        this.moodId = moodId;
    }

    @Ignore
    public JournalEntry() {
        this.timeStamp = Calendar.getInstance().getTimeInMillis();
        this.title = null;
        this.description = null;
        this.qualityId = SleepQuality.ofValue(0).qualityId;
        this.clarityId = DreamClarity.ofValue(0).clarityId;
        this.moodId = DreamMood.ofValue(0).moodId;
    }

    public void setEntryId(int entryId) {
        this.entryId = entryId;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof JournalEntry entry &&
                entryId == entry.entryId &&
                timeStamp == entry.timeStamp &&
                title.equals(entry.title) &&
                description.equals(entry.description) &&
                qualityId.equals(entry.qualityId) &&
                clarityId.equals(entry.clarityId) &&
                moodId.equals(entry.moodId);
    }
}
