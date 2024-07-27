package com.bitflaker.lucidsourcekit.main.binauralbeats;

import com.bitflaker.lucidsourcekit.data.BinauralBeatStages;
import com.bitflaker.lucidsourcekit.data.records.BinauralBeat;

import java.util.ArrayList;
import java.util.List;

public class BinauralBeatsCollection {
    private static BinauralBeatsCollection instance;
    private final List<BinauralBeat> binauralBeats;

    private BinauralBeatsCollection() {
        binauralBeats = new ArrayList<>();
        fillList();
    }

    public static BinauralBeatsCollection getInstance(){
        if (instance == null) {
            instance = new BinauralBeatsCollection();
        }
        return instance;
    }

    private void fillList() {
        binauralBeats.add(new BinauralBeat("Quick Nap Lucidity", "Great for supporting the induction of lucid dreams during a quick nap", 425, BinauralBeatStages.getQuickNapStages()));
        binauralBeats.add(new BinauralBeat("Nap Spike Lucidity", "Great for supporting the induction of lucid dreams during a longer nap with spikes in theta stage to slightly raise awareness", 425, BinauralBeatStages.getNapSpikeStages()));
        binauralBeats.add(new BinauralBeat("Test", "This one is just for testing purpose and can be ignored for normal use", 425, BinauralBeatStages.getTestStages()));
    }

    public List<BinauralBeat> getBinauralBeats() {
        return binauralBeats;
    }
}
