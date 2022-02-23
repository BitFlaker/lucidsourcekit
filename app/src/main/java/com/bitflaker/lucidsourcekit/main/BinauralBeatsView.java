package com.bitflaker.lucidsourcekit.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.charts.Brainwaves;
import com.bitflaker.lucidsourcekit.charts.FrequencyData;
import com.bitflaker.lucidsourcekit.charts.FrequencyList;
import com.bitflaker.lucidsourcekit.charts.LineGraph;

import java.util.ArrayList;
import java.util.List;

public class BinauralBeatsView extends Fragment {
    private RecyclerView binauralBeatsSelector;
    private LineGraph progressLineGraph;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_binaural_beats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binauralBeatsSelector = getView().findViewById(R.id.rcv_list_binaural_beats);
        progressLineGraph = getView().findViewById(R.id.lg_binaural_time_progress);

        List<BinauralBeat> beats = new ArrayList<>();
        // TODO auto set values for progress of beats
        FrequencyList freqs = new FrequencyList();
        freqs.add(new FrequencyData(32, 3));
        freqs.add(new FrequencyData(32, 13, 20));
        freqs.add(new FrequencyData(13, 10));
        freqs.add(new FrequencyData(13, 8, 20));
        freqs.add(new FrequencyData(8, 10));
        freqs.add(new FrequencyData(8, 4, 35));
        freqs.add(new FrequencyData(4, 25));
        freqs.add(new FrequencyData(4, 0.5f, 5));
        freqs.add(new FrequencyData(0.5f, 5));
        freqs.add(new FrequencyData(0.5f, 8, 5));
        freqs.add(new FrequencyData(8, 5));
        freqs.add(new FrequencyData(8, 32, 35));
        beats.add(new BinauralBeat("samplesad ", "sample description with some length to it", "NULL", freqs));
        beats.add(new BinauralBeat("sampleasd ", "sample description with some length to it", "NULL", freqs));
        beats.add(new BinauralBeat("sample asdas d", "sample description with some length to it", "NULL", freqs));
        beats.add(new BinauralBeat("sampleasd  sdasd as", "sample description with some length to it", "NULL", freqs));
        beats.add(new BinauralBeat("sample", "sample description wiasdk.fjh asildfghasildjfhglaisjdhfl asdghflias ghdlfghasldj bhfl ajshdflj ahsdlfjgh asljkdfghlkjasdh fj ghalsdjfg lasjdgfl jasgdf jkasdgfl iuagsdlf jkgabsldiuf gasljdfghlaui eghfaöihu öuioasehf ölausehf asuilöh fliuawehf ksjahdf liuaseghfkljashefliu ghasth some length to it", "NULL", freqs));
        beats.add(new BinauralBeat("sample", "sample description with some length to it", "NULL", freqs));
        beats.add(new BinauralBeat("sample", "sample description with some length to it", "NULL", freqs));
        beats.add(new BinauralBeat("sample", "sample description with some length to it", "NULL", freqs));
        beats.add(new BinauralBeat("sample", "sample description with some length to it", "NULL", freqs));
        beats.add(new BinauralBeat("sample", "sample description with some length to it", "NULL", freqs));
        beats.add(new BinauralBeat("sample", "sample description with some length to it", "NULL", freqs));
        beats.add(new BinauralBeat("sample", "sample description with some length to it", "NULL", freqs));
        beats.add(new BinauralBeat("sample", "sample description with some length to it", "NULL", freqs));
        beats.add(new BinauralBeat("samplea asd asd as", "sample description with some length to it", "NULL", freqs));
        beats.add(new BinauralBeat("sample", "sample description with some length to it", "NULL", freqs));
        beats.add(new BinauralBeat("sample dd d d  d d dd", "sample description with some length to it", "NULL", freqs));
        RecyclerViewAdapterBinauralBeatsSelector rvabbs = new RecyclerViewAdapterBinauralBeatsSelector(getContext(), beats);
        rvabbs.setOnEntryClickedListener((binauralBeat, position) -> {
            System.out.println(binauralBeat.getTitle());
        });
        binauralBeatsSelector.setAdapter(rvabbs);
        binauralBeatsSelector.setLayoutManager(new LinearLayoutManager(getContext()));

        progressLineGraph.setData(freqs, 32, 4f, false, Brainwaves.getStageColors(), Brainwaves.getStageFrequencyCenters());

        Thread newThread = new Thread(() -> {
            for (int i = 0; i < freqs.getDuration(); i++){
                progressLineGraph.updateProgress(i);
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        newThread.start();
    }
}