package com.bitflaker.lucidsourcekit.main.binauralbeats

data class BinauralBeat(
    val title: String,
    val description: String,
    val baseFrequency: Float,
    val segments: List<Segment>
) {
    data class Segment(
        val frequencyFrom: Double,
        val frequencyTo: Double,
        val duration: Double,
    )

    val duration: Double = segments.sumOf { it.duration }
}