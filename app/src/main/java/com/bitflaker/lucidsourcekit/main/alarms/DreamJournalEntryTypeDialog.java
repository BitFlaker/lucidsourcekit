package com.bitflaker.lucidsourcekit.main.alarms;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;

import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.resulttables.DreamJournalEntry;
import com.bitflaker.lucidsourcekit.databinding.DialogJournalTypeBinding;

public class DreamJournalEntryTypeDialog extends Dialog implements View.OnClickListener {
    private OnEntryTypeSelected mListener;
    private DialogJournalTypeBinding binding;

    public DreamJournalEntryTypeDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        binding = DialogJournalTypeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnTextEntry.setOnClickListener(e -> {
            if (mListener != null) {
                mListener.onEvent(DreamJournalEntry.EntryType.PLAIN_TEXT);
            }
            dismiss();
        });
        binding.btnFormsEntry.setOnClickListener(e -> {
            if (mListener != null) {
                mListener.onEvent(DreamJournalEntry.EntryType.FORMS_TEXT);
            }
            dismiss();
        });
    }

    @Override
    public void onClick(View v) {
        dismiss();
    }

    public interface OnEntryTypeSelected {
        void onEvent(DreamJournalEntry.EntryType journalType);
    }

    public void setOnEntryTypeSelected(OnEntryTypeSelected listener) {
        this.mListener = listener;
    }
}
