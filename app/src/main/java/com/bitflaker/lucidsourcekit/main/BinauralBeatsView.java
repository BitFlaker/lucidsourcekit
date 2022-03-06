package com.bitflaker.lucidsourcekit.main;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.charts.Brainwaves;
import com.bitflaker.lucidsourcekit.charts.FrequencyData;
import com.bitflaker.lucidsourcekit.charts.FrequencyList;
import com.bitflaker.lucidsourcekit.charts.LineGraph;
import com.bitflaker.lucidsourcekit.charts.TextLegend;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.bitflaker.lucidsourcekit.main.binauralbeats.BinauralBeatsPlayer;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class BinauralBeatsView extends Fragment {
    private LineGraph progressLineGraph;
    private ImageButton displayAllBeats, backgroundNoises, repeatButton, autoStopButton, playTrack;
    private List<BackgroundNoise> noises;
    private boolean repeatBeat;
    private Date autoStopTime;
    private RelativeLayout noBeatSelected, beatSelected;
    private TextView currentTrackName, currentTrackDescription, binauralTimeline, binauralTimeTotal, binauralFrequency;
    private TextLegend binauralLegend;
    private BinauralBeatsPlayer binBeatPlayer;

    // TODO really repeat beats
    // TODO really start and pause beats
    // TODO really stop with Auto-Stop
    // TODO get real noises
    // TODO get real binaural beats

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_binaural_beats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressLineGraph = getView().findViewById(R.id.lg_binaural_time_progress);
        displayAllBeats = getView().findViewById(R.id.btn_display_all_beats);
        backgroundNoises = getView().findViewById(R.id.btn_add_background_noise);
        repeatButton = getView().findViewById(R.id.btn_loop_track);
        autoStopButton = getView().findViewById(R.id.btn_auto_stop);
        noBeatSelected = getView().findViewById(R.id.rl_no_binaural_beat_selected);
        beatSelected = getView().findViewById(R.id.rl_playing_binaural_beat);
        currentTrackName = getView().findViewById(R.id.txt_curr_track_name);
        currentTrackDescription = getView().findViewById(R.id.txt_curr_track_description);
        playTrack = getView().findViewById(R.id.btn_play_track);
        binauralLegend = getView().findViewById(R.id.tl_binaural_legend);
        binauralTimeline = getView().findViewById(R.id.txt_binaural_beats_timeline);
        binauralTimeTotal = getView().findViewById(R.id.txt_binaural_beats_total_time);
        binauralFrequency = getView().findViewById(R.id.txt_current_binaural_frequency);

        repeatBeat = false;
        autoStopTime = null;

        generateNoises();

        FrequencyList freqs = new FrequencyList();
        //freqs.add(new FrequencyData(32, 20));
        //freqs.add(new FrequencyData(32, 15, 30));
        //freqs.add(new FrequencyData(15, 13, 12));
        //freqs.add(new FrequencyData(13, 10));
        //freqs.add(new FrequencyData(13, 8, 20));
        //freqs.add(new FrequencyData(8, 10));
        freqs.add(new FrequencyData(30, 5, 15));
        freqs.add(new FrequencyData(5, 5));         // 4 & 5 = no crack ???
        freqs.add(new FrequencyData(5, 2, 5));      // jump = no crack ???
        freqs.add(new FrequencyData(2, 5));
        freqs.add(new FrequencyData(2, 3, 5));      // jump = no crack ???
        freqs.add(new FrequencyData(3, 5));      // jump = no crack ???
        freqs.add(new FrequencyData(3, 32, 35));

        /*
        freqs.add(new FrequencyData(30, 4, 5));
        freqs.add(new FrequencyData(4, 5));
        freqs.add(new FrequencyData(4, 1, 5));      // crack ???
        freqs.add(new FrequencyData(1, 5));
        freqs.add(new FrequencyData(1, 4, 5));      // crack ???
        freqs.add(new FrequencyData(4, 32, 35));
        */

        displayAllBeats.setOnClickListener(e -> {
            final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext(), R.style.BottomSheetDialog_Dark);
            bottomSheetDialog.setContentView(R.layout.binaural_beats_selector_sheet);
            RecyclerView rcv = bottomSheetDialog.findViewById(R.id.rcv_list_binaural_beats);

            List<BinauralBeat> beats = new ArrayList<>();
            beats.add(new BinauralBeat("Quick Nap Lucidity ", "Great for supporting the induction of lucid dreams during a quick nap.", 455, "NULL", freqs));
            beats.add(new BinauralBeat("sampleasd ", "sample description with some length to it", 800, "NULL", freqs));
            beats.add(new BinauralBeat("sample asdas d", "sample description with some length to it", 234.5f, "NULL", freqs));
            beats.add(new BinauralBeat("sampleasd  sdasd as", "sample description with some length to it", 455, "NULL", freqs));
            beats.add(new BinauralBeat("sample", "sample description wiasdk.fjh asildfghasildjfhglaisjdhfl asdgghflias sdf sd fklsdfklsdfsdf s has some length to it", 1633.75f, "NULL", freqs));
            beats.add(new BinauralBeat("sample", "sample description with some length to it", 455, "NULL", freqs));
            beats.add(new BinauralBeat("sample", "sample description with some length to it", 345, "NULL", freqs));
            beats.add(new BinauralBeat("sample", "sample description with some length to it", 455, "NULL", freqs));
            beats.add(new BinauralBeat("sample", "sample description with some length to it", 1845, "NULL", freqs));
            beats.add(new BinauralBeat("sample", "sample description with some length to it", 455, "NULL", freqs));
            beats.add(new BinauralBeat("sample", "sample description with some length to it", 455, "NULL", freqs));
            beats.add(new BinauralBeat("sample", "sample description with some length to it", 153.67f, "NULL", freqs));
            beats.add(new BinauralBeat("sample", "sample description with some length to it", 455, "NULL", freqs));
            beats.add(new BinauralBeat("samplea asd asd as", "sample description with some length to it", 685, "NULL", freqs));
            beats.add(new BinauralBeat("sample", "sample description with some length to it", 455, "NULL", freqs));
            beats.add(new BinauralBeat("sample dd d d  d d dd", "sample description with some length to it", 455, "NULL", freqs));
            RecyclerViewAdapterBinauralBeatsSelector rvabbs = new RecyclerViewAdapterBinauralBeatsSelector(getContext(), beats);
            rvabbs.setOnEntryClickedListener((binauralBeat, position) -> {
                bottomSheetDialog.hide();
                System.out.println(binauralBeat.getTitle());
                noBeatSelected.setVisibility(GONE);
                beatSelected.setVisibility(VISIBLE);
                currentTrackName.setText(binauralBeat.getTitle());
                currentTrackDescription.setText(binauralBeat.getDescription());
                binauralTimeTotal.setText(String.format(" / %s", getTimeStringFromSeconds((int) binauralBeat.getFrequencyList().getDuration())));
                binauralTimeline.setText(getTimeStringFromSeconds(0));
                binauralFrequency.setText("0,00 Hz");
                binBeatPlayer = new BinauralBeatsPlayer(binauralBeat);
                binBeatPlayer.setOnTrackProgressListener(((currentBinauralBeat, progress) -> {
                    progressLineGraph.updateProgress(progress);
                    getActivity().runOnUiThread(() -> {
                        binauralTimeline.setText(getTimeStringFromSeconds(progress));
                        binauralFrequency.setText(String.format("%.2f Hz", currentBinauralBeat.getFrequencyList().getFrequencyAtDuration(progress)));
                    });
                }));
                binBeatPlayer.setOnTrackFinishedListener(currentBinauralBeat -> {
                    playTrack.setImageDrawable(getContext().getDrawable(R.drawable.ic_baseline_play_arrow_24));
                });
            });
            rcv.setAdapter(rvabbs);
            rcv.setLayoutManager(new LinearLayoutManager(getContext()));

            bottomSheetDialog.show();
        });

        backgroundNoises.setOnClickListener(e -> {
            final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext(), R.style.BottomSheetDialog_Dark);
            bottomSheetDialog.setContentView(R.layout.background_noises_manager);
            RecyclerView rcv = bottomSheetDialog.findViewById(R.id.rcv_list_background_noises);

            RecyclerViewAdapterBackgroundNoisesManager manager = new RecyclerViewAdapterBackgroundNoisesManager(getContext(), noises);
            manager.setOnEntryClickedListener((backgroundNoise, position) -> {
                System.out.println(backgroundNoise.getName());
            });
            manager.setOnVolumeChangedListener((backgroundNoise, position) -> {
                System.out.println(backgroundNoise.getName() + " | " + backgroundNoise.getVolume());
            });
            rcv.setAdapter(manager);
            rcv.setLayoutManager(new LinearLayoutManager(getContext()));

            bottomSheetDialog.show();
        });

        repeatButton.setOnClickListener(e -> {
            repeatBeat = !repeatBeat;
            if (repeatBeat){
                repeatButton.setImageDrawable(getContext().getDrawable(R.drawable.ic_baseline_repeat_on_24));
                Toast.makeText(getContext(), "repeat is now on", Toast.LENGTH_SHORT).show();
            }
            else {
                repeatButton.setImageDrawable(getContext().getDrawable(R.drawable.ic_baseline_repeat_24));
                Toast.makeText(getContext(), "repeat is now off", Toast.LENGTH_SHORT).show();
            }
        });

        autoStopButton.setOnClickListener(e -> {
            if(autoStopTime != null){
                new AlertDialog.Builder(getContext(), Tools.getThemeDialog()).setTitle("Disable Auto-Stop").setMessage("Do you really want to disable Auto-Stop?")
                        .setPositiveButton(getResources().getString(R.string.yes), (dialog, which) -> {
                            autoStopTime = null;
                            autoStopButton.setImageDrawable(getContext().getDrawable(R.drawable.ic_outline_timer_24));
                        })
                        .setNegativeButton(getResources().getString(R.string.no), null)
                        .show();
            }
            else {
                BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext(), R.style.BottomSheetDialog_Dark);
                bottomSheetDialog.setContentView(R.layout.auto_stop_duration_sheet);
                MaterialButton applyAutoStop = bottomSheetDialog.findViewById(R.id.btn_apply_auto_stop);
                NumberPicker autoStopHours = bottomSheetDialog.findViewById(R.id.np_hours_auto_stop);
                NumberPicker autoStopMinutes = bottomSheetDialog.findViewById(R.id.np_minutes_auto_stop);
                NumberPicker autoStopSeconds = bottomSheetDialog.findViewById(R.id.np_seconds_auto_stop);

                autoStopHours.setMinValue(0);
                autoStopHours.setMaxValue(23);
                autoStopHours.setValue(0);
                autoStopMinutes.setMinValue(0);
                autoStopMinutes.setMaxValue(59);
                autoStopMinutes.setValue(0);
                autoStopSeconds.setMinValue(0);
                autoStopSeconds.setMaxValue(59);
                autoStopSeconds.setValue(0);

                applyAutoStop.setOnClickListener(g -> {
                    Date dt = new Date();
                    Calendar cal = new GregorianCalendar();
                    cal.setTimeInMillis(dt.getTime());
                    cal.add(Calendar.SECOND, autoStopHours.getValue()*3600+autoStopMinutes.getValue()*60+autoStopSeconds.getValue());
                    autoStopTime = cal.getTime();
                    System.out.println(autoStopTime);
                    autoStopButton.setImageDrawable(getContext().getDrawable(R.drawable.ic_outline_timer_off_24));
                    bottomSheetDialog.hide();
                });

                bottomSheetDialog.show();
            }
        });

        playTrack.setOnClickListener(e -> {
            if(binBeatPlayer != null){
                if(!binBeatPlayer.isPlaying()){
                    playTrack.setImageDrawable(getContext().getDrawable(R.drawable.ic_baseline_pause_24));
                    binBeatPlayer.play();
                }
                else {
                    playTrack.setImageDrawable(getContext().getDrawable(R.drawable.ic_baseline_play_arrow_24));
                    binBeatPlayer.pause();
                }
            }
            else {
                Toast.makeText(getContext(), "no binaural beat selected", Toast.LENGTH_SHORT).show();
            }
        });

        progressLineGraph.setBottomLineSpacing(20);
        progressLineGraph.setData(freqs, 32, 4f, Brainwaves.getStageColors(), Brainwaves.getStageFrequencyCenters());
        progressLineGraph.setDrawGradient(true);
        progressLineGraph.setGradientOpacity(0.15f);

        String[] labels = new String[] { "beta", "alpha", "theta", "delta" };
        binauralLegend.setData(labels, Brainwaves.getStageColors(), Tools.getAttrColor(R.attr.primaryTextColor, getContext().getTheme()), 12);

        Thread newThread2 = new Thread(() -> {
            for (int i = 0; i < freqs.getDuration(); i++){
                final Runtime runtime = Runtime.getRuntime();
                final long usedMemInMB=(runtime.totalMemory() - runtime.freeMemory()) / 1048576L;
                final long maxHeapSizeInMB=runtime.maxMemory() / 1048576L;
                final long availHeapSizeInMB = maxHeapSizeInMB - usedMemInMB;
                System.out.println(String.format("Resource-Usage: [%d] [%d] [%d]", usedMemInMB, maxHeapSizeInMB, availHeapSizeInMB));
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        newThread2.start();
    }

    private String getTimeStringFromSeconds(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        return (hours > 0 ? String.format("%02d:", hours) : "") + String.format("%02d:%02d", minutes, seconds);
    }

    private void generateNoises() {
        noises = new ArrayList<>();
        noises.add(new BackgroundNoise("Flowing water", R.drawable.ic_baseline_water_24, 25));
        noises.add(new BackgroundNoise("Explosions", R.drawable.ic_baseline_brightness_7_24, 0));
        noises.add(new BackgroundNoise("Wind", R.drawable.ic_baseline_air_24, 75));
        noises.add(new BackgroundNoise("Alarm", R.drawable.ic_baseline_access_alarm_24, 10));
        noises.add(new BackgroundNoise("White noise", R.drawable.ic_baseline_waves_24, 100));
    }
}