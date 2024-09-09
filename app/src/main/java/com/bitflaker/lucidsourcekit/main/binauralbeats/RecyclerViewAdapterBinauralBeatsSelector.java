package com.bitflaker.lucidsourcekit.main.binauralbeats;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.data.Brainwaves;
import com.bitflaker.lucidsourcekit.data.records.BinauralBeat;
import com.bitflaker.lucidsourcekit.databinding.EntryBinauralBinding;

import java.util.List;
import java.util.Locale;

public class RecyclerViewAdapterBinauralBeatsSelector extends RecyclerView.Adapter<RecyclerViewAdapterBinauralBeatsSelector.MainViewHolderBinauralBeats> {
    private final List<BinauralBeat> binauralBeats;
    private final Context context;
    private OnEntryClicked mListener;

    public RecyclerViewAdapterBinauralBeatsSelector(Context context, List<BinauralBeat> binauralBeats) {
        this.context = context;
        this.binauralBeats = binauralBeats;
    }

    @NonNull
    @Override
    public MainViewHolderBinauralBeats onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        return new MainViewHolderBinauralBeats(EntryBinauralBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MainViewHolderBinauralBeats holder, int position) {
        holder.binding.txtBinauralTitle.setText(binauralBeats.get(position).title());
        holder.binding.txtBinauralDescription.setText(binauralBeats.get(position).description());
        holder.binding.txtBinauralBeatsBaseFrequency.setText(getBaseFrequencyString(binauralBeats.get(position)));
        int seconds = ((int) binauralBeats.get(position).frequencyList().getDuration());
        int sec = seconds % 60;
        int min = (seconds / 60)%60;
        int hours = (seconds/60)/60;
        String secS = String.format(Locale.ENGLISH, "%02d" , sec);
        String minS = String.format(Locale.ENGLISH, "%02d" , min);
        String hoursS = String.format(Locale.ENGLISH, "%02d" , hours);
        holder.binding.txtBinauralBeatsDuration.setText(hours > 0 ? hoursS + ":" : minS + ":" + secS);
        holder.binding.lgBinauralGradient.setData(binauralBeats.get(position).frequencyList(), 32, 3f, 0f, true, Brainwaves.getStageColors(), Brainwaves.getStageFrequencyCenters());

        holder.binding.crdBinauralSelectionCard.setOnClickListener(e -> {
            if(mListener != null) {
                mListener.onEvent(binauralBeats.get(position), position);
            }
        });
    }

    public String getBaseFrequencyString(BinauralBeat binauralBeat) {
        if(binauralBeat.baseFrequency() == (int) binauralBeat.baseFrequency()) {
            return String.format(Locale.getDefault(), "%d Hz", (int) binauralBeat.baseFrequency());
        }
        return String.format("%s Hz", binauralBeat.baseFrequency());
    }

    @Override
    public int getItemCount() {
        return binauralBeats.size();
    }

    public static class MainViewHolderBinauralBeats extends RecyclerView.ViewHolder {
        EntryBinauralBinding binding;

        public MainViewHolderBinauralBeats(@NonNull EntryBinauralBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            binding.lgBinauralGradient.setDrawProgressIndicator(false);
        }
    }

    public interface OnEntryClicked {
        void onEvent(BinauralBeat binauralBeat, int position);
    }

    public void setOnEntryClickedListener(OnEntryClicked eventListener) {
        mListener = eventListener;
    }
}
