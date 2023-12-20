package com.bitflaker.lucidsourcekit.general.database.values;

import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.resulttables.AssignedTags;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.AudioLocation;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntry;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntryHasType;

import java.util.ArrayList;
import java.util.List;

public class DreamJournalEntry {
    private JournalEntry entry;
    private List<AssignedTags> tags;
    private List<JournalEntryHasType> types;
    private List<AudioLocation> audioLocations;
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

    public void setTags(List<AssignedTags> tags) {
        this.tags = tags;
    }

    public void setAudioLocations(List<AudioLocation> audioLocations) {
        this.audioLocations = audioLocations;
    }

    public List<String> getTagStrings() {
        List<String> tagStrings = new ArrayList<>();
        for (AssignedTags tag : this.tags) {
            tagStrings.add(tag.description);
        }
        return tagStrings;
    }

    public List<JournalEntryHasType> getTypes() {
        return types;
    }

    public List<String> getTypeStrings() {
        List<String> typeStrings = new ArrayList<>();
        for (JournalEntryHasType type : types) {
            typeStrings.add(type.typeId);
        }
        return typeStrings;
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

    public void setTypes(List<JournalEntryHasType> types) {
        this.types = types;
    }

    public void setEntryData(JournalEntry entry) {
        this.entry = entry;
    }
}
