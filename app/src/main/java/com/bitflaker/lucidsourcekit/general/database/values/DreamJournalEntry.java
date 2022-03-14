package com.bitflaker.lucidsourcekit.general.database.values;

import com.bitflaker.lucidsourcekit.database.entities.AssignedTags;
import com.bitflaker.lucidsourcekit.database.entities.AudioLocation;
import com.bitflaker.lucidsourcekit.database.entities.JournalEntry;
import com.bitflaker.lucidsourcekit.database.entities.JournalEntryHasType;

import java.util.List;

public class DreamJournalEntry {
    private final JournalEntry entry;
    private final List<AssignedTags> tags;
    private final List<JournalEntryHasType> types;
    private final List<AudioLocation> audioLocations;
    private boolean visibleWithFilter;

    public DreamJournalEntry(JournalEntry entry, List<AssignedTags> tags, List<JournalEntryHasType> types, List<AudioLocation> audioLocations){
        this.entry = entry;
        this.tags = tags;
        this.types = types;
        this.audioLocations = audioLocations;
        visibleWithFilter = true;
    }

    public JournalEntry getEntry() {
        return entry;
    }

    public List<AssignedTags> getTags() {
        return tags;
    }

    public List<JournalEntryHasType> getTypes() {
        return types;
    }

    public List<AudioLocation> getAudioLocations() {
        return audioLocations;
    }

    public void setVisibleWithFilter(boolean visible) {
        this.visibleWithFilter = visible;
    }

    public boolean isVisibleWithFilter() {
        return visibleWithFilter;
    }
}
