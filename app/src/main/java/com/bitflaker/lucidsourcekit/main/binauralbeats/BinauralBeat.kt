package com.bitflaker.lucidsourcekit.main.binauralbeats

import com.bitflaker.lucidsourcekit.main.binauralbeats.player.FrequencyList

data class BinauralBeat(
    val title: String,
    val description: String,
    val baseFrequency: Float,
    val frequencyList: FrequencyList
)