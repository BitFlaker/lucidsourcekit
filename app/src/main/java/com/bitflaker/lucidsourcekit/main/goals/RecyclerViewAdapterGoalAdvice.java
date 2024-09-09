package com.bitflaker.lucidsourcekit.main.goals;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.data.records.GoalAdvice;
import com.bitflaker.lucidsourcekit.databinding.EntryGoalQuickAdviceBinding;

import java.util.List;

public class RecyclerViewAdapterGoalAdvice extends RecyclerView.Adapter<RecyclerViewAdapterGoalAdvice.MainViewHolderGoalAdvices> {
    private final Context context;
    private final List<GoalAdvice> goalAdvices;

    public RecyclerViewAdapterGoalAdvice(Context context, List<GoalAdvice> goalAdvices) {
        this.context = context;
        this.goalAdvices = goalAdvices;
    }

    @NonNull
    @Override
    public MainViewHolderGoalAdvices onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        return new MainViewHolderGoalAdvices(EntryGoalQuickAdviceBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MainViewHolderGoalAdvices holder, int position) {
        GoalAdvice goalAdvice = goalAdvices.get(position);
        RecyclerView.LayoutParams lParams = (RecyclerView.LayoutParams) holder.binding.crdAdvCard.getLayoutParams();
        holder.binding.crdAdvCard.setLayoutParams(lParams);
        holder.binding.txtAdvHeading.setText(goalAdvice.heading());
        holder.binding.imgAdvIcon.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(), goalAdvice.icon(), context.getTheme()));
        holder.binding.crdAdvCard.setOnClickListener(e -> goalAdvice.onAdviceSelectedListener().adviceSelected(goalAdvice));
    }

    @Override
    public int getItemCount() {
        return goalAdvices.size();
    }

    public static class MainViewHolderGoalAdvices extends RecyclerView.ViewHolder {
        EntryGoalQuickAdviceBinding binding;

        public MainViewHolderGoalAdvices(@NonNull EntryGoalQuickAdviceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
