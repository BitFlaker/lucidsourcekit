package com.bitflaker.lucidsourcekit.main.goals;

import android.app.TimePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.goals.entities.ShuffleTransaction;
import com.bitflaker.lucidsourcekit.databinding.EntryGoalTransactionBinding;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class RecyclerViewAdapterGoalTransactions extends RecyclerView.Adapter<RecyclerViewAdapterGoalTransactions.MainViewHolder> {
    private final Context context;
    private final List<ShuffleTransaction> shuffleTransactions;
    private final MainDatabase db;
    private final DateFormat formatter = DateFormat.getTimeInstance(DateFormat.SHORT);
    private OnEntryDeleted mDeletedListener;

    public RecyclerViewAdapterGoalTransactions(Context context, List<ShuffleTransaction> shuffleTransactions) {
        this.context = context;
        this.shuffleTransactions = shuffleTransactions;
        db = MainDatabase.getInstance(context);
    }

    @NonNull
    @Override
    public RecyclerViewAdapterGoalTransactions.MainViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        return new RecyclerViewAdapterGoalTransactions.MainViewHolder(EntryGoalTransactionBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewAdapterGoalTransactions.MainViewHolder holder, int position) {
        ShuffleTransaction currentTransaction = shuffleTransactions.get(position);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(currentTransaction.achievedAt);
        holder.binding.txtTransactionCount.setText(String.format(Locale.getDefault(), "%d", position + 1));
        holder.binding.btnGoalAchievedTime.setText(formatter.format(calendar.getTime()));
        holder.binding.btnGoalAchievedTime.setOnClickListener(e -> new TimePickerDialog(context, R.style.Theme_LucidSourceKit_TimePickerDialog, (timePickerFrom, hourFrom, minuteFrom) -> {
            calendar.set(Calendar.HOUR_OF_DAY, hourFrom);
            calendar.set(Calendar.MINUTE, minuteFrom);
            holder.binding.btnGoalAchievedTime.setText(formatter.format(calendar.getTime()));
            currentTransaction.achievedAt = calendar.getTimeInMillis();
            db.getShuffleTransactionDao().update(currentTransaction).blockingSubscribe();
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show());
        holder.binding.btnDeleteGoalTransaction.setOnClickListener(e -> {
            db.getShuffleTransactionDao().delete(currentTransaction).blockingSubscribe();
            shuffleTransactions.remove(currentTransaction);
            notifyItemRemoved(position);
            notifyItemRangeChanged(0, getItemCount());
            if (mDeletedListener != null) { mDeletedListener.onDelete(currentTransaction); }
        });
    }

    public interface OnEntryDeleted {
        void onDelete(ShuffleTransaction transaction);
    }

    public void setOnEntryDeletedListener(OnEntryDeleted listener) {
        mDeletedListener = listener;
    }

    @Override
    public int getItemCount() {
        return shuffleTransactions.size();
    }

    public static class MainViewHolder extends RecyclerView.ViewHolder {
        EntryGoalTransactionBinding binding;

        public MainViewHolder(@NonNull EntryGoalTransactionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
