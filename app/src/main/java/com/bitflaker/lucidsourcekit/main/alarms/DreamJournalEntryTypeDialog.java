package com.bitflaker.lucidsourcekit.main.alarms;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.resulttables.DreamJournalEntry;
import com.google.android.material.button.MaterialButton;

public class DreamJournalEntryTypeDialog extends Dialog implements View.OnClickListener {
    private OnEntryTypeSelected mListener;

    public DreamJournalEntryTypeDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_journal_type);
        MaterialButton textEntry = findViewById(R.id.btn_text_entry);
        MaterialButton formsEntry = findViewById(R.id.btn_forms_entry);
        textEntry.setOnClickListener(e -> {
            if (mListener != null) {
                mListener.onEvent(DreamJournalEntry.EntryType.PLAIN_TEXT);
            }
            dismiss();
        });
        formsEntry.setOnClickListener(e -> {
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
