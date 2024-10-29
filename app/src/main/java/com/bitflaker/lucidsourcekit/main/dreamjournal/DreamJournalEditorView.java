package com.bitflaker.lucidsourcekit.main.dreamjournal;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.window.OnBackInvokedDispatcher;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.AudioLocation;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntryHasTag;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntryTag;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.resulttables.DreamJournalEntry;
import com.bitflaker.lucidsourcekit.databinding.ActivityJournalEditorBinding;
import com.bitflaker.lucidsourcekit.utils.Tools;
import com.bitflaker.lucidsourcekit.setup.ViewPagerAdapter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class DreamJournalEditorView extends AppCompatActivity {
    private int journalEntryId;
    private boolean isSaved = false;
    private DreamJournalEntry entry;
    private MainDatabase db;
    private CompositeDisposable compositeDisposable;
    private ActivityJournalEditorBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityJournalEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Tools.makeStatusBarTransparent(this);

        db = MainDatabase.getInstance(this);
        compositeDisposable = new CompositeDisposable();
        ViewPagerAdapter vpAdapter = new ViewPagerAdapter(getSupportFragmentManager(), getLifecycle());
        binding.vpDjEditor.setUserInputEnabled(false);
        binding.vpDjEditor.setOnTouchListener((v, event) -> true);

        journalEntryId = getIntent().getIntExtra("JOURNAL_ENTRY_ID", -1);
        entry = journalEntryId == -1 ? DreamJournalEntry.createDefault() : db.getJournalEntryDao().getEntryDataById(journalEntryId).blockingGet();

        DreamJournalEntry.EntryType type = DreamJournalEntry.EntryType.PLAIN_TEXT;
        int journalTypeId = getIntent().getIntExtra("DREAM_JOURNAL_TYPE", DreamJournalEntry.EntryType.PLAIN_TEXT.ordinal());
        if (journalTypeId >= 0 && journalTypeId < DreamJournalEntry.EntryType.values().length) {
            type = DreamJournalEntry.EntryType.values()[journalTypeId];
        }

        DreamJournalEditorContentView vwDreamContentEditor = new DreamJournalEditorContentView();
        DreamJournalEditorRatingView vwDreamRatingsEditor = new DreamJournalEditorRatingView();

        vwDreamContentEditor.setJournalEntryId(entry);
        vwDreamContentEditor.setJournalEntryType(type);
        vwDreamContentEditor.setOnContinueButtonClicked(() -> {
            binding.tlDjEditorLayout.selectTab(binding.tlDjEditorLayout.getTabAt(1));
            vwDreamRatingsEditor.updatePreview();
        });
        vwDreamContentEditor.setOnCloseButtonClicked(this::finish);

        vwDreamRatingsEditor.setJournalEntryId(entry);
        vwDreamRatingsEditor.setOnBackButtonClicked(() -> binding.tlDjEditorLayout.selectTab(binding.tlDjEditorLayout.getTabAt(0)));
        vwDreamRatingsEditor.setOnDoneButtonClicked(() -> compositeDisposable.add(storeEntry().subscribeOn(Schedulers.io()).subscribe()));
        vwDreamRatingsEditor.setOnCloseButtonClicked(this::finish);

        vpAdapter.addFragment(vwDreamContentEditor, "");
        vpAdapter.addFragment(vwDreamRatingsEditor, "");
        binding.vpDjEditor.setAdapter(vpAdapter);
        binding.vpDjEditor.setOffscreenPageLimit(3);

        binding.tlDjEditorLayout.addOnTabSelectedListener(tabSelected());
        binding.vpDjEditor.registerOnPageChangeCallback(changeTab());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::promptDiscardChanges);
        }
        else {
            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    promptDiscardChanges();
                }
            });
        }
    }

    private Completable storeEntry() {
        return Completable.fromCallable(() -> {
            // In case this entry already exists, remove all already present assigned types,
            // tags and audio locations to ensure no duplicates will be created.
            // This should probably be replaced with a diff calculation in the future
            if (entry.journalEntry.entryId != -1) {
                db.getJournalEntryHasTagDao().deleteAll(entry.journalEntry.entryId).blockingSubscribe();
                db.getJournalEntryIsTypeDao().deleteAll(entry.journalEntry.entryId).blockingSubscribe();
                db.getAudioLocationDao().deleteAll(entry.journalEntry.entryId).blockingSubscribe();
            }

            // Insert (or replace if already exists) the current journal entry
            int entryId = db.getJournalEntryDao().insert(entry.journalEntry).blockingGet().intValue();

            // Assign dream types to the current entry
            db.getJournalEntryIsTypeDao().insertAll(entry.getDreamTypeEntries(entryId)).blockingSubscribe();

            // Insert missing tags and assign them to the entry
            db.getJournalEntryTagDao().insertAll(entry.journalEntryTags).blockingSubscribe();
            List<JournalEntryTag> journalEntryTags = db.getJournalEntryTagDao().getByDescription(entry.journalEntryTags.stream().map(x -> x.description).collect(Collectors.toList())).blockingGet();
            List<JournalEntryHasTag> tags = journalEntryTags.stream().map(x -> new JournalEntryHasTag(entryId, x.tagId)).collect(Collectors.toList());
            db.getJournalEntryHasTagDao().insertAll(tags).blockingSubscribe();

            // Insert all added audio locations
            db.getAudioLocationDao().insertAll(entry.getAudioLocations(entryId)).blockingSubscribe();

            // Only delete all recordings after the entry was saved and the
            // reference to the recording in the database was removed
            entry.deleteAllRemovedAudioLocations();

            // Finish the editor activity
            isSaved = true;
            runOnUiThread(() -> {
                Intent data = new Intent();
                data.putExtra("entryId", entryId);
                setResult(RESULT_OK, data);
                finish();
            });

            return Completable.complete();
        });
    }

    private void promptDiscardChanges() {
        new MaterialAlertDialogBuilder(this, R.style.Theme_LucidSourceKit_ThemedDialog)
                .setTitle("Discard changes")
                .setMessage("Do you really want to discard all changes")
                .setPositiveButton(getResources().getString(R.string.yes), (dialog, which) -> finish())
                .setNegativeButton(getResources().getString(R.string.no), null)
                .show();
    }

    @Override
    protected void onStop() {
        compositeDisposable.dispose();
        boolean isModeCreate = journalEntryId == -1;
        if (!isSaved && isModeCreate) {
            for (AudioLocation recData : entry.audioLocations) {
                new File(recData.audioPath).delete();
            }
        }
        super.onStop();
    }

    @NonNull
    private ViewPager2.OnPageChangeCallback changeTab() {
        return new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                binding.tlDjEditorLayout.selectTab(binding.tlDjEditorLayout.getTabAt(position));
            }
        };
    }

    @NonNull
    private TabLayout.OnTabSelectedListener tabSelected() {
        return new TabLayout.OnTabSelectedListener() {
            @Override public void onTabUnselected(TabLayout.Tab tab) { }
            @Override public void onTabReselected(TabLayout.Tab tab) { }

            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                binding.vpDjEditor.setCurrentItem(tab.getPosition());
            }
        };
    }
}