package com.bitflaker.lucidsourcekit.database.dreamjournal.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.bitflaker.lucidsourcekit.main.dreamjournal.RecordingData;

import java.util.ArrayList;
import java.util.List;

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

    public int entryId;
    public String audioPath;

    public AudioLocation(int entryId, String audioPath) {
        this.entryId = entryId;
        this.audioPath = audioPath;
    }

    public static List<AudioLocation> parse(int currentEntryId, List<RecordingData> audioRecordings) {
        List<AudioLocation> audioLocations = new ArrayList<>();
        for (RecordingData recData : audioRecordings){
            audioLocations.add(new AudioLocation(currentEntryId, recData.getFilepath()));
        }
        return audioLocations;
    }
}
