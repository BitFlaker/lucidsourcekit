package com.bitflaker.lucidsourcekit.main.binauralbeats;

import com.bitflaker.lucidsourcekit.charts.FrequencyData;
import com.bitflaker.lucidsourcekit.charts.FrequencyList;

public class BinauralBeatStages {
    public static FrequencyList getQuickNapStages() {
        FrequencyList frequencies = new FrequencyList();
        frequencies.add(new FrequencyData(30, 25));
        frequencies.add(new FrequencyData(30, 20, 240));
        frequencies.add(new FrequencyData(20, 2, 800));
        frequencies.add(new FrequencyData(2, 60));
        frequencies.add(new FrequencyData(2, 6, 150));
        frequencies.add(new FrequencyData(6, 540));
        frequencies.add(new FrequencyData(6, 30, 420));
        frequencies.add(new FrequencyData(30, 50));
        return frequencies;
    }

    public static FrequencyList getNapSpikeStages() {
        FrequencyList frequencies = new FrequencyList();
        frequencies.add(new FrequencyData(30, 25));
        frequencies.add(new FrequencyData(30, 20, 240));
        frequencies.add(new FrequencyData(20, 2, 800));
        frequencies.add(new FrequencyData(2, 60));
        frequencies.add(new FrequencyData(2, 5, 150));
        frequencies.add(new FrequencyData(5, 100));
        frequencies.add(new FrequencyData(5, 7.5f, 10));
        frequencies.add(new FrequencyData(7.5f, 5, 10));
        frequencies.add(new FrequencyData(5, 60));
        frequencies.add(new FrequencyData(5, 7.5f, 10));
        frequencies.add(new FrequencyData(7.5f, 5, 10));
        frequencies.add(new FrequencyData(5, 60));
        frequencies.add(new FrequencyData(5, 7.5f, 10));
        frequencies.add(new FrequencyData(7.5f, 5, 10));
        frequencies.add(new FrequencyData(5, 120));
        frequencies.add(new FrequencyData(5, 7.5f, 10));
        frequencies.add(new FrequencyData(7.5f, 5, 10));
        frequencies.add(new FrequencyData(5, 120));
        frequencies.add(new FrequencyData(5, 7.5f, 10));
        frequencies.add(new FrequencyData(7.5f, 5, 10));
        frequencies.add(new FrequencyData(5, 120));
        frequencies.add(new FrequencyData(5, 7.5f, 10));
        frequencies.add(new FrequencyData(7.5f, 5, 10));
        frequencies.add(new FrequencyData(5, 120));
        frequencies.add(new FrequencyData(5, 22, 360));
        frequencies.add(new FrequencyData(22, 30, 170));
        frequencies.add(new FrequencyData(30, 50));
        return frequencies;
    }

    public static FrequencyList getTestStages() {
        FrequencyList frequencies = new FrequencyList();
        frequencies.add(new FrequencyData(30, 2));
        frequencies.add(new FrequencyData(30, 20, 2));
        frequencies.add(new FrequencyData(20, 2, 2));
        frequencies.add(new FrequencyData(2, 2));
        frequencies.add(new FrequencyData(2, 5, 2));
        frequencies.add(new FrequencyData(5, 2));
        frequencies.add(new FrequencyData(5, 9, 2));
        frequencies.add(new FrequencyData(9, 5, 2));
        frequencies.add(new FrequencyData(5, 2));
        frequencies.add(new FrequencyData(5, 22, 2));
        frequencies.add(new FrequencyData(22, 30, 2));
        frequencies.add(new FrequencyData(30, 2));
        return frequencies;
    }
}
