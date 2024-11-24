package com.bitflaker.lucidsourcekit.main.binauralbeats;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.data.BackgroundNoise;
import com.bitflaker.lucidsourcekit.data.Brainwaves;
import com.bitflaker.lucidsourcekit.data.records.BinauralBeat;
import com.bitflaker.lucidsourcekit.databinding.FragmentMainBinauralBeatsBinding;
import com.bitflaker.lucidsourcekit.databinding.SheetBinauralAutoStopBinding;
import com.bitflaker.lucidsourcekit.databinding.SheetBinauralBackgroundNoiseBinding;
import com.bitflaker.lucidsourcekit.databinding.SheetBinauralBeatsBinding;
import com.bitflaker.lucidsourcekit.utils.Tools;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BinauralBeatsView extends Fragment {
    private FragmentMainBinauralBeatsBinding binding;
    private Handler autoStopHandler = new Handler();
    private BinauralBeatsPlayer binBeatPlayer;
    private List<BackgroundNoise> noises;
    private boolean repeatBeat, playingFinished, isAutoStopTimerRunning;
    private int autoStopInterval = -1;

    private final Runnable stopCurrentlyPlayingTrack = () -> {
        binding.btnPlayTrack.setIcon(getContext().getDrawable(R.drawable.ic_baseline_play_arrow_24));
        binBeatPlayer.pause();
        binding.btnAutoStop.setImageDrawable(getContext().getDrawable(R.drawable.ic_outline_timer_24));
        autoStopInterval = -1;
    };

    // TODO get real noises

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMainBinauralBeatsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.txtBinauralBeatsHeading.setLayoutParams(Tools.getRelativeLayoutParamsTopStatusbar(getContext()));
        repeatBeat = false;
        playingFinished = false;
        generateNoises();

        binding.btnDisplayAllBeats.setOnClickListener(e -> {
            final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext(), R.style.BottomSheetDialogStyle);
            SheetBinauralBeatsBinding beatsBinding = SheetBinauralBeatsBinding.inflate(getLayoutInflater());
            bottomSheetDialog.setContentView(beatsBinding.getRoot());

            RecyclerViewAdapterBinauralBeatsSelector rvabbs = new RecyclerViewAdapterBinauralBeatsSelector(getContext(), BinauralBeatsCollection.getInstance().getBinauralBeats());
            rvabbs.setOnEntryClickedListener((binauralBeat, position) -> {
                playingFinished = false;
                bottomSheetDialog.dismiss();
                binding.llBbpTimeContainer.setVisibility(View.VISIBLE);
                LinearLayout.LayoutParams lParams = (LinearLayout.LayoutParams) binding.llBbpCarrierFreqHeading.getLayoutParams();
                lParams.topMargin = Tools.dpToPx(getContext(), 15);
                binding.llBbpCarrierFreqHeading.setLayoutParams(lParams);
                binding.lgBinauralTimeProgress.setVisibility(View.VISIBLE);
                binding.txtBbpCarrierFreqHeading.setVisibility(View.VISIBLE);
                binding.txtCurrTrackName.setVisibility(View.VISIBLE);
                binding.txtCurrTrackName.setText(binauralBeat.title());
                binding.txtCurrTrackDescription.setText(binauralBeat.description());
                binding.txtBinauralBeatsTotalTime.setText(String.format(" / %s", getTimeStringFromSeconds((int) binauralBeat.frequencyList().getDuration())));
                binding.txtBinauralBeatsTimeline.setText(getTimeStringFromSeconds(0));
                binding.txtCurrentBinauralFrequency.setText("0.00");
                binding.txtCarrierFrequency.setText(String.format(Locale.ENGLISH, "%.0f Hz", binauralBeat.baseFrequency()));
                binding.txtCarrierFrequency.setTextColor(Tools.getAttrColor(R.attr.primaryTextColor, getContext().getTheme()));
                TextViewCompat.setCompoundDrawableTintList(binding.txtCarrierFrequency, Tools.getAttrColorStateList(R.attr.secondaryTextColor, getContext().getTheme()));
                binding.btnPlayTrack.setIcon(getContext().getDrawable(R.drawable.ic_baseline_play_arrow_24));
                setDataForProgress(binauralBeat, 0);
                if (binBeatPlayer != null) {
                    binBeatPlayer.stop();
                    binding.lgBinauralTimeProgress.resetProgress();
                }
                binBeatPlayer = new BinauralBeatsPlayer(binauralBeat);
                binding.lgBinauralTimeProgress.setData(binauralBeat.frequencyList(), 32, 4f, 0f, false, Brainwaves.getStageColors(), Brainwaves.getStageFrequencyCenters());
                binBeatPlayer.setOnTrackProgressListener(((currentBinauralBeat, progress) -> {
                    setDataForProgress(currentBinauralBeat, progress);
                    FragmentActivity fragAct = getActivity();
                    if (fragAct != null) {
                        fragAct.runOnUiThread(() -> binding.lgBinauralTimeProgress.updateProgress(progress));
                    }
                    else {
                        binBeatPlayer.stop();
                    }
                }));
                binBeatPlayer.setOnTrackFinishedListener(currentBinauralBeat -> {
                    playingFinished = true;
                    setEndValues(currentBinauralBeat);
                    if(repeatBeat) {
                        playingFinished = false;
                        binding.lgBinauralTimeProgress.resetProgress();
                        binBeatPlayer.play();
                    }
                    else {
                        binding.btnPlayTrack.setIcon(getContext().getDrawable(R.drawable.ic_baseline_play_arrow_24));
                    }
                });
            });
            beatsBinding.rcvListBinauralBeats.setAdapter(rvabbs);
            beatsBinding.rcvListBinauralBeats.setLayoutManager(new LinearLayoutManager(getContext()));
            bottomSheetDialog.show();
        });

        binding.btnAddBackgroundNoise.setOnClickListener(e -> {
            final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext(), R.style.BottomSheetDialogStyle);
            SheetBinauralBackgroundNoiseBinding sBinding = SheetBinauralBackgroundNoiseBinding.inflate(getLayoutInflater());
            bottomSheetDialog.setContentView(sBinding.getRoot());

            RecyclerViewAdapterBackgroundNoisesManager manager = new RecyclerViewAdapterBackgroundNoisesManager(getContext(), noises);
            manager.setOnEntryClickedListener((backgroundNoise, position) -> {
                System.out.println(backgroundNoise.getName());
            });
            manager.setOnVolumeChangedListener((backgroundNoise, position) -> {
                System.out.println(backgroundNoise.getName() + " | " + backgroundNoise.getVolume());
            });
            sBinding.rcvListBackgroundNoises.setAdapter(manager);
            sBinding.rcvListBackgroundNoises.setLayoutManager(new LinearLayoutManager(getContext()));

            bottomSheetDialog.show();
        });

        binding.btnLoopTrack.setOnClickListener(e -> {
            repeatBeat = !repeatBeat;
            if (repeatBeat){
                binding.btnLoopTrack.setImageDrawable(getContext().getDrawable(R.drawable.ic_baseline_repeat_on_24));
                Toast.makeText(getContext(), "repeat is now on", Toast.LENGTH_SHORT).show();
            }
            else {
                binding.btnLoopTrack.setImageDrawable(getContext().getDrawable(R.drawable.ic_baseline_repeat_24));
                Toast.makeText(getContext(), "repeat is now off", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnAutoStop.setOnClickListener(e -> {
            if(autoStopInterval != -1){
                new MaterialAlertDialogBuilder(getContext(), R.style.Theme_LucidSourceKit_ThemedDialog).setTitle("Disable Auto-Stop").setMessage("Do you really want to disable Auto-Stop?")
                        .setPositiveButton(getResources().getString(R.string.yes), (dialog, which) -> {
                            autoStopHandler.removeCallbacks(stopCurrentlyPlayingTrack);
                            binding.btnAutoStop.setImageDrawable(getContext().getDrawable(R.drawable.ic_outline_timer_24));
                            autoStopInterval = -1;
                        })
                        .setNegativeButton(getResources().getString(R.string.no), null)
                        .show();
            }
            else {
                BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext(), R.style.BottomSheetDialogStyle);
                SheetBinauralAutoStopBinding autoStopBinding = SheetBinauralAutoStopBinding.inflate(getLayoutInflater());
                bottomSheetDialog.setContentView(autoStopBinding.getRoot());

                autoStopBinding.npHoursAutoStop.setMinValue(0);
                autoStopBinding.npHoursAutoStop.setMaxValue(23);
                autoStopBinding.npHoursAutoStop.setValue(0);
                autoStopBinding.npMinutesAutoStop.setMinValue(0);
                autoStopBinding.npMinutesAutoStop.setMaxValue(59);
                autoStopBinding.npMinutesAutoStop.setValue(0);
                autoStopBinding.npSecondsAutoStop.setMinValue(0);
                autoStopBinding.npSecondsAutoStop.setMaxValue(59);
                autoStopBinding.npSecondsAutoStop.setValue(0);

                autoStopBinding.btnApplyAutoStop.setOnClickListener(g -> {
                    isAutoStopTimerRunning = false;
                    autoStopInterval = (autoStopBinding.npHoursAutoStop.getValue() * 3600 + autoStopBinding.npMinutesAutoStop.getValue() * 60 + autoStopBinding.npSecondsAutoStop.getValue()) * 1000;
                    autoStopHandler = new Handler();
                    if(binBeatPlayer != null && binBeatPlayer.isPlaying()) {
                        startAutoStopTimeNow();
                    }
                    binding.btnAutoStop.setImageDrawable(getContext().getDrawable(R.drawable.ic_outline_timer_off_24));
                    bottomSheetDialog.dismiss();
                });
                bottomSheetDialog.show();
            }
        });

        binding.btnPlayTrack.setOnClickListener(e -> {
            if(binBeatPlayer != null){
                if(playingFinished) {
                    binding.txtBinauralBeatsTimeline.setText(getTimeStringFromSeconds(0));
                    binding.lgBinauralTimeProgress.resetProgress();
                    playingFinished = false;
                }
                if(!binBeatPlayer.isPlaying()){
                    binding.btnPlayTrack.setIcon(getContext().getDrawable(R.drawable.ic_baseline_pause_24));
                    binBeatPlayer.play();
                    if(autoStopInterval != -1 && !isAutoStopTimerRunning) {
                        startAutoStopTimeNow();
                    }
                }
                else {
                    binding.btnPlayTrack.setIcon(getContext().getDrawable(R.drawable.ic_baseline_play_arrow_24));
                    binBeatPlayer.pause();
                }
            }
            else {
                Toast.makeText(getContext(), "no binaural beat selected", Toast.LENGTH_SHORT).show();
            }
        });

        binding.lgBinauralTimeProgress.setBottomLineSpacing(10);
        binding.lgBinauralTimeProgress.setDrawGradient(true);
        binding.lgBinauralTimeProgress.setGradientOpacity(0.25f);
        binding.lgBinauralTimeProgress.setDrawProgressIndicator(false);

        String[] labels = new String[] { "β", "α", "θ", "δ" };
        binding.tlBinauralLegend.setData(labels, Brainwaves.getStageColors(), Tools.getAttrColor(R.attr.secondaryTextColor, getContext().getTheme()), Tools.getAttrColor(R.attr.tertiaryTextColor, getContext().getTheme()), 18);
    }

    private void startAutoStopTimeNow() {
        autoStopHandler.postAtTime(stopCurrentlyPlayingTrack, System.currentTimeMillis() + autoStopInterval);
        autoStopHandler.postDelayed(stopCurrentlyPlayingTrack, autoStopInterval);
        isAutoStopTimerRunning = true;
    }

    private void setEndValues(BinauralBeat currentBinauralBeat) {
        getActivity().runOnUiThread(() -> {
            int finishProgress = (int) currentBinauralBeat.frequencyList().getDuration();
            binding.txtBinauralBeatsTimeline.setText(getTimeStringFromSeconds(finishProgress));
            binding.txtCurrentBinauralFrequency.setText(String.format(Locale.ENGLISH, "%.2f", currentBinauralBeat.frequencyList().getFrequencyAtDuration(finishProgress)));
            // TODO: the progress updating is a bit a workaround and should be better
            binding.lgBinauralTimeProgress.updateProgress(binding.lgBinauralTimeProgress.getDurationProgress());
        });
    }

    private void setDataForProgress(BinauralBeat binauralBeat, int progress) {
        double currFreq = binauralBeat.frequencyList().getFrequencyAtDuration(progress);
        int stageIndex = Brainwaves.getStageIndex(currFreq);
        String greekLetter = Brainwaves.getStageFrequencyGreekLetters()[stageIndex];
        String greekLetterName = Brainwaves.getStageFrequencyGreekLetterNames()[stageIndex];
        FragmentActivity fragAct = getActivity();
        if (fragAct != null) {
            fragAct.runOnUiThread(() -> {
                binding.txtCurrentFrequencyGreekLetter.setText(greekLetter);
                binding.txtCurrentFrequencyName.setText(greekLetterName);
                binding.tlBinauralLegend.setCurrentSelectedIndex(stageIndex);
                binding.txtCurrentBinauralFrequency.setText(String.format(Locale.ENGLISH, "%.2f", currFreq));
                binding.txtBinauralBeatsTimeline.setText(getTimeStringFromSeconds(progress));
            });
        }
        else {
            binBeatPlayer.stop();
        }
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