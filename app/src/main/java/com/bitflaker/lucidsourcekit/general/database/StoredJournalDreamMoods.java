package com.bitflaker.lucidsourcekit.general.database;

public class StoredJournalDreamMoods {
    public static final String TABLE_NAME = "dream_moods";
    public static final String MOOD_ID = "mood_id";
    public static final String DESCRIPTION = "description";

    public static final String[] DATA_ROW1 = new String[] { "OSD", "Outstanding" };
    public static final String[] DATA_ROW2 = new String[] { "GRT", "Great" };
    public static final String[] DATA_ROW3 = new String[] { "OKM", "Ok" };
    public static final String[] DATA_ROW4 = new String[] { "POR", "Poor" };
    public static final String[] DATA_ROW5 = new String[] { "TRB", "Terrible" };

    private final String moodId;
    private final String description;

    public StoredJournalDreamMoods(String moodId, String description) {
        this.moodId = moodId;
        this.description = description;
    }

    public String getMoodId() {
        return moodId;
    }

    public String getDescription() {
        return description;
    }
}
