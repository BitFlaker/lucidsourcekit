package com.bitflaker.lucidsourcekit.main.binauralbeats;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.data.BackgroundNoise;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.slider.Slider;

import java.util.List;

public class RecyclerViewAdapterBackgroundNoisesManager extends RecyclerView.Adapter<RecyclerViewAdapterBackgroundNoisesManager.MainViewHolderBackgroundNoises> {
    private OnEntryClicked mListener;
    private OnVolumeChanged mVolumeChangedListener;
    private final Context context;
    private List<BackgroundNoise> backgroundNoises;

    public RecyclerViewAdapterBackgroundNoisesManager(Context context, List<BackgroundNoise> backgroundNoises) {
        this.context = context;
        this.backgroundNoises = backgroundNoises;
    }

    @NonNull
    @Override
    public MainViewHolderBackgroundNoises onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.entry_binaural_background_noise, parent, false);
        return new MainViewHolderBackgroundNoises(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MainViewHolderBackgroundNoises holder, int position) {
        holder.noiseTitle.setText(backgroundNoises.get(position).getName());
        holder.volumeSlider.setValue(backgroundNoises.get(position).getVolume());
        holder.icon.setImageDrawable(context.getResources().getDrawable(backgroundNoises.get(position).getIcon(), context.getTheme()));
        holder.icon.setOnClickListener(e -> {
            if(mListener != null) {
                // TODO display that it is paused by for example changing slider color to gray?
                backgroundNoises.get(position).setPaused(!backgroundNoises.get(position).isPaused());
                mListener.onEvent(backgroundNoises.get(position), position);
            }
        });
        holder.volumeSlider.addOnChangeListener((slider, value, fromUser) -> {
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
        TextView noiseTitle;
        MaterialCardView card;
        Slider volumeSlider;
        ImageView icon;

        public MainViewHolderBackgroundNoises(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.crd_background_noise_manager_card);
            icon = itemView.findViewById(R.id.img_noise_icon);
            noiseTitle = itemView.findViewById(R.id.txt_background_noise_name);
            volumeSlider = itemView.findViewById(R.id.sld_volume_background_noise);
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
