package com.bitflaker.lucidsourcekit.main.binauralbeats;

import com.bitflaker.lucidsourcekit.main.BinauralBeat;

import java.util.ArrayList;
import java.util.List;

public class BinauralBeatsCollection {
    private static BinauralBeatsCollection instance;
    private List<BinauralBeat> binauralBeats;

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
    }

    public List<BinauralBeat> getBinauralBeats() {
        return binauralBeats;
    }
}
