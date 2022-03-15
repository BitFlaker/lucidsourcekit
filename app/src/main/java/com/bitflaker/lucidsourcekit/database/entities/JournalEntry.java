package com.bitflaker.lucidsourcekit.database.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

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
        indices = { @Index("qualityId"), @Index("clarityId"), @Index("moodId") }
)
public class JournalEntry {
    @NonNull
    @PrimaryKey(autoGenerate = true)
    public int entryId;

    // TODO: migrate timestamp to: System.currentTimeMillis();
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

    public void setEntryId(int entryId) {
        this.entryId = entryId;
    }
}
