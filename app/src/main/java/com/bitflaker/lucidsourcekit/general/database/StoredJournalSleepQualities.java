package com.bitflaker.lucidsourcekit.general.database;

public class StoredJournalSleepQualities {
    public static final String TABLE_NAME = "sleep_qualities";
    public static final String QUALITY_ID = "quality_id";
    public static final String DESCRIPTION = "description";

    public static final String[] DATA_ROW1 = new String[] { "OSD", "Outstanding" };
    public static final String[] DATA_ROW2 = new String[] { "GRT", "Great" };
    public static final String[] DATA_ROW3 = new String[] { "POR", "Poor" };
    public static final String[] DATA_ROW4 = new String[] { "TRB", "Terrible" };

    private final String qualityId;
    private final String description;

    public StoredJournalSleepQualities(String qualityId, String description) {
        this.qualityId = qualityId;
        this.description = description;
    }

    public String getQualityId() {
        return qualityId;
    }

    public String getDescription() {
        return description;
    }
}
