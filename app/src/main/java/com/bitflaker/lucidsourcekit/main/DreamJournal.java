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
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.AudioLocation;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntry;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntryHasType;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.resulttables.AssignedTags;
import com.bitflaker.lucidsourcekit.general.JournalTypes;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.bitflaker.lucidsourcekit.general.database.values.DreamJournalEntriesList;
import com.bitflaker.lucidsourcekit.general.database.values.DreamJournalEntry;
import com.bitflaker.lucidsourcekit.main.dreamjournal.DreamJournalEntryEditor;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class DreamJournal extends Fragment {

    private RecyclerView recyclerView;
    private TextView noEntryFound;
    private FloatingActionButton fabAdd, fabText, fabForms;
    private Animation fabOpen, fabClose, rotateForward, rotateBackward;
    private boolean isOpen = false;
    private RecyclerViewAdapterDreamJournal recyclerViewAdapterDreamJournal = null;
    public ActivityResultLauncher<Intent> journalEditorActivityResultLauncher;
    private ImageButton sortEntries, filterEntries, resetFilterEntries;
    private int sortBy = 0;
    private MainDatabase db;
    private DreamJournalEntriesList djel = new DreamJournalEntriesList();
    @Nullable
    private JournalTypes autoOpenJournalTypeCreator = null;
    private CompositeDisposable compositeDisposable;

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
        compositeDisposable = new CompositeDisposable();

        getView().findViewById(R.id.ll_header).setLayoutParams(Tools.getRelativeLayoutParamsTopStatusbar(getContext()));
        fabAdd = getView().findViewById(R.id.btn_add_journal_entry);
        fabText = getView().findViewById(R.id.fab_text);
//        fabAudio = getView().findViewById(R.id.fab_audio);
        fabForms = getView().findViewById(R.id.fab_forms);
        sortEntries = getView().findViewById(R.id.btn_sort);
        filterEntries = getView().findViewById(R.id.btn_filter);
        resetFilterEntries = getView().findViewById(R.id.btn_filter_off);
        noEntryFound = getView().findViewById(R.id.txt_no_entries);
        recyclerView = getView().findViewById(R.id.recycler_view);

        fabOpen = AnimationUtils.loadAnimation(getContext(), R.anim.add_open);
        fabClose = AnimationUtils.loadAnimation(getContext(),R.anim.add_close);
        rotateForward = AnimationUtils.loadAnimation(getContext(),R.anim.rotate_forward);
        rotateBackward = AnimationUtils.loadAnimation(getContext(),R.anim.rotate_backward);

        db = MainDatabase.getInstance(getContext());

        compositeDisposable.add(db.getJournalEntryDao().getAll().subscribe(journalEntries -> {
            compositeDisposable.add(loadAllJournalData(journalEntries)
                    .subscribeOn(Schedulers.io())
                    .subscribe(entries -> getActivity().runOnUiThread(() -> {
                        recyclerViewAdapterDreamJournal = new RecyclerViewAdapterDreamJournal(this, getActivity(), getContext(), entries);
                        setBasics();
                        setupFAB();
                        setupSortButton();
                        setupFilterButton();
                        setupResetFilterButton();
                        checkForEntries();
                    })));
                }));
        if(autoOpenJournalTypeCreator != null) {
            // TODO when an entry was created after the editor was opened by the alarm quick action, the list of entries in the MainViewer does not get updated
            showJournalCreator(autoOpenJournalTypeCreator);
            autoOpenJournalTypeCreator = null;
        }
    }

    private Single<DreamJournalEntriesList> loadAllJournalData(List<JournalEntry> journalEntries) {
        return Single.fromCallable(() -> {
            DreamJournalEntriesList entries = new DreamJournalEntriesList();
            for (JournalEntry entry : journalEntries) {
                List<AssignedTags> assignedTags = db.getJournalEntryHasTagDao().getAllFromEntryId(entry.entryId).blockingGet();
                List<JournalEntryHasType> journalEntryHasTypes = db.getJournalEntryIsTypeDao().getAllFromEntryId(entry.entryId).blockingGet();
                List<AudioLocation> audioLocations = db.getAudioLocationDao().getAllFromEntryId(entry.entryId).blockingGet();
                DreamJournalEntry djEntry = new DreamJournalEntry(entry, assignedTags, journalEntryHasTypes, audioLocations);
                entries.add(djEntry);
            }
            return entries;
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
        if(recyclerViewAdapterDreamJournal == null) {
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
            recyclerViewAdapterDreamJournal = new RecyclerViewAdapterDreamJournal(this, getActivity(), getContext(), entries);
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

    public void showJournalCreator(JournalTypes type) {
        // TODO start loading animation
        animateFab();
        Intent intent = new Intent(requireContext(), DreamJournalEntryEditor.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("type", type.ordinal());
        journalEditorActivityResultLauncher.launch(intent);
    }

    private void animateFab(){
        @ColorInt int colorPrimary = Tools.getAttrColor(R.attr.colorPrimary, getContext().getTheme());
        @ColorInt int colorSlightElevated = Tools.getAttrColor(R.attr.slightElevated2x, getContext().getTheme());

        if (fabAdd == null) { return; }
        if (isOpen) {
            fabAdd.startAnimation(rotateForward);
            fabText.startAnimation(fabClose);
            fabForms.startAnimation(fabClose);
            Tools.animateBackgroundTint(fabAdd, colorSlightElevated, colorPrimary, 300);
            fabText.setClickable(false);
            fabForms.setClickable(false);
            isOpen=false;
        }
        else {
            fabAdd.startAnimation(rotateBackward);
            fabText.startAnimation(fabOpen);
            fabForms.startAnimation(fabOpen);
            Tools.animateBackgroundTint(fabAdd, colorPrimary, colorSlightElevated, 300);
            fabText.setClickable(true);
            fabForms.setClickable(true);
            isOpen=true;
        }
    }

    public void pageChanged() {
        isOpen = false;
    }

    private void setupViewResultLauncher() {
        journalEditorActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if(data != null && data.hasExtra("entryId")) {
                            int entryId = data.getIntExtra("entryId", -1);
                            if(entryId != -1) {
                                // TODO: make async in future
                                reloadEntryData(entryId);
                            }
                        }
                    }
                });
    }

    private void reloadEntryData(int entryId) {
        JournalEntry entry = db.getJournalEntryDao().getEntryById(entryId).blockingGet();
        List<AssignedTags> assignedTags = db.getJournalEntryHasTagDao().getAllFromEntryId(entryId).blockingGet();
        List<JournalEntryHasType> journalEntryHasTypes = db.getJournalEntryIsTypeDao().getAllFromEntryId(entryId).blockingGet();
        List<AudioLocation> audioLocations = db.getAudioLocationDao().getAllFromEntryId(entryId).blockingGet();
        recyclerViewAdapterDreamJournal.updateDataForEntry(entry, assignedTags, journalEntryHasTypes, audioLocations);
    }

    public void showJournalCreatorWhenLoaded(@Nullable JournalTypes type) {
        this.autoOpenJournalTypeCreator = type;
    }

    @Override
    public void onDestroyView() {
        compositeDisposable.dispose();
        super.onDestroyView();
    }
}