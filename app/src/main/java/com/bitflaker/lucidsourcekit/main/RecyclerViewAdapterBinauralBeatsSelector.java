package com.bitflaker.lucidsourcekit.main;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.charts.Brainwaves;
import com.bitflaker.lucidsourcekit.charts.LineGraph;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

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
        View view = inflater.inflate(R.layout.binaural_entry, parent, false);
        return new MainViewHolderBinauralBeats(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MainViewHolderBinauralBeats holder, int position) {
        holder.binauralTitle.setText(binauralBeats.get(position).getTitle());
        holder.binauralDescription.setText(binauralBeats.get(position).getDescription());
        holder.baseFrequency.setText(binauralBeats.get(position).getBaseFrequencyString());

        holder.dataGraph.setData(binauralBeats.get(position).getFrequencyList(), 32, 3f, 0f, Brainwaves.getStageColors(), Brainwaves.getStageFrequencyCenters());
        holder.dataGraph.changeProgressIndicator(Color.TRANSPARENT, 0);

        holder.card.setOnClickListener(e -> {
            if(mListener != null) {
                mListener.onEvent(binauralBeats.get(position), position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return binauralBeats.size();
    }

    public class MainViewHolderBinauralBeats extends RecyclerView.ViewHolder {
        TextView binauralTitle, binauralDescription, baseFrequency;
        MaterialCardView card;
        LineGraph dataGraph;

        public MainViewHolderBinauralBeats(@NonNull View itemView) {
            super(itemView);
            binauralTitle = itemView.findViewById(R.id.txt_binaural_title);
            binauralDescription = itemView.findViewById(R.id.txt_binaural_description);
            card = itemView.findViewById(R.id.crd_binaural_selection_card);
            dataGraph = itemView.findViewById(R.id.lg_binaural_gradient);
            baseFrequency = itemView.findViewById(R.id.txt_binaural_beats_base_frequency);
        }
    }

    public interface OnEntryClicked {
        void onEvent(BinauralBeat binauralBeat, int position);
    }

    public void setOnEntryClickedListener(OnEntryClicked eventListener) {
        mListener = eventListener;
    }
}
