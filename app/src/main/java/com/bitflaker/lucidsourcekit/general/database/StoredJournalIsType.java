package com.bitflaker.lucidsourcekit.general.database;

public class StoredJournalIsType {
    public static final String TABLE_NAME = "journal_entry_is_type";
    public static final String ENTRY_ID = "entry_id";
    public static final String TYPE_ID = "type_id";

    private final int entryId;
    private final String typeId;

    public StoredJournalIsType(int entryId, String typeId) {
        this.entryId = entryId;
        this.typeId = typeId;
    }

    public int getEntryId() {
        return entryId;
    }

    public String getTypeId() {
        return typeId;
    }
}
