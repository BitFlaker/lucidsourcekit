package com.bitflaker.lucidsourcekit.general.database;

public class StoredJournalEntries {
    public static final String TABLE_NAME = "journal_entries";
    public static final String ENTRY_ID = "entry_id";
    public static final String DATE = "date";
    public static final String TIME = "time";
    public static final String TITLE = "title";
    public static final String DESCRIPTION = "description";
    public static final String QUALITY_ID = "quality_id";
    public static final String CLARITY_ID = "clarity_id";
    public static final String MOOD_ID = "mood_id";

    private int entryId;
    private final String date;
    private final String time;
    private final String title;
    private final String description;
    private final String quality_id;
    private final String clarity_id;
    private final String mood_id;

    public StoredJournalEntries(int entryId, String date, String time, String title, String description, String quality_id, String clarity_id, String mood_id) {
        this.entryId = entryId;
        this.date = date;
        this.time = time;
        this.title = title;
        this.description = description;
        this.quality_id = quality_id;
        this.clarity_id = clarity_id;
        this.mood_id = mood_id;
    }

    public String getDescription() {
        return description;
    }

    public int getEntryId() {
        return entryId;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public String getTitle() {
        return title;
    }

    public String getQuality_id() {
        return quality_id;
    }

    public String getClarity_id() {
        return clarity_id;
    }

    public String getMood_id() {
        return mood_id;
    }

    public void setEntryId(int entryId) {
        this.entryId = entryId;
    }
}
