package com.bitflaker.lucidsourcekit.data.records

import com.bitflaker.lucidsourcekit.data.enums.journalratings.DreamClarity
import com.bitflaker.lucidsourcekit.data.enums.journalratings.DreamMoods
import com.bitflaker.lucidsourcekit.data.enums.journalratings.SleepQuality

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
