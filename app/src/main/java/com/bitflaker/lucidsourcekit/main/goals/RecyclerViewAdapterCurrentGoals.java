package com.bitflaker.lucidsourcekit.main.goals;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.goals.entities.Goal;
import com.bitflaker.lucidsourcekit.database.goals.entities.ShuffleTransaction;
import com.bitflaker.lucidsourcekit.databinding.EntryCurrentGoalBinding;
import com.bitflaker.lucidsourcekit.databinding.SheetGoalTransactionsBinding;
import com.bitflaker.lucidsourcekit.utils.Tools;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class RecyclerViewAdapterCurrentGoals extends RecyclerView.Adapter<RecyclerViewAdapterCurrentGoals.MainViewHolder> {
    private final Context context;
    private final List<Goal> goals;
    private final int shuffleId;
    private final MainDatabase db;
    private final HashMap<Integer, Integer> goalAchievedCount;
    private final ColorStateList colorNotAchieved, colorAchieved;

    public RecyclerViewAdapterCurrentGoals(Context context, List<Goal> goals, int shuffleId) {
        this.context = context;
        this.goals = goals;
        this.shuffleId = shuffleId;
        db = MainDatabase.getInstance(context);
        goalAchievedCount = db.getShuffleTransactionDao().getAllCountsFromShuffle(shuffleId)
                .blockingGet()
                .stream()
                .collect(Collectors.toMap(x -> x.goalId, x -> x.count, (prev, next) -> next, HashMap::new));
        colorNotAchieved = Tools.getAttrColorStateList(R.attr.colorSurfaceContainer, context.getTheme());
        colorAchieved = Tools.getAttrColorStateList(R.attr.colorTertiary, context.getTheme());
    }

    @NonNull
    @Override
    public RecyclerViewAdapterCurrentGoals.MainViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        return new RecyclerViewAdapterCurrentGoals.MainViewHolder(EntryCurrentGoalBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewAdapterCurrentGoals.MainViewHolder holder, int position) {
        Goal currentGoal = goals.get(position);
        holder.binding.txtGoal.setText(currentGoal.description);

        int achievedCount = goalAchievedCount.getOrDefault(currentGoal.goalId, 0);
        holder.binding.imgAchievedCounterBackground.setImageTintList(achievedCount == 0 ? colorNotAchieved : colorAchieved);
        holder.binding.txtAchievedCounter.setVisibility(achievedCount == 0 ? View.GONE : View.VISIBLE);
        holder.binding.txtAchievedCounterPlaceholder.setVisibility(achievedCount == 0 ? View.VISIBLE : View.GONE);
        holder.binding.txtAchievedCounter.setText(String.format(Locale.getDefault(), "%d", achievedCount));
        holder.binding.crdCurrentGoal.setOnClickListener(e -> {
            ShuffleTransaction transaction = new ShuffleTransaction(shuffleId, currentGoal.goalId, Calendar.getInstance().getTimeInMillis());
            db.getShuffleTransactionDao().insert(transaction).blockingSubscribe();
            int newCount = goalAchievedCount.getOrDefault(currentGoal.goalId, 0) + 1;
            goalAchievedCount.put(currentGoal.goalId, newCount);
            holder.binding.txtAchievedCounter.setText(String.format(Locale.getDefault(), "%d", newCount));
            if (achievedCount == 0) {
                notifyItemChanged(position);
            }
        });
        holder.binding.crdCurrentGoal.setOnLongClickListener(e -> {
            final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context, R.style.BottomSheetDialogStyle);
            SheetGoalTransactionsBinding sBinding = SheetGoalTransactionsBinding.inflate(LayoutInflater.from(context));
            bottomSheetDialog.setContentView(sBinding.getRoot());

            List<ShuffleTransaction> transactions = db.getShuffleTransactionDao().getAllFromShuffleGoal(shuffleId, currentGoal.goalId).blockingGet();
            RecyclerViewAdapterGoalTransactions manager = new RecyclerViewAdapterGoalTransactions(context, transactions);
            sBinding.rcvShuffleTransactions.setAdapter(manager);
            sBinding.rcvShuffleTransactions.setLayoutManager(new LinearLayoutManager(context));
            sBinding.txtGoal.setText(currentGoal.description);
            sBinding.txtNoneAchieved.setVisibility(transactions.isEmpty() ? View.VISIBLE : View.GONE);
            sBinding.rcvShuffleTransactions.setVisibility(transactions.isEmpty() ? View.GONE : View.VISIBLE);

            manager.setOnEntryDeletedListener(deleted -> {
                int newCount = goalAchievedCount.getOrDefault(currentGoal.goalId, 0) - 1;
                goalAchievedCount.put(currentGoal.goalId, newCount);
                notifyItemChanged(position);
                if (transactions.isEmpty()) {
                    sBinding.txtNoneAchieved.setVisibility(View.VISIBLE);
                }
            });

            bottomSheetDialog.show();
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return goals.size();
    }

    public static class MainViewHolder extends RecyclerView.ViewHolder {
        EntryCurrentGoalBinding binding;

        public MainViewHolder(@NonNull EntryCurrentGoalBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
