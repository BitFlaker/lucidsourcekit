package com.bitflaker.lucidsourcekit.main;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class RecyclerViewAdapterGoalAdvice extends RecyclerView.Adapter<RecyclerViewAdapterGoalAdvice.MainViewHolderGoalAdvices> {
    private OnEntryClicked mListener;
    private Context context;
    private List<GoalAdvice> gaolAdvices;

    public RecyclerViewAdapterGoalAdvice(Context context, List<GoalAdvice> gaolAdvices) {
        this.context = context;
        this.gaolAdvices = gaolAdvices;
    }

    @NonNull
    @Override
    public MainViewHolderGoalAdvices onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.goal_quick_advice, parent, false);
        return new MainViewHolderGoalAdvices(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MainViewHolderGoalAdvices holder, int position) {
        RecyclerView.LayoutParams lParams = (RecyclerView.LayoutParams) holder.card.getLayoutParams();
        if(position == 0) {
            lParams.leftMargin = Tools.dpToPx(context, 20);
            lParams.rightMargin = Tools.dpToPx(context, 6);
        }
        else if (position == gaolAdvices.size() - 1) {
            lParams.leftMargin = Tools.dpToPx(context, 6);
            lParams.rightMargin = Tools.dpToPx(context, 20);
        }
        else {
            lParams.leftMargin = Tools.dpToPx(context, 6);
            lParams.rightMargin = Tools.dpToPx(context, 6);
        }
        holder.card.setLayoutParams(lParams);
        holder.title.setText(gaolAdvices.get(position).getTitle().toUpperCase());
        holder.heading.setText(gaolAdvices.get(position).getHeading());
        holder.description.setText(gaolAdvices.get(position).getDescription());
        holder.icon.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(), gaolAdvices.get(position).getIcon(), context.getTheme()));
        holder.topCardColor.setBackgroundTintList(ColorStateList.valueOf(gaolAdvices.get(position).getColor()));
        holder.card.setOnClickListener(e -> {
            // TODO add logic to apply advice
            System.out.println("advice clicked --> apply");
        });
    }

    @Override
    public int getItemCount() {
        return gaolAdvices.size();
    }

    public class MainViewHolderGoalAdvices extends RecyclerView.ViewHolder {
        TextView title, heading, description;
        MaterialCardView card;
        ImageView icon;
        View topCardColor;

        public MainViewHolderGoalAdvices(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.crd_adv_card);
            title = itemView.findViewById(R.id.txt_adv_title);
            heading = itemView.findViewById(R.id.txt_adv_heading);
            description = itemView.findViewById(R.id.txt_adv_description);
            icon = itemView.findViewById(R.id.img_adv_icon);
            topCardColor = itemView.findViewById(R.id.vw_adv_top_card_color);
        }
    }

    public interface OnEntryClicked {
        void onEvent(BinauralBeat binauralBeat, int position);
    }

    public void setOnEntryClickedListener(OnEntryClicked eventListener) {
        mListener = eventListener;
    }
}
