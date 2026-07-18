package com.bitflaker.lucidsourcekit.main.binauralbeats.presets

import com.bitflaker.lucidsourcekit.main.binauralbeats.BinauralBeat

object BinauralBeatStages {
    val quickNapStages: List<BinauralBeat.Segment> = listOf(
        BinauralBeat.Segment(30.0, 30.0, 25.0),
        BinauralBeat.Segment(30.0, 20.0, 240.0),
        BinauralBeat.Segment(20.0, 2.0, 800.0),
        BinauralBeat.Segment(2.0, 2.0, 60.0),
        BinauralBeat.Segment(2.0, 6.0, 150.0),
        BinauralBeat.Segment(6.0, 6.0, 540.0),
        BinauralBeat.Segment(6.0, 30.0, 420.0),
        BinauralBeat.Segment(30.0, 30.0, 50.0)
    )

    val napSpikeStages: List<BinauralBeat.Segment> = listOf(
        BinauralBeat.Segment(30.0, 30.0, 25.0),
        BinauralBeat.Segment(30.0, 20.0, 240.0),
        BinauralBeat.Segment(20.0, 2.0, 800.0),
        BinauralBeat.Segment(2.0, 2.0, 60.0),
        BinauralBeat.Segment(2.0, 5.0, 150.0),
        BinauralBeat.Segment(5.0, 5.0, 100.0),
        BinauralBeat.Segment(5.0, 7.5, 10.0),
        BinauralBeat.Segment(7.5, 5.0, 10.0),
        BinauralBeat.Segment(5.0, 5.0, 60.0),
        BinauralBeat.Segment(5.0, 7.5, 10.0),
        BinauralBeat.Segment(7.5, 5.0, 10.0),
        BinauralBeat.Segment(5.0, 5.0, 60.0),
        BinauralBeat.Segment(5.0, 7.5, 10.0),
        BinauralBeat.Segment(7.5, 5.0, 10.0),
        BinauralBeat.Segment(5.0, 5.0, 120.0),
        BinauralBeat.Segment(5.0, 7.5, 10.0),
        BinauralBeat.Segment(7.5, 5.0, 10.0),
        BinauralBeat.Segment(5.0, 5.0, 120.0),
        BinauralBeat.Segment(5.0, 7.5, 10.0),
        BinauralBeat.Segment(7.5, 5.0, 10.0),
        BinauralBeat.Segment(5.0, 5.0, 120.0),
        BinauralBeat.Segment(5.0, 7.5, 10.0),
        BinauralBeat.Segment(7.5, 5.0, 10.0),
        BinauralBeat.Segment(5.0, 5.0, 120.0),
        BinauralBeat.Segment(5.0, 22.0, 360.0),
        BinauralBeat.Segment(22.0, 30.0, 170.0),
        BinauralBeat.Segment(30.0, 30.0, 50.0),
    )

    val testStages: List<BinauralBeat.Segment> = listOf(
        BinauralBeat.Segment(30.0, 30.0, 2.0),
        BinauralBeat.Segment(30.0, 20.0, 2.0),
        BinauralBeat.Segment(20.0, 2.0, 2.0),
        BinauralBeat.Segment(2.0, 2.0, 2.0),
        BinauralBeat.Segment(2.0, 5.0, 2.0),
        BinauralBeat.Segment(5.0, 5.0, 2.0),
        BinauralBeat.Segment(5.0, 9.0, 2.0),
        BinauralBeat.Segment(9.0, 5.0, 2.0),
        BinauralBeat.Segment(5.0, 5.0, 2.0),
        BinauralBeat.Segment(5.0, 22.0, 2.0),
        BinauralBeat.Segment(22.0, 30.0, 2.0),
        BinauralBeat.Segment(30.0, 30.0, 2.0)
    )
}