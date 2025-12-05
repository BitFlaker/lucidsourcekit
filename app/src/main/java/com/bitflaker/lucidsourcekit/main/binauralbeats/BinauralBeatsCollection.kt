package com.bitflaker.lucidsourcekit.main.binauralbeats

object BinauralBeatsCollection {
    val binauralBeats = arrayListOf(
        BinauralBeat(
            "Quick Nap Lucidity",
            "Great for supporting the induction of lucid dreams during a quick nap",
            425f,
            BinauralBeatStages.quickNapStages
        ),
        BinauralBeat(
            "Nap Spike Lucidity",
            "Great for supporting the induction of lucid dreams during a longer nap with spikes in theta stage to slightly raise awareness",
            425f,
            BinauralBeatStages.napSpikeStages
        ),
        BinauralBeat(
            "Test",
            "This one is just for testing purpose and can be ignored for normal use",
            425f,
            BinauralBeatStages.testStages
        )
    )
}
