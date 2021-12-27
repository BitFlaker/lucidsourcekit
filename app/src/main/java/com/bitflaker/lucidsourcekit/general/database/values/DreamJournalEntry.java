package com.bitflaker.lucidsourcekit.general.database.values;

import com.bitflaker.lucidsourcekit.general.database.StoredJournalEntries;

public class DreamJournalEntry {
    private final StoredJournalEntries entry;
    private final String[] tags;
    private final String[] types;
    private final String[] audioLocations;
    private boolean visibleWithFilter;

    public DreamJournalEntry(StoredJournalEntries entry, String[] tags, String[] types, String[] audioLocations){
        this.entry = entry;
        this.tags = tags;
        this.types = types;
        this.audioLocations = audioLocations;
        visibleWithFilter = true;
    }

    public StoredJournalEntries getEntry() {
        return entry;
    }

    public String[] getTags() {
        return tags;
    }

    public String[] getTypes() {
        return types;
    }

    public String[] getAudioLocations() {
        return audioLocations;
    }

    public void setVisibleWithFilter(boolean visible) {
        this.visibleWithFilter = visible;
    }

    public boolean isVisibleWithFilter() {
        return visibleWithFilter;
    }
}
