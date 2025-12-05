package com.bitflaker.lucidsourcekit.main.dreamjournal

import com.bitflaker.lucidsourcekit.main.dreamjournal.rating.DreamClarity
import com.bitflaker.lucidsourcekit.main.dreamjournal.rating.DreamMoods
import com.bitflaker.lucidsourcekit.main.dreamjournal.rating.SleepQuality

data class AppliedFilter(
    val filterTagsList: MutableList<String>,
    val dreamTypes: MutableList<String>,
    val dreamMood: DreamMoods,
    val dreamClarity: DreamClarity,
    val sleepQuality: SleepQuality
) {
    companion object {
        fun isEmptyFilter(filter: AppliedFilter): Boolean {
            return filter.filterTagsList.isEmpty() &&
                    filter.dreamTypes.isEmpty() &&
                    filter.dreamMood == DreamMoods.None &&
                    filter.dreamClarity == DreamClarity.None &&
                    filter.sleepQuality == SleepQuality.None
        }
    }
}