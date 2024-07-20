package com.bitflaker.lucidsourcekit.main;

import com.bitflaker.lucidsourcekit.general.database.values.DreamClarity;
import com.bitflaker.lucidsourcekit.general.database.values.DreamMoods;
import com.bitflaker.lucidsourcekit.general.database.values.SleepQuality;

import java.util.ArrayList;
import java.util.List;

public record AppliedFilter(
        List<String> filterTagsList,
        List<String> dreamTypes,
        DreamMoods dreamMood,
        DreamClarity dreamClarity,
        SleepQuality sleepQuality) {

    public static final AppliedFilter DEFAULT = new AppliedFilter(
            new ArrayList<>(),
            new ArrayList<>(),
            DreamMoods.None,
            DreamClarity.None,
            SleepQuality.None
    );

    public static boolean isEmptyFilter(AppliedFilter filter) {
        return filter.filterTagsList.isEmpty() &&
                filter.dreamTypes.isEmpty() &&
                filter.dreamMood == DreamMoods.None &&
                filter.dreamClarity == DreamClarity.None &&
                filter.sleepQuality == SleepQuality.None;
    }
}
