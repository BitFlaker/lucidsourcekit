package com.bitflaker.lucidsourcekit.main;

import com.bitflaker.lucidsourcekit.general.JournalTypes;
import com.bitflaker.lucidsourcekit.general.database.values.DreamClarity;
import com.bitflaker.lucidsourcekit.general.database.values.DreamMoods;
import com.bitflaker.lucidsourcekit.general.database.values.SleepQuality;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AppliedFilter {
    private final List<String> filterTagsList;
    private final boolean[] filterDreamTypes;
    private final JournalTypes journalType;
    private final DreamMoods dreamMood;
    private final DreamClarity dreamClarity;
    private final SleepQuality sleepQuality;

    public AppliedFilter(List<String> filterTagsList, boolean[] filterDreamTypes, JournalTypes journalType, DreamMoods dreamMood, DreamClarity dreamClarity, SleepQuality sleepQuality) {
        this.filterTagsList = filterTagsList;
        this.filterDreamTypes = filterDreamTypes;
        this.journalType = journalType;
        this.dreamMood = dreamMood;
        this.dreamClarity = dreamClarity;
        this.sleepQuality = sleepQuality;
    }

    public static boolean isEmptyFilter(AppliedFilter filter) {
        return filter.filterTagsList.size() == 0 && Arrays.equals(filter.filterDreamTypes,
                new boolean[]{false, false, false, false, false}) && filter.journalType == JournalTypes.None &&
                filter.dreamMood == DreamMoods.None && filter.dreamClarity == DreamClarity.None && filter.sleepQuality == SleepQuality.None;
    }

    public static AppliedFilter getEmptyFilter() {
        return new AppliedFilter(new ArrayList<>(), new boolean[]{false, false, false, false, false}, JournalTypes.None, DreamMoods.None, DreamClarity.None, SleepQuality.None);
    }

    public List<String> getFilterTagsList() {
        return filterTagsList;
    }

    public boolean[] getFilterDreamTypes() {
        return filterDreamTypes;
    }

    public JournalTypes getJournalType() {
        return journalType;
    }

    public DreamMoods getDreamMood() {
        return dreamMood;
    }

    public DreamClarity getDreamClarity() {
        return dreamClarity;
    }

    public SleepQuality getSleepQuality() {
        return sleepQuality;
    }
}
