package com.bitflaker.lucidsourcekit.main;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.database.goals.entities.Goal;
import com.bitflaker.lucidsourcekit.general.Tools;

import java.util.List;

public class RecyclerViewAdapterEditGoals extends RecyclerView.Adapter<RecyclerViewAdapterEditGoals.MainViewHolderGoals> {
    private OnEntryClicked mListener;
    private Context context;
    private List<Goal> goals;
    private boolean isInSelectionMode = false;
    private int selectionCount = 0;

    public RecyclerViewAdapterEditGoals(Context context, List<Goal> goals) {
        this.context = context;
        this.goals = goals;
    }

    @NonNull
    @Override
    public MainViewHolderGoals onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.goal_entry, parent, false);
        return new MainViewHolderGoals(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MainViewHolderGoals holder, int position) {
        holder.goalText.setText(goals.get(position).description);
        if(goals.get(position).isSelected && !holder.isSelected) {
            holder.select();
        }
        else if(!goals.get(position).isSelected && holder.isSelected) {
            holder.unselect();
        }
        @ColorInt int difficultyColor = Tools.getColorAtGradientPosition(goals.get(position).difficulty, 1, 3, Tools.getAttrColor(R.attr.colorSuccess, context.getTheme()), Tools.getAttrColor(R.attr.colorWarning, context.getTheme()), Tools.getAttrColor(R.attr.colorError, context.getTheme()));
        holder.difficulty.setCompoundDrawableTintList(ColorStateList.valueOf(difficultyColor));
        holder.container.setOnClickListener(e -> {
            if(!isInSelectionMode){
                if(mListener != null) {
                    mListener.onEvent(goals.get(position), position);
                }
            }
            else {
                if(!holder.isSelected) {
                    goals.get(position).isSelected = true;
                    holder.select();
                    selectionCount++;
                }
                else {
                    goals.get(position).isSelected = false;
                    holder.unselect();
                    selectionCount--;
                    if(selectionCount == 0) {
                        isInSelectionMode = false;
                    }
                }
            }
        });
        holder.container.setOnLongClickListener(view -> {
            if(!holder.isSelected) {
                // TODO: raise event so in lister the add button can be exchanged to a delete button (and colored red)
                isInSelectionMode = true;
                goals.get(position).isSelected = true;
                holder.select();
                selectionCount++;
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return goals.size();
    }

    public class MainViewHolderGoals extends RecyclerView.ViewHolder {
        TextView goalText, difficulty;
        ConstraintLayout container;
        boolean isSelected = false;

        public MainViewHolderGoals(@NonNull View itemView) {
            super(itemView);
            goalText = itemView.findViewById(R.id.txt_goal_text);
            difficulty = itemView.findViewById(R.id.txt_goal_difficulty);
            container = itemView.findViewById(R.id.cl_goal_container);
        }

        public void select() {
            isSelected = true;
            container.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.slightElevated, context.getTheme()));
            difficulty.setCompoundDrawablesWithIntrinsicBounds(context.getResources().getDrawable(R.drawable.ic_baseline_check_24, context.getTheme()), null, null, null);
        }

        public void unselect() {
            isSelected = false;
            container.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.backgroundColor, context.getTheme()));
            difficulty.setCompoundDrawablesWithIntrinsicBounds(context.getResources().getDrawable(R.drawable.ic_baseline_circle_24, context.getTheme()), null, null, null);
        }
    }

    public interface OnEntryClicked {
        void onEvent(Goal goal, int position);
    }

    public void setOnEntryClickedListener(OnEntryClicked eventListener) {
        mListener = eventListener;
    }
}
