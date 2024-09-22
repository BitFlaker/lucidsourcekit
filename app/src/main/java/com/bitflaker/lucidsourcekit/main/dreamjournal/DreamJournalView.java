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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.data.enums.SortBy;
import com.bitflaker.lucidsourcekit.data.records.AppliedFilter;
import com.bitflaker.lucidsourcekit.data.records.SortEntry;
import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.resulttables.DreamJournalEntry;
import com.bitflaker.lucidsourcekit.databinding.FragmentMainJournalBinding;
import com.bitflaker.lucidsourcekit.utils.Tools;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class DreamJournalView extends Fragment {
    public ActivityResultLauncher<Intent> journalEditorActivityResultLauncher;
    private RecyclerViewAdapterDreamJournal recyclerViewAdapterDreamJournal = null;
    private Animation fabOpen, fabClose, rotateForward, rotateBackward;
    private DreamJournalEntry.EntryType autoOpenJournalTypeCreator = null;
    private CompositeDisposable compositeDisposable;
    private FragmentMainJournalBinding binding;
    private MainDatabase db;
    private boolean isOpen = false;
    private int sortBy = 0;

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
        binding = FragmentMainJournalBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        compositeDisposable = new CompositeDisposable();
        binding.llHeader.setLayoutParams(Tools.getRelativeLayoutParamsTopStatusbar(getContext()));

        db = MainDatabase.getInstance(getContext());

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
        binding.txtNoEntries.setText(Html.fromHtml("<span><big><big><strong>Uhh...</strong></big></big><br />" + getContext().getResources().getString(R.string.empty_dream_journal) + "</span>", Html.FROM_HTML_MODE_COMPACT));
        binding.recyclerView.setAdapter(recyclerViewAdapterDreamJournal);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    private void setupResetFilterButton() {
        binding.btnFilterOff.setOnClickListener(e -> {
            binding.btnFilterOff.setVisibility(View.GONE);
            handleItemCount(recyclerViewAdapterDreamJournal.resetFilters());
        });
    }

    private void setupFilterButton() {
        binding.btnFilter.setOnClickListener(e -> {
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
                        binding.btnFilterOff.setVisibility(View.VISIBLE);
                    }
                    else if (binding.btnFilterOff.getVisibility() == View.VISIBLE) {
                        binding.btnFilterOff.setVisibility(View.GONE);
                        handleItemCount(recyclerViewAdapterDreamJournal.resetFilters());
                    }
                    fd.dismiss();
                });
                fd.show(getParentFragmentManager(), "filter-dialog");
            }));
        });
    }

    private void setupSortButton() {
        binding.btnSort.setOnClickListener(e -> {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext(), R.style.Theme_LucidSourceKit_ThemedDialog);
            builder.setTitle("Sort entries by");
            String[] entries = sortEntryValues.stream().map(SortEntry::sortText).toArray(String[]::new);
            builder.setSingleChoiceItems(entries, sortBy, (dialog, which) -> {
                sortBy = which;
                SortEntry sortEntry = sortEntryValues.get(sortBy);
                recyclerViewAdapterDreamJournal.submitSortedEntries(sortEntry.sortBy(), sortEntry.isDescending(), () -> binding.recyclerView.scrollToPosition(0));
                dialog.dismiss();
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        });
    }

    private void setupFAB() {
        binding.btnAddJournalEntry.setOnClickListener(e -> animateFab());
        binding.fabText.setOnClickListener(e -> showJournalCreator(DreamJournalEntry.EntryType.PLAIN_TEXT));
        binding.fabForms.setOnClickListener(e -> showJournalCreator(DreamJournalEntry.EntryType.FORMS_TEXT));
    }

    private void handleItemCount(int itemCount) {
        binding.txtNoEntries.setVisibility(itemCount == 0 ? View.VISIBLE : View.GONE);
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

        if (isOpen) {
            binding.btnAddJournalEntry.startAnimation(rotateForward);
            binding.fabText.startAnimation(fabClose);
            binding.fabForms.startAnimation(fabClose);
            Tools.animateBackgroundTint(binding.btnAddJournalEntry, colorOpenBackground, colorClosedBackground, 300);
            Tools.animateImageTint(binding.btnAddJournalEntry, colorOnOpenBackground, colorOnClosedBackground, 300);
            binding.fabText.setClickable(false);
            binding.fabForms.setClickable(false);
            isOpen=false;
        }
        else {
            binding.btnAddJournalEntry.startAnimation(rotateBackward);
            binding.fabText.startAnimation(fabOpen);
            binding.fabForms.startAnimation(fabOpen);
            Tools.animateBackgroundTint(binding.btnAddJournalEntry, colorClosedBackground, colorOpenBackground, 300);
            Tools.animateImageTint(binding.btnAddJournalEntry, colorOnClosedBackground, colorOnOpenBackground, 300);
            binding.fabText.setClickable(true);
            binding.fabForms.setClickable(true);
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
                binding.recyclerView.scrollToPosition(insertedIndex);
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