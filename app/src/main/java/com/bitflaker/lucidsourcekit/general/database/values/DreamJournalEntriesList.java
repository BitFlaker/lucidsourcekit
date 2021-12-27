package com.bitflaker.lucidsourcekit.general.database.values;

import com.bitflaker.lucidsourcekit.general.JournalTypes;
import com.bitflaker.lucidsourcekit.general.database.StoredJournalEntries;
import com.bitflaker.lucidsourcekit.main.AppliedFilter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class DreamJournalEntriesList {
    private List<DreamJournalEntry> entries;

    public DreamJournalEntriesList(){
        entries = new ArrayList<>();
    }

    public Integer[] getEntryIds() {
        Integer[] entryIds = new Integer[size()];
        for (int i = 0; i < entryIds.length; i++) {
            entryIds[i] = entries.get(i).getEntry().getEntryId();
        }
        return entryIds;
    }

    public String[] getDates(){
        String[] dates = new String[size()];
        for (int i = 0; i < dates.length; i++) {
            dates[i] = entries.get(i).getEntry().getDate();
        }
        return dates;
    }

    public String[] getTimes(){
        String[] times = new String[size()];
        for (int i = 0; i < times.length; i++) {
            times[i] = entries.get(i).getEntry().getTime();
        }
        return times;
    }

    public String[] getTitles(){
        String[] titles = new String[size()];
        for (int i = 0; i < titles.length; i++) {
            titles[i] = entries.get(i).getEntry().getTitle();
        }
        return titles;
    }

    public String[] getDescriptions(){
        String[] descriptions = new String[size()];
        for (int i = 0; i < descriptions.length; i++) {
            descriptions[i] = entries.get(i).getEntry().getDescription();
        }
        return descriptions;
    }

    public String[] getDreamMoods(){
        String[] dreamMoods = new String[size()];
        for (int i = 0; i < dreamMoods.length; i++) {
            dreamMoods[i] = entries.get(i).getEntry().getMood_id();
        }
        return dreamMoods;
    }

    public String[] getDreamClarities(){
        String[] dreamClarities = new String[size()];
        for (int i = 0; i < dreamClarities.length; i++) {
            dreamClarities[i] = entries.get(i).getEntry().getClarity_id();
        }
        return dreamClarities;
    }

    public String[] getSleepQualities(){
        String[] sleepQualities = new String[size()];
        for (int i = 0; i < sleepQualities.length; i++) {
            sleepQualities[i] = entries.get(i).getEntry().getQuality_id();
        }
        return sleepQualities;
    }

    public List<String[]> getTags(){
        List<String[]> tags = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            tags.add(entries.get(i).getTags());
        }
        return tags;
    }

    public List<String[]> getTypes(){
        List<String[]> types = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            types.add(entries.get(i).getTypes());
        }
        return types;
    }

    public List<String[]> getAudioLocations(){
        List<String[]> audioLocations = new ArrayList<>();
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

    public void add(StoredJournalEntries entry, String[] assignedTags, String[] assignedAudioLocations, String[] assignedTypes){
        entries.add(new DreamJournalEntry(entry, assignedTags, assignedTypes, assignedAudioLocations));
    }

    public void add(DreamJournalEntry entry){
        entries.add(entry);
    }

    public void addFirst(StoredJournalEntries entry, String[] assignedTags, String[] assignedTypes, String[] assignedAudioLocations) {
        entries.add(0, new DreamJournalEntry(entry, assignedTags, assignedTypes, assignedAudioLocations));
    }

    public void addFirst(DreamJournalEntry entryToAdd) {
        entries.add(0, entryToAdd);
    }

    public void changeAt(int position, StoredJournalEntries entry, String[] assignedTags, String[] assignedTypes, String[] assignedAudioLocations) {
        if(entry.getEntryId() == -1) {
            entry.setEntryId(entries.get(position).getEntry().getEntryId());
        }
        entries.set(position, new DreamJournalEntry(entry, assignedTags, assignedTypes, assignedAudioLocations));
    }

    public void change(DreamJournalEntry oldEntry, DreamJournalEntry newEntry){
        entries.set(entries.indexOf(oldEntry), newEntry);
    }

    public void sortByTitle(boolean a_to_z) {
        Collections.sort(entries, (dje1, dje2) -> {
            int order = dje1.getEntry().getTitle().toLowerCase().compareTo(dje2.getEntry().getTitle().toLowerCase());
            return a_to_z ? order : order * -1;
        });
    }

    public void sortByDescription(boolean a_to_z) {
        Collections.sort(entries, (dje1, dje2) -> {
            int order = 0;
            String desc1 = dje1.getEntry().getDescription();
            String desc2 = dje2.getEntry().getDescription();
            if(desc1 != null && desc2 != null){  order = desc1.toLowerCase().compareTo(desc2.toLowerCase()); }
            else if(desc1 == null && desc2 != null){ order = a_to_z ? 1 : -1; }
            else if(desc1 != null){ order = a_to_z ? -1 : 1; }
            return a_to_z ? order : order * -1;
        });
    }

    public void sortByTimestamp(boolean newestFirst) {
        Collections.sort(entries, (dje1, dje2) -> {
            Date date1 = null;
            Date date2 = null;
            SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm");
            try {
                date1 = format.parse(dje1.getEntry().getDate() + " " + dje1.getEntry().getTime());
                date2 = format.parse(dje2.getEntry().getDate() + " " + dje2.getEntry().getTime());
            } catch (Exception e) {
                e.printStackTrace();
            }
            int order = date1.compareTo(date2);
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
            boolean isAudio = entry.getEntry().getDescription() == null;
            return (filter.getJournalType() == JournalTypes.Audio) == isAudio;
        }
        return true;
    }

    private static boolean passesClarityFilter(AppliedFilter filter, DreamJournalEntry entry) {
        if(filter.getDreamClarity() != DreamClarity.None) {
            return DreamClarity.getEnum(entry.getEntry().getClarity_id()) == filter.getDreamClarity();
        }
        return true;
    }

    private static boolean passesQualityFilter(AppliedFilter filter, DreamJournalEntry entry) {
        if(filter.getSleepQuality() != SleepQuality.None) {
            return SleepQuality.getEnum(entry.getEntry().getQuality_id()) == filter.getSleepQuality();
        }
        return true;
    }

    private static boolean passesMoodFilter(AppliedFilter filter, DreamJournalEntry entry){
        if(filter.getDreamMood() != DreamMoods.None) {
            return DreamMoods.getEnum(entry.getEntry().getMood_id()) == filter.getDreamMood();
        }
        return true;
    }
}
