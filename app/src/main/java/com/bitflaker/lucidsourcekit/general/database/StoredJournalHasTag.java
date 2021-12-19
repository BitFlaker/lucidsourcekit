package com.bitflaker.lucidsourcekit.general.database;

public class StoredJournalHasTag {
    public static final String TABLE_NAME = "journal_entry_has_tag";
    public static final String ENTRY_ID = "entry_id";
    public static final String TAG_ID = "tag_id";

    private final int entryId;
    private final int tagId;

    public StoredJournalHasTag(int entryId, int tagId) {
        this.entryId = entryId;
        this.tagId = tagId;
    }

    public int getTagId() {
        return tagId;
    }

    public int getEntryId() {
        return entryId;
    }
}
