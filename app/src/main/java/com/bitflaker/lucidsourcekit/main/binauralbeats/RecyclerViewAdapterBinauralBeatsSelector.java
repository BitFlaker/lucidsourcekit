package com.bitflaker.lucidsourcekit.main.binauralbeats;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.data.Brainwaves;
import com.bitflaker.lucidsourcekit.data.records.BinauralBeat;
import com.bitflaker.lucidsourcekit.views.LineGraph;
import com.google.android.material.card.MaterialCardView;

import java.util.List;
import java.util.Locale;

public class RecyclerViewAdapterBinauralBeatsSelector extends RecyclerView.Adapter<RecyclerViewAdapterBinauralBeatsSelector.MainViewHolderBinauralBeats> {
    private OnEntryClicked mListener;
    private Context context;
    private List<BinauralBeat> binauralBeats;

    public RecyclerViewAdapterBinauralBeatsSelector(Context context, List<BinauralBeat> binauralBeats) {
        this.context = context;
        this.binauralBeats = binauralBeats;
    }

    @NonNull
    @Override
    public MainViewHolderBinauralBeats onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.entry_binaural, parent, false);
        return new MainViewHolderBinauralBeats(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MainViewHolderBinauralBeats holder, int position) {
        holder.binauralTitle.setText(binauralBeats.get(position).title());
        holder.binauralDescription.setText(binauralBeats.get(position).description());
        holder.baseFrequency.setText(getBaseFrequencyString(binauralBeats.get(position)));
        int seconds = ((int) binauralBeats.get(position).frequencyList().getDuration());
        int sec = seconds % 60;
        int min = (seconds / 60)%60;
        int hours = (seconds/60)/60;
        String secS = String.format(Locale.ENGLISH, "%02d" , sec);
        String minS = String.format(Locale.ENGLISH, "%02d" , min);
        String hoursS = String.format(Locale.ENGLISH, "%02d" , hours);
        holder.beatsDuration.setText(hours > 0 ? hoursS + ":" : "" + minS + ":" + secS);
        holder.dataGraph.setData(binauralBeats.get(position).frequencyList(), 32, 3f, 0f, true, Brainwaves.getStageColors(), Brainwaves.getStageFrequencyCenters());

        holder.card.setOnClickListener(e -> {
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

    public class MainViewHolderBinauralBeats extends RecyclerView.ViewHolder {
        TextView binauralTitle, binauralDescription, baseFrequency, beatsDuration;
        MaterialCardView card;
        LineGraph dataGraph;

        public MainViewHolderBinauralBeats(@NonNull View itemView) {
            super(itemView);
            binauralTitle = itemView.findViewById(R.id.txt_binaural_title);
            binauralDescription = itemView.findViewById(R.id.txt_binaural_description);
            card = itemView.findViewById(R.id.crd_binaural_selection_card);
            dataGraph = itemView.findViewById(R.id.lg_binaural_gradient);
            baseFrequency = itemView.findViewById(R.id.txt_binaural_beats_base_frequency);
            beatsDuration = itemView.findViewById(R.id.txt_binaural_beats_duration);

            dataGraph.setDrawProgressIndicator(false);
        }
    }

    public interface OnEntryClicked {
        void onEvent(BinauralBeat binauralBeat, int position);
    }

    public void setOnEntryClickedListener(OnEntryClicked eventListener) {
        mListener = eventListener;
    }
}
