package com.bitflaker.lucidsourcekit.main.dreamjournal;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.general.JournalTypes;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.bitflaker.lucidsourcekit.setup.ViewPagerAdapter;
import com.google.android.material.tabs.TabLayout;

import java.io.File;

public class DreamJournalEntryEditor extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager2;

    private ViewPagerAdapter vpAdapter;

    private DreamJournalContentEditor vwDreamContentEditor;
    private DreamJournalRatingEditor vwDreamRatingsEditor;
    private String journalInMemoryEntryID;
    private boolean isSaved;
    private JournalInMemory jim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dream_journal_entry_editor);

        vpAdapter = new ViewPagerAdapter(getSupportFragmentManager(), getLifecycle());
        tabLayout = findViewById(R.id.tl_dj_editor_layout);
        viewPager2 = findViewById(R.id.vp_dj_editor);
        viewPager2.setOnTouchListener((v, event) -> true);
        if(getIntent().hasExtra("journal_in_memory_id")) {
            journalInMemoryEntryID = getIntent().getStringExtra("journal_in_memory_id");
        }
        else {
            journalInMemoryEntryID = JournalInMemoryManager.getInstance().newEntry();
        }
        jim = JournalInMemoryManager.getInstance().getEntry(journalInMemoryEntryID);
        if(getIntent().hasExtra("type") && getIntent().getIntExtra("type", JournalTypes.Text.ordinal()) == JournalTypes.Forms.ordinal()){
            jim.setEntryType(JournalInMemory.EntryType.FORMS_TEXT);
        }
        isSaved = false;

        vwDreamContentEditor = new DreamJournalContentEditor();
        vwDreamContentEditor.setJournalEntryId(journalInMemoryEntryID);
        vwDreamContentEditor.setOnContinueButtonClicked(() -> tabLayout.selectTab(tabLayout.getTabAt(1)));
        vwDreamContentEditor.setOnCloseButtonClicked(this::finish);

        vwDreamRatingsEditor = new DreamJournalRatingEditor();
        vwDreamRatingsEditor.setJournalEntryId(journalInMemoryEntryID);
        vwDreamRatingsEditor.setOnBackButtonClicked(() -> tabLayout.selectTab(tabLayout.getTabAt(0)));
        vwDreamRatingsEditor.setOnDoneButtonClicked(() -> {
            // TODO: store data
        });
        vwDreamRatingsEditor.setOnDreamJournalEntrySavedListener((entryId) -> {
            isSaved = true;
            Intent data = new Intent();
            data.putExtra("entryId", entryId);
            data.putExtra("journal_in_memory_id", journalInMemoryEntryID);
            setResult(RESULT_OK, data);
            finish();
        });
        vwDreamRatingsEditor.setOnCloseButtonClicked(this::finish);

        vpAdapter.addFragment(vwDreamContentEditor, "");
        vpAdapter.addFragment(vwDreamRatingsEditor, "");
        viewPager2.setAdapter(vpAdapter);
        viewPager2.setOffscreenPageLimit(3);

        tabLayout.addOnTabSelectedListener(tabSelected());
        viewPager2.registerOnPageChangeCallback(changeTab());
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this, Tools.getThemeDialog()).setTitle("Discard changes").setMessage("Do you really want to discard all changes")
                .setPositiveButton(getResources().getString(R.string.yes), (dialog, which) -> {
                    super.onBackPressed();
                })
                .setNegativeButton(getResources().getString(R.string.no), null)
                .show();
    }

    @Override
    protected void onStop() {
        if(!isSaved && jim.getEditMode() != JournalInMemory.EditMode.EDIT) {
            for (RecordingData recData : jim.getAudioRecordings()) {
                File audio = new File(recData.getFilepath());
                if(audio.delete()) {
                    jim.markAudioRecordingToRemove(recData);
                }
            }
        }
        super.onStop();
    }

    @NonNull
    private ViewPager2.OnPageChangeCallback changeTab() {
        return new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                // TODO probably optimize as it should only store when it was 0 before and is different now (but does not matter with only 2 pages)
                if(tabLayout.getSelectedTabPosition() != 0) {
                    jim.setTitle(vwDreamContentEditor.getTitle());
                    jim.setDescription(vwDreamContentEditor.getDescription());
                }
                tabLayout.selectTab(tabLayout.getTabAt(position));
            }
        };
    }

    @NonNull
    private TabLayout.OnTabSelectedListener tabSelected() {
        return new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager2.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }

            @Override
            public void onTabReselected(TabLayout.Tab tab) { }
        };
    }
}