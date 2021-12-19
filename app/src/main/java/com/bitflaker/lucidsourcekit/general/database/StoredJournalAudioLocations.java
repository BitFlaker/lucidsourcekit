package com.bitflaker.lucidsourcekit.general.database;

public class StoredJournalAudioLocations {
    public static final String TABLE_NAME = "journal_audio_location";
    public static final String AUDIO_ID = "audio_id";
    public static final String AUDIO_PATH = "audio_path";
    public static final String ENTRY_ID = "entry_id";

    private final int audioId;
    private final String audioPath;
    private final int entryId;

    public StoredJournalAudioLocations(int audioId, String audioPath, int entryId) {
        this.audioId = audioId;
        this.audioPath = audioPath;
        this.entryId = entryId;
    }

    public int getAudioId() {
        return audioId;
    }

    public String getAudioPath() {
        return audioPath;
    }

    public int getEntryId() {
        return entryId;
    }
}
