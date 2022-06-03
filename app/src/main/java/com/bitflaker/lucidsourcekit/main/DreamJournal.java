package com.bitflaker.lucidsourcekit.main;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntry;
import com.bitflaker.lucidsourcekit.general.JournalTypes;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.bitflaker.lucidsourcekit.general.database.values.DreamJournalEntriesList;
import com.bitflaker.lucidsourcekit.general.database.values.DreamJournalEntry;
import com.bitflaker.lucidsourcekit.main.dreamjournal.DreamJournalEntryEditor;
import com.bitflaker.lucidsourcekit.main.dreamjournal.JournalInMemory;
import com.bitflaker.lucidsourcekit.main.dreamjournal.JournalInMemoryManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class DreamJournal extends Fragment {

    RecyclerView recyclerView;
    private TextView noEntryFound;
    private FloatingActionButton fabAdd, fabText, fabForms;
    private Animation fabOpen, fabClose, rotateForward, rotateBackward;
    private boolean isOpen = false;
    private RecyclerViewAdapterDreamJournal recyclerViewAdapterDreamJournal = null;
    public ActivityResultLauncher<Intent> viewEntryActivityResultLauncher;
    public ActivityResultLauncher<Intent> dreamJournalEditorActivityResultLauncher;
    private ImageButton sortEntries, filterEntries, resetFilterEntries;
    private int sortBy = 0;
    private MainDatabase db;

    // TODO: new audio entry added => displaying 0 audio files and does not display them, but when reloaded, it shows correct values
    // same goes for tags => queried too fast => entry added but tags and audio files not !

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setupViewResultLauncher();
        return inflater.inflate(R.layout.fragment_dream_journal, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getView().findViewById(R.id.ll_header).setLayoutParams(Tools.getRelativeLayoutParamsTopStatusbar(getContext()));

        db = MainDatabase.getInstance(getContext());

        Context context = getContext();

        db.getJournalEntryDao().getAll().subscribe(journalEntries -> {
            // TODO gather all data in a more efficient way!
            DreamJournalEntriesList djel = new DreamJournalEntriesList();
            for (JournalEntry entry : journalEntries) {
                db.getJournalEntryHasTagDao().getAllFromEntryId(entry.entryId).subscribe((assignedTags, throwable1) -> {
                    db.getJournalEntryIsTypeDao().getAllFromEntryId(entry.entryId).subscribe((journalEntryHasTypes, throwable2) -> {
                        db.getAudioLocationDao().getAllFromEntryId(entry.entryId).subscribe((audioLocations, throwable3) -> {
                            DreamJournalEntry djEntry = new DreamJournalEntry(entry, assignedTags, journalEntryHasTypes, audioLocations);
                            djel.add(djEntry);
                        });
                    });
                });
            }

            Handler mainHandler = new Handler(context.getMainLooper());
            Runnable myRunnable = () -> {
                if(setData(djel)) {
                    setBasics();
                    setupFAB();
                    setupSortButton();
                    setupFilterButton();
                    setupResetFilterButton();
                }
                // TODO: get changed element and make precise notify and not just reload the whole dataset
                //recyclerViewAdapterDreamJournal.notifyDataSetChanged();
                checkForEntries();
            };
            mainHandler.post(myRunnable);
        });
    }

    private void setBasics() {
        noEntryFound.setText(Html.fromHtml("<span><big><big><strong>Uhh...</strong></big></big><br />" + getContext().getResources().getString(R.string.empty_dream_journal) + "</span>", Html.FROM_HTML_MODE_COMPACT));
        recyclerView.setAdapter(recyclerViewAdapterDreamJournal);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    private void setupResetFilterButton() {
        resetFilterEntries.setOnClickListener(e -> {
            resetFilterEntries.setVisibility(View.GONE);
            recyclerViewAdapterDreamJournal.resetFilters();
            checkForEntries();
        });
    }

    private void setupFilterButton() {
        filterEntries.setOnClickListener(e -> {
            // TODO start loading animation
            db.getJournalEntryTagDao().getAll().subscribe((journalEntryTags, throwable) -> {
                String[] availableTags = new String[journalEntryTags.size()];
                for (int i = 0; i < journalEntryTags.size(); i++) {
                    availableTags[i] = journalEntryTags.get(i).description;
                }

                FilterDialog fd = new FilterDialog(getActivity(), availableTags, recyclerViewAdapterDreamJournal.getCurrentFilter());
                fd.setOnClickPositiveButton(g -> {
                    AppliedFilter af = fd.getFilters();
                    if(!AppliedFilter.isEmptyFilter(af)){
                        recyclerViewAdapterDreamJournal.filter(af);
                        resetFilterEntries.setVisibility(View.VISIBLE);
                    }
                    else if (resetFilterEntries.getVisibility() == View.VISIBLE){
                        resetFilterEntries.setVisibility(View.GONE);
                        recyclerViewAdapterDreamJournal.resetFilters();
                    }
                    checkForEntries();
                    fd.dismiss();
                });
                fd.show();
            });
        });
    }

    private void setupSortButton() {
        sortEntries.setOnClickListener(e -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), Tools.getThemeDialog());
            builder.setTitle("Sort entries by");
            String[] sortOrder = {"timestamp - newest first", "timestamp - oldest first", "title - A to Z", "title - Z to A", "description - A to Z", "description - Z to A"};
            builder.setSingleChoiceItems(sortOrder, sortBy, (dialog, which) -> {
                sortBy = which;
                recyclerViewAdapterDreamJournal.sortEntries(sortBy);
                dialog.dismiss();
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        });
    }

    private void setupFAB() {
        fabAdd.setOnClickListener(view13 -> animateFab());
        fabText.setOnClickListener(view12 -> showJournalCreator(JournalTypes.Text));
//        fabAudio.setOnClickListener(view1 -> showJournalCreator(JournalTypes.Audio));
        fabForms.setOnClickListener(view1 -> showJournalCreator(JournalTypes.Forms));
    }

    private boolean setData(DreamJournalEntriesList entries) {
        boolean isFirstInit = false;
        if(recyclerViewAdapterDreamJournal == null){
            isFirstInit = true;
            fabAdd = getView().findViewById(R.id.btn_add_journal_entry);
            fabText = getView().findViewById(R.id.fab_text);
//            fabAudio = getView().findViewById(R.id.fab_audio);
            fabForms = getView().findViewById(R.id.fab_forms);
            sortEntries = getView().findViewById(R.id.btn_sort);
            filterEntries = getView().findViewById(R.id.btn_filter);
            resetFilterEntries = getView().findViewById(R.id.btn_filter_off);
            fabOpen = AnimationUtils.loadAnimation(getContext(), R.anim.add_open);
            fabClose = AnimationUtils.loadAnimation(getContext(),R.anim.add_close);
            rotateForward = AnimationUtils.loadAnimation(getContext(),R.anim.rotate_forward);
            rotateBackward = AnimationUtils.loadAnimation(getContext(),R.anim.rotate_backward);
            noEntryFound = getView().findViewById(R.id.txt_no_entries);
            recyclerView = getView().findViewById(R.id.recycler_view);
            recyclerViewAdapterDreamJournal = new RecyclerViewAdapterDreamJournal(this, getContext(), entries);
        }
        else {
            recyclerViewAdapterDreamJournal.setEntries(entries);
        }
        return isFirstInit;
    }

    private void checkForEntries() {
        if (recyclerViewAdapterDreamJournal.getItemCount() == 0) {
            noEntryFound.setVisibility(View.VISIBLE);
        }
        else if (noEntryFound.getVisibility() == View.VISIBLE){
            noEntryFound.setVisibility(View.GONE);
        }
    }

    private void showJournalCreator(JournalTypes type) {
        // TODO migrate available tags from extra to load in journal entry creator!
        // TODO start loading animation
        animateFab();
        Intent intent = new Intent(getContext(), DreamJournalEntryEditor.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("type", type.ordinal());
        dreamJournalEditorActivityResultLauncher.launch(intent);
    }

    private void animateFab(){
        if (isOpen){
            fabAdd.startAnimation(rotateForward);
            fabText.startAnimation(fabClose);
            fabForms.startAnimation(fabClose);
            fabText.setClickable(false);
            fabForms.setClickable(false);
            isOpen=false;
        }
        else {
            fabAdd.startAnimation(rotateBackward);
            fabText.startAnimation(fabOpen);
            fabForms.startAnimation(fabOpen);
            fabText.setClickable(true);
            fabForms.setClickable(true);
            isOpen=true;
        }
    }

    public void pageChanged() {
        isOpen = false;
    }

    private void setupViewResultLauncher() {
        viewEntryActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        String id = data.getStringExtra("journal_in_memory_id");
                        JournalInMemory jim = JournalInMemoryManager.getInstance().getEntry(id);
                        reloadEntryData(jim.getEntryId());
                    }
                });
        dreamJournalEditorActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();

                        if(data.hasExtra("entryId")) {
                            int entryId = data.getIntExtra("entryId", -1);
                            if(entryId != -1) {
                                reloadEntryData(entryId);
                            }
                        }
                    }
                });
    }

    private void reloadEntryData(int entryId) {
        db.getJournalEntryHasTagDao().getAllFromEntryId(entryId).subscribe((assignedTags, throwable1) -> {
            db.getJournalEntryIsTypeDao().getAllFromEntryId(entryId).subscribe((journalEntryHasTypes, throwable2) -> {
                db.getAudioLocationDao().getAllFromEntryId(entryId).subscribe((audioLocations, throwable3) -> {
                    recyclerViewAdapterDreamJournal.updateDataForEntry(entryId, assignedTags, journalEntryHasTypes, audioLocations);
                });
            });
        });
    }
}