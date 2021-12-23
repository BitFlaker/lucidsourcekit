package com.bitflaker.lucidsourcekit.general.database.values;

import com.bitflaker.lucidsourcekit.general.database.StoredJournalEntries;

import java.util.ArrayList;
import java.util.List;

public class DreamJournalEntriesList {
    private List<StoredJournalEntries> entries;
    private List<String[]> tags;
    private List<String[]> types;
    private List<String[]> audioLocations;

    public DreamJournalEntriesList(){
        entries = new ArrayList<>();
        tags = new ArrayList<>();
        types = new ArrayList<>();
        audioLocations = new ArrayList<>();
    }

    public Integer[] getEntryIds(){
        Integer[] entryIds = new Integer[size()];
        for (int i = 0; i < entryIds.length; i++) {
            entryIds[i] = entries.get(i).getEntryId();
        }
        return entryIds;
    }

    public String[] getDates(){
        String[] dates = new String[size()];
        for (int i = 0; i < dates.length; i++) {
            dates[i] = entries.get(i).getDate();
        }
        return dates;
    }

    public String[] getTimes(){
        String[] times = new String[size()];
        for (int i = 0; i < times.length; i++) {
            times[i] = entries.get(i).getTime();
        }
        return times;
    }

    public String[] getTitles(){
        String[] titles = new String[size()];
        for (int i = 0; i < titles.length; i++) {
            titles[i] = entries.get(i).getTitle();
        }
        return titles;
    }

    public String[] getDescriptions(){
        String[] descriptions = new String[size()];
        for (int i = 0; i < descriptions.length; i++) {
            descriptions[i] = entries.get(i).getDescription();
        }
        return descriptions;
    }

    public String[] getDreamMoods(){
        String[] dreamMoods = new String[size()];
        for (int i = 0; i < dreamMoods.length; i++) {
            dreamMoods[i] = entries.get(i).getMood_id();
        }
        return dreamMoods;
    }

    public String[] getDreamClarities(){
        String[] dreamClarities = new String[size()];
        for (int i = 0; i < dreamClarities.length; i++) {
            dreamClarities[i] = entries.get(i).getClarity_id();
        }
        return dreamClarities;
    }

    public String[] getSleepQualities(){
        String[] sleepQualities = new String[size()];
        for (int i = 0; i < sleepQualities.length; i++) {
            sleepQualities[i] = entries.get(i).getQuality_id();
        }
        return sleepQualities;
    }

    public List<String[]> getTags(){
        return tags;
    }

    public List<String[]> getTypes(){
        return types;
    }

    public List<String[]> getAudioLocations(){
        return audioLocations;
    }

    public void add(StoredJournalEntries entry, String[] assignedTags, String[] assignedAudioLocations, String[] assignedTypes){
        entries.add(entry);
        tags.add(assignedTags);
        audioLocations.add(assignedAudioLocations);
        types.add(assignedTypes);
    }

    public void remove(StoredJournalEntries entry){
        int i = entries.indexOf(entry);
        entries.remove(i);
        tags.remove(i);
        audioLocations.remove(i);
        types.remove(i);
    }

    public void remove(int i){
        entries.remove(i);
        tags.remove(i);
        audioLocations.remove(i);
        types.remove(i);
    }

    public int size() {
        return entries.size();
    }
}
