package com.bitflaker.lucidsourcekit.data.records

import com.bitflaker.lucidsourcekit.data.FrequencyList

data class BinauralBeat(
    val title: String,
    val description: String,
    val baseFrequency: Float,
    val frequencyList: FrequencyList
)
