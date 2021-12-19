package com.bitflaker.lucidsourcekit.general.database;

public class StoredJournalDreamClarities {
    public static final String TABLE_NAME = "dream_clarities";
    public static final String CLARITY_ID = "clarity_id";
    public static final String DESCRIPTION = "description";

    public static final String[] DATA_ROW1 = new String[] { "CCL", "CrystalClear" };
    public static final String[] DATA_ROW2 = new String[] { "CLR", "Clear" };
    public static final String[] DATA_ROW3 = new String[] { "CLD", "Cloudy" };
    public static final String[] DATA_ROW4 = new String[] { "VCL", "VeryCloudy" };

    private final String clarityId;
    private final String description;

    public StoredJournalDreamClarities(String clarityId, String description) {
        this.clarityId = clarityId;
        this.description = description;
    }

    public String getClarityId() {
        return clarityId;
    }

    public String getDescription() {
        return description;
    }
}
