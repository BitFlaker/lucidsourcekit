package com.bitflaker.lucidsourcekit.main.dreamjournal;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.bitflaker.lucidsourcekit.R;
import com.google.android.material.button.MaterialButton;

public class DreamJournalContentEditor extends Fragment {
    private OnContinueButtonClicked mContinueButtonClicked;
    private ConstraintLayout topHeading;
    private EditText title, description;
    private ScrollView editorScroller;
    private ImageButton continueButton;
    private MaterialButton dateTime;
    private JournalInMemoryManager journalManger;
    private String journalEntryId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_dream_journal_content_editor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
//        Tools.makeStatusBarTransparent(DreamJournalEditor.this);

        topHeading = getView().findViewById(R.id.csl_dj_top_bar);
        title = getView().findViewById(R.id.txt_dj_title_dream);
        description = getView().findViewById(R.id.txt_dj_description_dream);
        editorScroller = getView().findViewById(R.id.scrl_editor_scroll);
        continueButton = getView().findViewById(R.id.btn_dj_continue_to_ratings);
        dateTime = getView().findViewById(R.id.btn_dj_date);

//        RelativeLayout.LayoutParams lParams = (RelativeLayout.LayoutParams) editorScroller.getLayoutParams();
//        lParams.setMargins(0, Tools.getStatusBarHeight(this), 0, 0);
//        editorScroller.setLayoutParams(lParams);

        dateTime.setOnClickListener(e -> {

        });

        continueButton.setOnClickListener(e -> {
            if(mContinueButtonClicked != null){
                mContinueButtonClicked.onEvent();
            }
        });

        title.requestFocus();
        // TODO: description has to lose focus and then clicked in again in order to fully get rid of the edittext scrolling => CHANGE!
    }

    public void setJournalEntryId(String id) {
        journalEntryId = id;
        journalManger = JournalInMemoryManager.getInstance();
    }

    public interface OnContinueButtonClicked {
        void onEvent();
    }

    public void setOnContinueButtonClicked(OnContinueButtonClicked eventListener) {
        mContinueButtonClicked = eventListener;
    }
}