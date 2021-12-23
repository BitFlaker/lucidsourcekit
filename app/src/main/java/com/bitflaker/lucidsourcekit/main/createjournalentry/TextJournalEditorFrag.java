package com.bitflaker.lucidsourcekit.main.createjournalentry;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.Nullable;

import com.bitflaker.lucidsourcekit.R;

public class TextJournalEditorFrag extends Fragment {
    private String textToSetOnReady = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dream_text_story, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((EditText) getView().findViewById(R.id.txt_description_dream)).setText(textToSetOnReady);
    }

    public void setTextOnReady(String text) {
        textToSetOnReady = text;
    }
}
