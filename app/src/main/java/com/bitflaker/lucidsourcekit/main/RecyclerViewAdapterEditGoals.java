package com.bitflaker.lucidsourcekit.main;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.TextViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.database.goals.entities.Goal;
import com.bitflaker.lucidsourcekit.general.Tools;

import java.util.ArrayList;
import java.util.List;

public class RecyclerViewAdapterEditGoals extends RecyclerView.Adapter<RecyclerViewAdapterEditGoals.MainViewHolderGoals> {
    private OnEntryClicked mListener;
    private OnMultiselectEntered mListenerMultiSelectEnter;
    private OnMultiselectExited mListenerMultiSelectExit;
    private final Context context;
    private List<Goal> goals;
    private boolean isInSelectionMode = false;
    private int selectionCount = 0;
    private RecyclerView mRecyclerView;

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
        TextViewCompat.setCompoundDrawableTintList(holder.goalText, ColorStateList.valueOf(difficultyColor));
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
                        if(mListenerMultiSelectExit != null) { mListenerMultiSelectExit.onEvent(); }
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
                if(mListenerMultiSelectEnter != null) { mListenerMultiSelectEnter.onEvent(); }
            }
            return true;
        });
        if(goals.get(position).difficultyLocked){
            holder.difficultyLocked.setVisibility(View.VISIBLE);
        }
        else {
            holder.difficultyLocked.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return goals.size();
    }

    public void setEntries(List<Goal> goals) {
        isInSelectionMode = false;
        selectionCount = 0;
        int pos = 0;
        for (Goal goal : this.goals) {
            if(goal.isSelected) {
                goal.isSelected = false;
                notifyItemChanged(pos);
            }
            pos++;
        }
        if(mListenerMultiSelectExit != null) { mListenerMultiSelectExit.onEvent(); }
        Pair<RecyclerViewAdapterDreamJournal.Operation, Integer> changes = getChanges(goals);
        List<Integer> removedIndexes = getAllRemovedIndexes(goals);
        this.goals = goals;
        int index;
        switch (changes.first){
            case ADDED:
                index = changes.second;
                mRecyclerView.scrollToPosition(index);
                notifyItemInserted(index);
                notifyItemRangeChanged(index, this.goals.size());
                break;
            case DELETED:
                if(changes.second != -1){
                    index = changes.second;
                    notifyItemRemoved(index);
                    notifyItemRangeChanged(index, this.goals.size());
                }
                else {
                    int cntr = 0;
                    for (int i : removedIndexes) {
                        notifyItemRemoved(i - cntr);
                        cntr++;
                    }
                    notifyItemRangeChanged(removedIndexes.get(0), this.goals.size());
                }
                break;
            case CHANGED:
                // TODO: when timestamp changed and therefore entry moved in list => old one still there => duplicate entries
                // TODO: get index of changed item
                notifyDataSetChanged();
                break;
        }
    }

    private List<Integer> getAllRemovedIndexes(List<Goal> goals) {
        List<Integer> indexes = new ArrayList<>();
        int j = 0;
        for (int i = 0; i < this.goals.size(); i++) {
            if(j >= goals.size() || this.goals.get(i).goalId != goals.get(j).goalId) {
                indexes.add(i);
            }
            else {
                j++;
            }
        }
        return indexes;
    }

    private Pair<RecyclerViewAdapterDreamJournal.Operation, Integer> getChanges(List<Goal> goals) {
        if(this.goals.size() == goals.size()){
            // Operation has to either be a changed event or nothing
            return new Pair<>(RecyclerViewAdapterDreamJournal.Operation.CHANGED, -1);
        }
        else if(this.goals.size() < goals.size()) {
            for (int i = 0; i < goals.size(); i++) {
                if(this.goals.size() > i && this.goals.get(i).goalId != goals.get(i).goalId || this.goals.size() <= i) {
                    return new Pair<>(RecyclerViewAdapterDreamJournal.Operation.ADDED, i);
                }
            }
        }
        else if (this.goals.size() == goals.size() + 1) {
            for (int i = 0; i < this.goals.size(); i++) {
                if(goals.size() > i && this.goals.get(i).goalId != goals.get(i).goalId || goals.size() <= i) {
                    return new Pair<>(RecyclerViewAdapterDreamJournal.Operation.DELETED, i);
                }
            }
        }
        else {
            // TODO: multiple that were deleted
            //for (int i = 0; i < this.goals.size(); i++) {
            //    if(goals.size() > i && this.goals.get(i).goalId != goals.get(i).goalId || goals.size() <= i) {
                    return new Pair<>(RecyclerViewAdapterDreamJournal.Operation.DELETED, -1);
            //    }
            //}
        }

        return new Pair<>(RecyclerViewAdapterDreamJournal.Operation.NONE, -1);
    }

    public List<Integer> getSelectedGoalIds() {
        List<Integer> selectedGoalIds = new ArrayList<>();
        for (Goal goal : goals) {
            if (goal.isSelected) {
                selectedGoalIds.add(goal.goalId);
            }
        }
        return selectedGoalIds;
    }

    public List<Goal> getSelectedGoals() {
        List<Goal> selectedGoalIds = new ArrayList<>();
        for (Goal goal : goals) {
            if (goal.isSelected) {
                selectedGoalIds.add(goal);
            }
        }
        return selectedGoalIds;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mRecyclerView = recyclerView;
    }

    public class MainViewHolderGoals extends RecyclerView.ViewHolder {
        TextView goalText;
        ConstraintLayout container;
        ImageView difficultyLocked;
        boolean isSelected = false;

        public MainViewHolderGoals(@NonNull View itemView) {
            super(itemView);
            goalText = itemView.findViewById(R.id.txt_goal_text);
            container = itemView.findViewById(R.id.cl_goal_container);
            difficultyLocked = itemView.findViewById(R.id.img_difficulty_locked);
        }

        public void select() {
            isSelected = true;
            container.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.slightElevated, context.getTheme()));
            goalText.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_baseline_check_24, context.getTheme()), null, null, null);
        }

        public void unselect() {
            isSelected = false;
            container.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.backgroundColor, context.getTheme()));
            goalText.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_baseline_circle_24, context.getTheme()), null, null, null);
        }
    }

    public interface OnEntryClicked {
        void onEvent(Goal goal, int position);
    }

    public void setOnEntryClickedListener(OnEntryClicked eventListener) {
        mListener = eventListener;
    }

    public interface OnMultiselectEntered {
        void onEvent();
    }

    public void setOnMultiselectEntered(OnMultiselectEntered eventListener) {
        mListenerMultiSelectEnter = eventListener;
    }

    public interface OnMultiselectExited {
        void onEvent();
    }

    public void setOnMultiselectExited(OnMultiselectExited eventListener) {
        mListenerMultiSelectExit = eventListener;
    }
}
