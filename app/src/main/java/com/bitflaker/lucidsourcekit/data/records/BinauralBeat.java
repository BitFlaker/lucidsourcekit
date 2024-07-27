package com.bitflaker.lucidsourcekit.data.records;

import com.bitflaker.lucidsourcekit.data.FrequencyList;

public record BinauralBeat(
        String title,
        String description,
        float baseFrequency,
        FrequencyList frequencyList) {
}
