package com.bitflaker.lucidsourcekit.general.database;

public class StoredJournalTypes {
    public static final String TABLE_NAME = "journal_types";
    public static final String TYPE_ID = "type_id";
    public static final String DESCRIPTION = "description";

    public static final String[] DATA_ROW1 = new String[] { "NTM", "Nightmare" };
    public static final String[] DATA_ROW2 = new String[] { "SPL", "Sleep Paralysis" };
    public static final String[] DATA_ROW3 = new String[] { "FAW", "False Awakening" };
    public static final String[] DATA_ROW4 = new String[] { "LCD", "Lucid" };

    private final String typeId;
    private final String description;

    public StoredJournalTypes(String typeId, String description) {
        this.typeId = typeId;
        this.description = description;
    }

    public String getTypeId() {
        return typeId;
    }

    public String getDescription() {
        return description;
    }
}
