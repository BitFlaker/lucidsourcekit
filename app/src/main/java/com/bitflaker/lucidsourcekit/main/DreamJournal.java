package com.bitflaker.lucidsourcekit.main;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.general.DatabaseWrapper;
import com.bitflaker.lucidsourcekit.general.JournalTypes;
import com.bitflaker.lucidsourcekit.general.database.values.DreamJournalEntriesList;
import com.bitflaker.lucidsourcekit.main.createjournalentry.AddTextEntry;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class DreamJournal extends Fragment {

    RecyclerView recyclerView;
    String[][] tags;
    int[] moods, sleepQualities, sleepTypes;
    private TextView noEntryFound;
    private FloatingActionButton fabAdd, fabText, fabForms, fabAudio;
    private Animation fabOpen, fabClose, rotateForward, rotateBackward;
    private boolean isOpen = false;
    private DatabaseWrapper dbWrapper;
    private RecyclerViewAdapter recyclerViewAdapter;
    private ActivityResultLauncher<Intent> someActivityResultLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        someActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        String selectedDate = data.getStringExtra("date");
                        String selectedTime = data.getStringExtra("time");
                        String title = data.getStringExtra("title");
                        String description = data.getStringExtra("description");
                        String quality = data.getStringExtra("quality");
                        String clarity = data.getStringExtra("clarity");
                        String mood = data.getStringExtra("mood");
                        String[] dreamTypes = data.getStringArrayExtra("dreamTypes");
                        String[] tags = data.getStringArrayExtra("tags");
                        String[] recordedAudios = data.getStringArrayExtra("recordings");
                        recyclerViewAdapter.addEntry(selectedDate, selectedTime, title, description, tags, quality, clarity, mood, dreamTypes, recordedAudios);
                        recyclerViewAdapter.notifyItemInserted(0);
                        recyclerView.scrollToPosition(0);
                    }
                });
        return inflater.inflate(R.layout.fragment_dream_journal, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        noEntryFound = getView().findViewById(R.id.txt_no_entries);
        noEntryFound.setText(Html.fromHtml("<span><big><big><strong>Uhh...</strong></big></big><br />" + getContext().getResources().getString(R.string.empty_dream_journal) + "</span>", Html.FROM_HTML_MODE_COMPACT));

        recyclerView = getView().findViewById(R.id.recycler_view);
        dbWrapper = new DatabaseWrapper(getContext());

        DreamJournalEntriesList entries = dbWrapper.getJournalEntries();

        // TODO desc can be null => Audio recording
        recyclerViewAdapter = new RecyclerViewAdapter(getContext(), entries.getDates(), entries.getTimes(), entries.getTitles(), entries.getDescriptions(), entries.getTags(), entries.getSleepQualities(), entries.getDreamClarities(), entries.getDreamMoods(), entries.getTypes(), entries.getAudioLocations());
        recyclerView.setAdapter(recyclerViewAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        fabAdd = (FloatingActionButton) getView().findViewById(R.id.btn_add_journal_entry);
        fabText = (FloatingActionButton) getView().findViewById(R.id.fab_text);
        fabAudio = (FloatingActionButton) getView().findViewById(R.id.fab_audio);
        fabForms = (FloatingActionButton) getView().findViewById(R.id.fab_forms);

        fabOpen = AnimationUtils.loadAnimation(getContext(), R.anim.add_open);
        fabClose = AnimationUtils.loadAnimation(getContext(),R.anim.add_close);
        rotateForward = AnimationUtils.loadAnimation(getContext(),R.anim.rotate_forward);
        rotateBackward = AnimationUtils.loadAnimation(getContext(),R.anim.rotate_backward);

        fabAdd.setOnClickListener(view13 -> animateFab());
        fabText.setOnClickListener(view12 -> {
            showJournalCreator(JournalTypes.Text);
        });
        fabAudio.setOnClickListener(view1 -> {
            showJournalCreator(JournalTypes.Audio);
        });
        fabForms.setOnClickListener(view1 -> {
            showJournalCreator(JournalTypes.Forms);
        });
    }

    private void showJournalCreator(JournalTypes forms) {
        animateFab();
        Intent intent = new Intent(getContext(), AddTextEntry.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("type", forms.ordinal());
        someActivityResultLauncher.launch(intent);
    }

    private void animateFab(){
        if (isOpen){
            fabAdd.startAnimation(rotateForward);
            fabText.startAnimation(fabClose);
            fabAudio.startAnimation(fabClose);
            fabForms.startAnimation(fabClose);
            fabText.setClickable(false);
            fabAudio.setClickable(false);
            fabForms.setClickable(false);
            isOpen=false;
        }
        else {
            fabAdd.startAnimation(rotateBackward);
            fabText.startAnimation(fabOpen);
            fabAudio.startAnimation(fabOpen);
            fabForms.startAnimation(fabOpen);
            fabText.setClickable(true);
            fabAudio.setClickable(true);
            fabForms.setClickable(true);
            isOpen=true;
        }
    }

    public void pageChanged() {
        isOpen = false;
    }
}