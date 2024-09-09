package com.bitflaker.lucidsourcekit.data.records;

import com.bitflaker.lucidsourcekit.data.enums.journalratings.DreamClarity;
import com.bitflaker.lucidsourcekit.data.enums.journalratings.DreamMoods;
import com.bitflaker.lucidsourcekit.data.enums.journalratings.SleepQuality;

import java.util.List;

public record AppliedFilter(
        List<String> filterTagsList,
        List<String> dreamTypes,
        DreamMoods dreamMood,
        DreamClarity dreamClarity,
        SleepQuality sleepQuality) {

    public static boolean isEmptyFilter(AppliedFilter filter) {
        return filter.filterTagsList.isEmpty() &&
                filter.dreamTypes.isEmpty() &&
                filter.dreamMood == DreamMoods.None &&
                filter.dreamClarity == DreamClarity.None &&
                filter.sleepQuality == SleepQuality.None;
    }
}
