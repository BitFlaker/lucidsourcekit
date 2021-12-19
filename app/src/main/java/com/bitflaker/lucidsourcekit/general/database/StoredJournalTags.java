package com.bitflaker.lucidsourcekit.general.database;

public class StoredJournalTags {
    public static final String TABLE_NAME = "journal_tags";
    public static final String TAG_ID = "tag_id";
    public static final String DESCRIPTION = "description";

    private final int tagId;
    private final String description;

    public StoredJournalTags(int tagId, String description) {
        this.tagId = tagId;
        this.description = description;
    }

    public int getTagId() {
        return tagId;
    }

    public String getDescription() {
        return description;
    }
}
