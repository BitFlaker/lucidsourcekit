package com.bitflaker.lucidsourcekit.alarms;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.general.JournalTypes;
import com.google.android.material.button.MaterialButton;

public class DreamJournalEntryTypeDialog extends Dialog implements View.OnClickListener {
    private MaterialButton formsEntry, textEntry;
    private OnEntryTypeSelected mListener;

    public DreamJournalEntryTypeDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_journal_type);
        textEntry = findViewById(R.id.btn_text_entry);
        formsEntry = findViewById(R.id.btn_forms_entry);
        textEntry.setOnClickListener(e -> {
            if(mListener != null){
                mListener.onEvent(JournalTypes.Text);
            }
            dismiss();
        });
        formsEntry.setOnClickListener(e -> {
            if(mListener != null){
                mListener.onEvent(JournalTypes.Forms);
            }
            dismiss();
        });
    }

    @Override
    public void onClick(View v) {
        dismiss();
    }

    public interface OnEntryTypeSelected {
        void onEvent(JournalTypes journalType);
    }

    public void setOnEntryTypeSelected(OnEntryTypeSelected listener) {
        this.mListener = listener;
    }
}
