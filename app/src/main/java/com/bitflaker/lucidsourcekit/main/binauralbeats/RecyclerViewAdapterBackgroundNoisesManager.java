package com.bitflaker.lucidsourcekit.main.binauralbeats;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.data.BackgroundNoise;
import com.bitflaker.lucidsourcekit.databinding.EntryBinauralBackgroundNoiseBinding;

import java.util.List;

public class RecyclerViewAdapterBackgroundNoisesManager extends RecyclerView.Adapter<RecyclerViewAdapterBackgroundNoisesManager.MainViewHolderBackgroundNoises> {
    private final List<BackgroundNoise> backgroundNoises;
    private final Context context;
    private OnVolumeChanged mVolumeChangedListener;
    private OnEntryClicked mListener;

    public RecyclerViewAdapterBackgroundNoisesManager(Context context, List<BackgroundNoise> backgroundNoises) {
        this.context = context;
        this.backgroundNoises = backgroundNoises;
    }

    @NonNull
    @Override
    public MainViewHolderBackgroundNoises onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        return new MainViewHolderBackgroundNoises(EntryBinauralBackgroundNoiseBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MainViewHolderBackgroundNoises holder, int position) {
        holder.binding.txtBackgroundNoiseName.setText(backgroundNoises.get(position).getName());
        holder.binding.sldVolumeBackgroundNoise.setValue(backgroundNoises.get(position).getVolume());
        holder.binding.imgNoiseIcon.setImageDrawable(context.getResources().getDrawable(backgroundNoises.get(position).getIcon(), context.getTheme()));
        holder.binding.imgNoiseIcon.setOnClickListener(e -> {
            if(mListener != null) {
                // TODO display that it is paused by for example changing slider color to gray?
                backgroundNoises.get(position).setPaused(!backgroundNoises.get(position).isPaused());
                mListener.onEvent(backgroundNoises.get(position), position);
            }
        });
        holder.binding.sldVolumeBackgroundNoise.addOnChangeListener((slider, value, fromUser) -> {
            if(fromUser && mVolumeChangedListener != null){
                backgroundNoises.get(position).setVolume(value);
                mVolumeChangedListener.onEvent(backgroundNoises.get(position), position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return backgroundNoises.size();
    }

    public static class MainViewHolderBackgroundNoises extends RecyclerView.ViewHolder {
        EntryBinauralBackgroundNoiseBinding binding;

        public MainViewHolderBackgroundNoises(@NonNull EntryBinauralBackgroundNoiseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public interface OnEntryClicked {
        void onEvent(BackgroundNoise noise, int position);
    }

    public void setOnEntryClickedListener(OnEntryClicked eventListener) {
        mListener = eventListener;
    }

    public interface OnVolumeChanged {
        void onEvent(BackgroundNoise noise, int position);
    }

    public void setOnVolumeChangedListener(OnVolumeChanged eventListener) {
        mVolumeChangedListener = eventListener;
    }
}
