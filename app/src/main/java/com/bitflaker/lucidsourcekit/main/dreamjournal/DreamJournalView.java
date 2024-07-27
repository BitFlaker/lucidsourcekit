package com.bitflaker.lucidsourcekit.main.dreamjournal;

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
import com.bitflaker.lucidsourcekit.data.records.AppliedFilter;
import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.resulttables.DreamJournalEntry;
import com.bitflaker.lucidsourcekit.data.enums.SortBy;
import com.bitflaker.lucidsourcekit.data.records.SortEntry;
import com.bitflaker.lucidsourcekit.utils.Tools;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class DreamJournalView extends Fragment {

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
    private DreamJournalEntry.EntryType autoOpenJournalTypeCreator = null;
    private CompositeDisposable compositeDisposable;

    private final List<SortEntry> sortEntryValues = List.of(
            new SortEntry("timestamp - newest first", SortBy.Timestamp, true),
            new SortEntry("timestamp - oldest first", SortBy.Timestamp, false),
            new SortEntry("title - A to Z", SortBy.Title, true),
            new SortEntry("title - Z to A", SortBy.Title, false),
            new SortEntry("description - A to Z", SortBy.Description, true),
            new SortEntry("description - Z to A", SortBy.Description, false)
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setupViewResultLauncher();
        return inflater.inflate(R.layout.fragment_main_journal, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        compositeDisposable = new CompositeDisposable();
        getView().findViewById(R.id.ll_header).setLayoutParams(Tools.getRelativeLayoutParamsTopStatusbar(getContext()));

        db = MainDatabase.getInstance(getContext());
        fabAdd = getView().findViewById(R.id.btn_add_journal_entry);
        fabText = getView().findViewById(R.id.fab_text);
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

        compositeDisposable.add(db.getJournalEntryDao().getAll().subscribe(journalEntries -> {
            getActivity().runOnUiThread(() -> {
                recyclerViewAdapterDreamJournal = new RecyclerViewAdapterDreamJournal(getActivity(), this, journalEntries);
                setBasics();
                setupFAB();
                setupSortButton();
                setupFilterButton();
                setupResetFilterButton();
                handleItemCount(journalEntries.size());
                recyclerViewAdapterDreamJournal.setOnEntryCountChangedListener(this::handleItemCount);
            });
        }));

        if(autoOpenJournalTypeCreator != null) {
            // TODO when an entry was created after the editor was opened by the alarm quick action, the list of entries in the MainViewer does not get updated
            showJournalCreator(autoOpenJournalTypeCreator);
            autoOpenJournalTypeCreator = null;
        }
    }

    private void setBasics() {
        noEntryFound.setText(Html.fromHtml("<span><big><big><strong>Uhh...</strong></big></big><br />" + getContext().getResources().getString(R.string.empty_dream_journal) + "</span>", Html.FROM_HTML_MODE_COMPACT));
        recyclerView.setAdapter(recyclerViewAdapterDreamJournal);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    private void setupResetFilterButton() {
        resetFilterEntries.setOnClickListener(e -> {
            resetFilterEntries.setVisibility(View.GONE);
            handleItemCount(recyclerViewAdapterDreamJournal.resetFilters());
        });
    }

    private void setupFilterButton() {
        filterEntries.setOnClickListener(e -> {
            // TODO start loading animation
            compositeDisposable.add(db.getJournalEntryTagDao().getAll().subscribe((journalEntryTags, throwable) -> {
                String[] availableTags = new String[journalEntryTags.size()];
                for (int i = 0; i < journalEntryTags.size(); i++) {
                    availableTags[i] = journalEntryTags.get(i).description;
                }
                FilterDialog fd = new FilterDialog(getContext(), availableTags, recyclerViewAdapterDreamJournal.getCurrentFilter());
                fd.setOnClickPositiveButton((dialog, g) -> {
                    AppliedFilter af = fd.getFilters();
                    if(!AppliedFilter.isEmptyFilter(af)){
                        handleItemCount(recyclerViewAdapterDreamJournal.filter(af));
                        resetFilterEntries.setVisibility(View.VISIBLE);
                    }
                    else if (resetFilterEntries.getVisibility() == View.VISIBLE) {
                        resetFilterEntries.setVisibility(View.GONE);
                        handleItemCount(recyclerViewAdapterDreamJournal.resetFilters());
                    }
                    fd.dismiss();
                });
                fd.show(getParentFragmentManager(), "filter-dialog");
            }));
        });
    }

    private void setupSortButton() {
        sortEntries.setOnClickListener(e -> {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext(), R.style.Theme_LucidSourceKit_ThemedDialog);
            builder.setTitle("Sort entries by");
            String[] entries = sortEntryValues.stream().map(SortEntry::sortText).toArray(String[]::new);
            builder.setSingleChoiceItems(entries, sortBy, (dialog, which) -> {
                sortBy = which;
                SortEntry sortEntry = sortEntryValues.get(sortBy);
                recyclerViewAdapterDreamJournal.submitSortedEntries(sortEntry.sortBy(), sortEntry.isDescending(), () -> recyclerView.scrollToPosition(0));
                dialog.dismiss();
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        });
    }

    private void setupFAB() {
        fabAdd.setOnClickListener(e -> animateFab());
        fabText.setOnClickListener(e -> showJournalCreator(DreamJournalEntry.EntryType.PLAIN_TEXT));
        fabForms.setOnClickListener(e -> showJournalCreator(DreamJournalEntry.EntryType.FORMS_TEXT));
    }

    private void handleItemCount(int itemCount) {
        noEntryFound.setVisibility(itemCount == 0 ? View.VISIBLE : View.GONE);
    }

    public void showJournalCreator(DreamJournalEntry.EntryType type) {
        animateFab();
        Intent intent = new Intent(requireContext(), DreamJournalEditorView.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("DREAM_JOURNAL_TYPE", type.ordinal());
        journalEditorActivityResultLauncher.launch(intent);
    }

    private void animateFab() {
        @ColorInt int colorClosedBackground = Tools.getAttrColor(R.attr.colorPrimaryContainer, getContext().getTheme());
        @ColorInt int colorOnClosedBackground = Tools.getAttrColor(R.attr.colorOnPrimaryContainer, getContext().getTheme());
        @ColorInt int colorOpenBackground = Tools.getAttrColor(R.attr.colorSurfaceContainerHigh, getContext().getTheme());
        @ColorInt int colorOnOpenBackground = Tools.getAttrColor(R.attr.colorOnSurface, getContext().getTheme());

        if (fabAdd == null) { return; }
        if (isOpen) {
            fabAdd.startAnimation(rotateForward);
            fabText.startAnimation(fabClose);
            fabForms.startAnimation(fabClose);
            Tools.animateBackgroundTint(fabAdd, colorOpenBackground, colorClosedBackground, 300);
            Tools.animateImageTint(fabAdd, colorOnOpenBackground, colorOnClosedBackground, 300);
            fabText.setClickable(false);
            fabForms.setClickable(false);
            isOpen=false;
        }
        else {
            fabAdd.startAnimation(rotateBackward);
            fabText.startAnimation(fabOpen);
            fabForms.startAnimation(fabOpen);
            Tools.animateBackgroundTint(fabAdd, colorClosedBackground, colorOpenBackground, 300);
            Tools.animateImageTint(fabAdd, colorOnClosedBackground, colorOnOpenBackground, 300);
            fabText.setClickable(true);
            fabForms.setClickable(true);
            isOpen=true;
        }
    }

    public void pageChanged() {
        isOpen = false;
    }

    private void setupViewResultLauncher() {
        journalEditorActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            Intent data = result.getData();
            if (result.getResultCode() == Activity.RESULT_OK && data != null && data.hasExtra("entryId")) {
                int entryId = data.getIntExtra("entryId", -1);
                reloadEntryData(entryId);
            }
        });
    }

    private void reloadEntryData(int entryId) {
        DreamJournalEntry entry = db.getJournalEntryDao().getEntryDataById(entryId).blockingGet();
        recyclerViewAdapterDreamJournal.updateDataForEntry(entry, insertedIndex -> {
            if (insertedIndex != -1) {
                recyclerView.scrollToPosition(insertedIndex);
            }
        });
    }

    public void showJournalCreatorWhenLoaded(@Nullable DreamJournalEntry.EntryType type) {
        this.autoOpenJournalTypeCreator = type;
    }

    @Override
    public void onDestroyView() {
        compositeDisposable.dispose();
        super.onDestroyView();
    }
}