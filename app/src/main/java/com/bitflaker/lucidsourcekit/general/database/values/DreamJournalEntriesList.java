package com.bitflaker.lucidsourcekit.general.database.values;

import com.bitflaker.lucidsourcekit.database.entities.AssignedTags;
import com.bitflaker.lucidsourcekit.database.entities.AudioLocation;
import com.bitflaker.lucidsourcekit.database.entities.JournalEntry;
import com.bitflaker.lucidsourcekit.database.entities.JournalEntryHasType;
import com.bitflaker.lucidsourcekit.general.JournalTypes;
import com.bitflaker.lucidsourcekit.main.AppliedFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

public class DreamJournalEntriesList {
    private List<DreamJournalEntry> entries;

    public DreamJournalEntriesList(){
        entries = new ArrayList<>();
    }

    public DreamJournalEntriesList(List<DreamJournalEntry> entries){
        this.entries = entries;
    }

    public Integer[] getEntryIds() {
        Integer[] entryIds = new Integer[size()];
        for (int i = 0; i < entryIds.length; i++) {
            entryIds[i] = entries.get(i).getEntry().entryId;
        }
        return entryIds;
    }

    public Calendar[] getTimestamps() {
        Calendar[] dates = new Calendar[size()];
        for (int i = 0; i < dates.length; i++) {
            Calendar cldr = new GregorianCalendar(TimeZone.getDefault());
            cldr.setTimeInMillis(entries.get(i).getEntry().timeStamp);
            dates[i] = cldr;
        }
        return dates;
    }

    public String[] getTitles(){
        String[] titles = new String[size()];
        for (int i = 0; i < titles.length; i++) {
            titles[i] = entries.get(i).getEntry().title;
        }
        return titles;
    }

    public String[] getDescriptions(){
        String[] descriptions = new String[size()];
        for (int i = 0; i < descriptions.length; i++) {
            descriptions[i] = entries.get(i).getEntry().description;
        }
        return descriptions;
    }

    public String[] getDreamMoods(){
        String[] dreamMoods = new String[size()];
        for (int i = 0; i < dreamMoods.length; i++) {
            dreamMoods[i] = entries.get(i).getEntry().moodId;
        }
        return dreamMoods;
    }

    public String[] getDreamClarities(){
        String[] dreamClarities = new String[size()];
        for (int i = 0; i < dreamClarities.length; i++) {
            dreamClarities[i] = entries.get(i).getEntry().clarityId;
        }
        return dreamClarities;
    }

    public String[] getSleepQualities(){
        String[] sleepQualities = new String[size()];
        for (int i = 0; i < sleepQualities.length; i++) {
            sleepQualities[i] = entries.get(i).getEntry().qualityId;
        }
        return sleepQualities;
    }

    public List<List<AssignedTags>> getTags() {
        List<List<AssignedTags>> tags = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            tags.add(entries.get(i).getTags());
        }
        return tags;
    }

    public List<List<JournalEntryHasType>> getTypes(){
        List<List<JournalEntryHasType>> types = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            types.add(entries.get(i).getTypes());
        }
        return types;
    }

    public List<List<AudioLocation>> getAudioLocations(){
        List<List<AudioLocation>> audioLocations = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            audioLocations.add(entries.get(i).getAudioLocations());
        }
        return audioLocations;
    }

    public List<Boolean> getVisibleWithFilter() {
        List<Boolean> visibleWithFilter = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            visibleWithFilter.add(entries.get(i).isVisibleWithFilter());
        }
        return visibleWithFilter;
    }

    public void add(JournalEntry entry, List<AssignedTags> assignedTags, List<AudioLocation> assignedAudioLocations, List<JournalEntryHasType> assignedTypes){
        entries.add(new DreamJournalEntry(entry, assignedTags, assignedTypes, assignedAudioLocations));
    }

    public void add(DreamJournalEntry entry){
        entries.add(entry);
    }

    public void addFirst(DreamJournalEntry entryToAdd) {
        entries.add(0, entryToAdd);
    }

    public void changeAt(int position, JournalEntry entry, List<AssignedTags> assignedTags, List<JournalEntryHasType> assignedTypes, List<AudioLocation> assignedAudioLocations) {
        if(entry.entryId == -1) {
            // TODO is this case relevant?
            entry.entryId = entries.get(position).getEntry().entryId;
        }
        entries.set(position, new DreamJournalEntry(entry, assignedTags, assignedTypes, assignedAudioLocations));
    }

    public void change(DreamJournalEntry oldEntry, DreamJournalEntry newEntry){
        entries.set(entries.indexOf(oldEntry), newEntry);
    }

    public void sortByTitle(boolean a_to_z) {
        Collections.sort(entries, (dje1, dje2) -> {
            int order = dje1.getEntry().title.toLowerCase().compareTo(dje2.getEntry().title.toLowerCase());
            return a_to_z ? order : order * -1;
        });
    }

    public void sortByDescription(boolean a_to_z) {
        Collections.sort(entries, (dje1, dje2) -> {
            int order = 0;
            String desc1 = dje1.getEntry().description;
            String desc2 = dje2.getEntry().description;
            if(desc1 != null && desc2 != null){  order = desc1.toLowerCase().compareTo(desc2.toLowerCase()); }
            else if(desc1 == null && desc2 != null){ order = a_to_z ? 1 : -1; }
            else if(desc1 != null){ order = a_to_z ? -1 : 1; }
            return a_to_z ? order : order * -1;
        });
    }

    public void sortByTimestamp(boolean newestFirst) {
        Collections.sort(entries, (dje1, dje2) -> {
            Calendar timestampCalendar1 = new GregorianCalendar(TimeZone.getDefault());
            Calendar timestampCalendar2 = new GregorianCalendar(TimeZone.getDefault());
            try {
                timestampCalendar1.setTimeInMillis(dje1.getEntry().timeStamp);
                timestampCalendar2.setTimeInMillis(dje2.getEntry().timeStamp);
            } catch (Exception e) {
                e.printStackTrace();
            }
            int order = timestampCalendar1.compareTo(timestampCalendar2);
            return newestFirst ? order * -1: order;
        });
    }

    public DreamJournalEntry get(int i) {
        return entries.get(i);
    }

    public void updateFilterVisibility(int i, boolean visible) {
        entries.get(i).setVisibleWithFilter(visible);
    }

    public void remove(DreamJournalEntry entry){
        entries.remove(entry);
    }

    public void removeAt(int i){
        entries.remove(i);
    }

    public int size() {
        return entries.size();
    }

    public DreamJournalEntriesList filter(AppliedFilter filter) {
        DreamJournalEntriesList filteredEntries = new DreamJournalEntriesList();
        for (DreamJournalEntry entry : entries) {
            boolean compliesWithFilters = entryCompliesWithFilter(entry, filter);
            entry.setVisibleWithFilter(compliesWithFilters);
            if(compliesWithFilters){ filteredEntries.add(entry); }
        }
        return filteredEntries;
    }

    public static boolean entryCompliesWithFilter(DreamJournalEntry entry, AppliedFilter filter) {
        return passesMoodFilter(filter, entry) && passesClarityFilter(filter, entry) && passesQualityFilter(filter, entry)
                && passesJournalTypeFilter(filter, entry) && passesTagsFilter(filter, entry) && passesDreamTypeFilter(filter, entry);
    }

    private static boolean passesDreamTypeFilter(AppliedFilter filter, DreamJournalEntry entry) {
        for (int i = 0; i < filter.getFilterDreamTypes().length; i++) {
            if(!Arrays.asList(entry.getTypes()).contains(DreamTypes.values()[i].getId()) && filter.getFilterDreamTypes()[i]){
                return false;
            }
        }
        return true;
    }

    private static boolean passesTagsFilter(AppliedFilter filter, DreamJournalEntry entry) {
        if(filter.getFilterTagsList().size() > 0) {
            for (String mustHaveTag : filter.getFilterTagsList()) {
                if(!Arrays.asList(entry.getTags()).contains(mustHaveTag)){
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean passesJournalTypeFilter(AppliedFilter filter, DreamJournalEntry entry) {
        if(filter.getJournalType() != JournalTypes.None)  {
            boolean isAudio = entry.getEntry().description == null;
            return (filter.getJournalType() == JournalTypes.Audio) == isAudio;
        }
        return true;
    }

    private static boolean passesClarityFilter(AppliedFilter filter, DreamJournalEntry entry) {
        if(filter.getDreamClarity() != DreamClarity.None) {
            return DreamClarity.getEnum(entry.getEntry().clarityId) == filter.getDreamClarity();
        }
        return true;
    }

    private static boolean passesQualityFilter(AppliedFilter filter, DreamJournalEntry entry) {
        if(filter.getSleepQuality() != SleepQuality.None) {
            return SleepQuality.getEnum(entry.getEntry().qualityId) == filter.getSleepQuality();
        }
        return true;
    }

    private static boolean passesMoodFilter(AppliedFilter filter, DreamJournalEntry entry){
        if(filter.getDreamMood() != DreamMoods.None) {
            return DreamMoods.getEnum(entry.getEntry().moodId) == filter.getDreamMood();
        }
        return true;
    }

    public int getLucidDreamsCount() {
        int count = 0;
        for (DreamJournalEntry entry : entries) {
            if(Arrays.asList(entry.getTypes()).contains(DreamTypes.Lucid.getId())) {
                count++;
            }
        }
        return count;
    }

    public int getTotalDreamsCount() {
        return entries.size();
    }
}
