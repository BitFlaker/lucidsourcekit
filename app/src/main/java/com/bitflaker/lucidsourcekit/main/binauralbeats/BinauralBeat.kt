package com.bitflaker.lucidsourcekit.main.binauralbeats

data class BinauralBeat(
    val title: String,
    val description: String,
    val baseFrequency: Float,
    val frequencyList: FrequencyList
)