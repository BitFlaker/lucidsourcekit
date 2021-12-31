package com.bitflaker.lucidsourcekit.main;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.general.Tools;

import java.util.List;

public class RecyclerViewAdapterEditGoals extends RecyclerView.Adapter<RecyclerViewAdapterEditGoals.MainViewHolder> {
    private Context context;
    private List<Goal> goals;

    public RecyclerViewAdapterEditGoals(Context context, List<Goal> goals) {
        this.context = context;
        this.goals = goals;
    }

    @NonNull
    @Override
    public MainViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.journal_entry, parent, false);
        return new MainViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MainViewHolder holder, int position) {
        holder.goalText.setText(goals.get(position).getName());
        holder.difficulty.setCompoundDrawablesWithIntrinsicBounds(context.getResources().getDrawable(R.drawable.ic_baseline_priority_high_24, context.getTheme()), null, null, null);
        switch (goals.get(position).getDifficulty()){
            case Easy:
                holder.difficulty.setCompoundDrawableTintList(Tools.getAttrColorStateList(R.attr.colorSuccess, context.getTheme()));
                break;
            case Moderate:
                holder.difficulty.setCompoundDrawableTintList(Tools.getAttrColorStateList(R.attr.colorWarning, context.getTheme()));
                break;
            case Difficult:
                holder.difficulty.setCompoundDrawableTintList(Tools.getAttrColorStateList(R.attr.colorError, context.getTheme()));
                break;
        }
    }

    @Override
    public int getItemCount() {
        return goals.size();
    }

    public class MainViewHolder extends RecyclerView.ViewHolder{
        TextView goalText, difficulty;

        public MainViewHolder(@NonNull View itemView) {
            super(itemView);
            goalText = itemView.findViewById(R.id.txt_goal_text);
            difficulty = itemView.findViewById(R.id.txt_goal_difficulty);
        }
    }
}
