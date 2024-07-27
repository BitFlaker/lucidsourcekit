package com.bitflaker.lucidsourcekit.database.dreamjournal.entities.resulttables;

import androidx.annotation.Nullable;
import androidx.room.Embedded;
import androidx.room.Ignore;
import androidx.room.Junction;
import androidx.room.Relation;

import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.AudioLocation;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntry;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntryHasTag;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntryHasType;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntryTag;
import com.bitflaker.lucidsourcekit.data.enums.journalratings.DreamClarity;
import com.bitflaker.lucidsourcekit.data.enums.journalratings.DreamMoods;
import com.bitflaker.lucidsourcekit.data.enums.journalratings.DreamTypes;
import com.bitflaker.lucidsourcekit.data.enums.journalratings.SleepQuality;
import com.bitflaker.lucidsourcekit.data.records.AppliedFilter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class DreamJournalEntry {
    @Embedded
    public JournalEntry journalEntry;

    @Relation(parentColumn = "entryId", entityColumn = "tagId", associateBy = @Junction(JournalEntryHasTag.class))
    public List<JournalEntryTag> journalEntryTags;

    @Relation(parentColumn = "entryId", entityColumn = "entryId")
    public List<AudioLocation> audioLocations;

    @Relation(parentColumn = "entryId", entityColumn = "entryId")
    public List<JournalEntryHasType> journalEntryHasTypes;

    @Ignore
    private final HashMap<String, AudioLocation> addedAudioLocations = new HashMap<>();
    @Ignore
    private final List<String> audioLocationsToDelete = new ArrayList<>();

    // Recording modifications

    public void addAudioLocation(AudioLocation audioLocation) {
        AudioLocation audio = new AudioLocation(audioLocation);
        addedAudioLocations.put(audio.audioPath, audio);
        audioLocations.add(audio);
    }

    public void deleteAudioLocation(String audioPath) {
        audioLocations.stream()
                .filter(x -> x.audioPath.equalsIgnoreCase(audioPath))
                .findFirst()
                .ifPresent(audio -> {
                    addedAudioLocations.remove(audioPath);
                    audioLocations.remove(audio);
                    audioLocationsToDelete.add(audio.audioPath);
                });
    }

    public void removeAllAddedRecordings() {
        addedAudioLocations.values().forEach(audio -> {
            new File(audio.audioPath).delete();
            audioLocations.remove(audio);
        });
        addedAudioLocations.clear();
    }

    public List<AudioLocation> getAudioLocations(int entryId) {
        return audioLocations.stream()
                .peek(e -> e.entryId = entryId)
                .collect(Collectors.toList());
    }

    public void deleteAllRemovedAudioLocations() {
        audioLocationsToDelete.forEach(audioPath -> new File(audioPath).delete());
    }

    public static DreamJournalEntry createDefault() {
        DreamJournalEntry entry = new DreamJournalEntry();
        entry.journalEntry = new JournalEntry();
        entry.journalEntryTags = new ArrayList<>();
        entry.audioLocations = new ArrayList<>();
        entry.journalEntryHasTypes = new ArrayList<>();
        return entry;
    }

    // Value helpers

    public List<String> getStringTags() {
        return journalEntryTags.stream().map(x -> x.description).collect(Collectors.toList());
    }

    public List<JournalEntryHasType> getDreamTypeEntries(int entryId) {
        return journalEntryHasTypes.stream()
                .peek(e -> e.entryId = entryId)
                .collect(Collectors.toList());
    }

    public List<DreamTypes> getDreamTypes() {
        return journalEntryHasTypes.stream()
                .map(e -> DreamTypes.getEnum(e.typeId))
                .collect(Collectors.toList());
    }

    public boolean hasSpecialType(String typeId) {
        return journalEntryHasTypes.stream().anyMatch(x -> x.typeId.equals(typeId));
    }

    public void addDreamType(String dreamType) {
        boolean containsType = journalEntryHasTypes.stream().anyMatch(x -> x.typeId.equals(dreamType));
        if (!containsType) {
            journalEntryHasTypes.add(new JournalEntryHasType(journalEntry.entryId, dreamType));
        }
    }

    public void removeDreamType(String dreamType) {
        journalEntryHasTypes.stream()
                .filter(x -> x.typeId.equals(dreamType))
                .findFirst()
                .ifPresent(type -> journalEntryHasTypes.remove(type));
    }

    // Equality checking

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof DreamJournalEntry entry &&
                journalEntry.equals(entry.journalEntry) &&
                journalEntryTags.equals(entry.journalEntryTags) &&
                audioLocations.equals(entry.audioLocations) &&
                journalEntryHasTypes.equals(entry.journalEntryHasTypes);
    }

    public boolean compliesWithFilter(AppliedFilter filter) {
        return filterDreamTypes(filter.dreamTypes()) &&
                filterTags(filter.filterTagsList()) &&
                filterDreamClarity(filter.dreamClarity()) &&
                filterSleepQuality(filter.sleepQuality()) &&
                filterDreamMood(filter.dreamMood());
    }

    private boolean filterDreamTypes(List<String> dreamTypes) {
        return journalEntryHasTypes.stream().map(x -> x.typeId)
                .collect(Collectors.toCollection(HashSet::new))
                .containsAll(dreamTypes);
    }

    private boolean filterTags(List<String> tags) {
        return journalEntryTags.stream().map(x -> x.description)
                .collect(Collectors.toCollection(HashSet::new))
                .containsAll(tags);
    }

    private boolean filterDreamClarity(DreamClarity dreamClarity) {
        return dreamClarity == DreamClarity.None || DreamClarity.getEnum(journalEntry.clarityId) == dreamClarity;
    }

    private boolean filterSleepQuality(SleepQuality sleepQuality) {
        return sleepQuality == SleepQuality.None || SleepQuality.getEnum(journalEntry.qualityId) == sleepQuality;
    }

    private boolean filterDreamMood(DreamMoods dreamMood){
        return dreamMood == DreamMoods.None || DreamMoods.getEnum(journalEntry.moodId) == dreamMood;
    }

    public enum EntryType {
        PLAIN_TEXT,
        FORMS_TEXT
    }
}
