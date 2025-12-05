package com.bitflaker.lucidsourcekit.main.binauralbeats.presets

import com.bitflaker.lucidsourcekit.main.binauralbeats.player.FrequencyData
import com.bitflaker.lucidsourcekit.main.binauralbeats.player.FrequencyList

object BinauralBeatStages {
    val quickNapStages: FrequencyList = FrequencyList().apply {
        add(FrequencyData(30f, 25f))
        add(FrequencyData(30f, 20f, 240f))
        add(FrequencyData(20f, 2f, 800f))
        add(FrequencyData(2f, 60f))
        add(FrequencyData(2f, 6f, 150f))
        add(FrequencyData(6f, 540f))
        add(FrequencyData(6f, 30f, 420f))
        add(FrequencyData(30f, 50f))
    }

    val napSpikeStages: FrequencyList = FrequencyList().apply {
        add(FrequencyData(30f, 25f))
        add(FrequencyData(30f, 20f, 240f))
        add(FrequencyData(20f, 2f, 800f))
        add(FrequencyData(2f, 60f))
        add(FrequencyData(2f, 5f, 150f))
        add(FrequencyData(5f, 100f))
        add(FrequencyData(5f, 7.5f, 10f))
        add(FrequencyData(7.5f, 5f, 10f))
        add(FrequencyData(5f, 60f))
        add(FrequencyData(5f, 7.5f, 10f))
        add(FrequencyData(7.5f, 5f, 10f))
        add(FrequencyData(5f, 60f))
        add(FrequencyData(5f, 7.5f, 10f))
        add(FrequencyData(7.5f, 5f, 10f))
        add(FrequencyData(5f, 120f))
        add(FrequencyData(5f, 7.5f, 10f))
        add(FrequencyData(7.5f, 5f, 10f))
        add(FrequencyData(5f, 120f))
        add(FrequencyData(5f, 7.5f, 10f))
        add(FrequencyData(7.5f, 5f, 10f))
        add(FrequencyData(5f, 120f))
        add(FrequencyData(5f, 7.5f, 10f))
        add(FrequencyData(7.5f, 5f, 10f))
        add(FrequencyData(5f, 120f))
        add(FrequencyData(5f, 22f, 360f))
        add(FrequencyData(22f, 30f, 170f))
        add(FrequencyData(30f, 50f))
    }

    val testStages: FrequencyList = FrequencyList().apply {
        add(FrequencyData(30f, 2f))
        add(FrequencyData(30f, 20f, 2f))
        add(FrequencyData(20f, 2f, 2f))
        add(FrequencyData(2f, 2f))
        add(FrequencyData(2f, 5f, 2f))
        add(FrequencyData(5f, 2f))
        add(FrequencyData(5f, 9f, 2f))
        add(FrequencyData(9f, 5f, 2f))
        add(FrequencyData(5f, 2f))
        add(FrequencyData(5f, 22f, 2f))
        add(FrequencyData(22f, 30f, 2f))
        add(FrequencyData(30f, 2f))
    }
}