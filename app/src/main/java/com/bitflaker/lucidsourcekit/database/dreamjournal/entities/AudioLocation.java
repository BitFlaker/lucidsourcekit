package com.bitflaker.lucidsourcekit.database.dreamjournal.entities;

import android.media.MediaPlayer;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.io.IOException;

@Entity(foreignKeys = {
        @ForeignKey(entity = JournalEntry.class,
                parentColumns = "entryId",
                childColumns = "entryId",
                onDelete = ForeignKey.CASCADE),
        },
        indices = { @Index("entryId") }
)
public class AudioLocation {
    @PrimaryKey(autoGenerate = true)
    public int audioId;
    public int entryId;
    public String audioPath;
    @ColumnInfo(defaultValue = "0")
    public long recordingTimestamp;

    @Ignore
    private int recordingLength = -1;

    @Ignore
    public AudioLocation(String audioPath, long recordingTimestamp) {
        this.audioPath = audioPath;
        this.recordingTimestamp = recordingTimestamp;
    }

    public AudioLocation(int entryId, String audioPath, long recordingTimestamp) {
        this.entryId = entryId;
        this.audioPath = audioPath;
        this.recordingTimestamp = recordingTimestamp;
    }

    @Ignore
    public AudioLocation(AudioLocation audioLocation) {
        this.audioId = audioLocation.audioId;
        this.entryId = audioLocation.entryId;
        this.audioPath = audioLocation.audioPath;
        this.recordingTimestamp = audioLocation.recordingTimestamp;
        this.recordingLength = audioLocation.recordingLength;
    }

    public long getRecordingLength() {
        // If the recording length has already been calculated, simply return it
        if (recordingLength != -1) {
            return recordingLength;
        }

        // Otherwise if the recording length has not yet been calculated, calculate it
        try {
            MediaPlayer dataReader = new MediaPlayer();
            dataReader.setDataSource(audioPath);
            dataReader.prepare();
            recordingLength = dataReader.getDuration();
        } catch (IOException e) {
            Log.e("RecordingDB", "Error calculating recording duration: " + e.getMessage());
            recordingLength = -1;
        }
        return recordingLength;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof AudioLocation audioLocation &&
                audioId == audioLocation.audioId &&
                entryId == audioLocation.entryId &&
                audioPath.equals(audioLocation.audioPath) &&
                recordingTimestamp == audioLocation.recordingTimestamp;
    }
}
