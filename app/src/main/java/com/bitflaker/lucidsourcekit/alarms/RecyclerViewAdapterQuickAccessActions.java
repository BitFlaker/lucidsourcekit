package com.bitflaker.lucidsourcekit.alarms;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.R;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class RecyclerViewAdapterQuickAccessActions extends RecyclerView.Adapter<RecyclerViewAdapterQuickAccessActions.MainViewHolderQuickAccessActions> {
    private OnEntryClicked mListener;
    private Context context;
    private List<QuickAccessAction> quickAccessActions;

    public RecyclerViewAdapterQuickAccessActions(Context context, List<QuickAccessAction> quickAccessActions) {
        this.context = context;
        this.quickAccessActions = quickAccessActions;
    }

    @NonNull
    @Override
    public MainViewHolderQuickAccessActions onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.quick_access_action, parent, false);
        return new MainViewHolderQuickAccessActions(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MainViewHolderQuickAccessActions holder, int position) {
        holder.title.setText(quickAccessActions.get(position).getTitle());
        holder.description.setText(quickAccessActions.get(position).getDescription());
        holder.primaryIcon.setImageDrawable(quickAccessActions.get(position).getPrimaryIcon());
        Drawable secondaryIcon = quickAccessActions.get(position).getSecondaryIcon();
        if(secondaryIcon == null){
            holder.secondaryIcon.setVisibility(View.GONE);
        }
        else {
            holder.secondaryIcon.setImageDrawable(secondaryIcon);
            holder.secondaryIcon.setVisibility(View.VISIBLE);
        }
        holder.card.setOnClickListener(e -> {
            if(mListener != null){
                mListener.onEvent(quickAccessActions.get(position));
            }
        });
    }

    @Override
    public int getItemCount() {
        return quickAccessActions.size();
    }

    public class MainViewHolderQuickAccessActions extends RecyclerView.ViewHolder {
        TextView title, description;
        MaterialCardView card;
        ImageView primaryIcon, secondaryIcon;

        public MainViewHolderQuickAccessActions(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.crd_qac);
            title = itemView.findViewById(R.id.txt_qac_heading);
            description = itemView.findViewById(R.id.txt_qac_description);
            primaryIcon = itemView.findViewById(R.id.img_primary_icon);
            secondaryIcon = itemView.findViewById(R.id.img_secondary_icon);
        }
    }

    public interface OnEntryClicked {
        void onEvent(QuickAccessAction quickAccessAction);
    }

    public void setOnEntryClickedListener(OnEntryClicked eventListener) {
        mListener = eventListener;
    }
}
