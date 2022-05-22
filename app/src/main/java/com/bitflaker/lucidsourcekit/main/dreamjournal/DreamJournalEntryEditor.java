package com.bitflaker.lucidsourcekit.main.dreamjournal;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.setup.ViewPagerAdapter;
import com.google.android.material.tabs.TabLayout;

public class DreamJournalEntryEditor extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager2;

    private ViewPagerAdapter vpAdapter;

    private DreamJournalContentEditor vwDreamContentEditor;
    private DreamJournalRatingEditor vwDreamRatingsEditor;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dream_journal_entry_editor);

        vpAdapter = new ViewPagerAdapter(getSupportFragmentManager(), getLifecycle());
        tabLayout = findViewById(R.id.tl_dj_editor_layout);
        viewPager2 = findViewById(R.id.vp_dj_editor);
        viewPager2.setOnTouchListener((v, event) -> true);
        vwDreamContentEditor = new DreamJournalContentEditor();
        vwDreamRatingsEditor = new DreamJournalRatingEditor();

        String entryID = JournalInMemoryManager.getInstance().newEntry();
        vwDreamContentEditor.setJournalEntryId(entryID);
        vwDreamRatingsEditor.setJournalEntryId(entryID);

        vwDreamContentEditor.setOnContinueButtonClicked(() -> {
            tabLayout.selectTab(tabLayout.getTabAt(1));
        });

        vwDreamRatingsEditor.setOnBackButtonClicked(() -> {
            tabLayout.selectTab(tabLayout.getTabAt(0));
        });
        vwDreamRatingsEditor.setOnDoneButtonClicked(() -> {
            // TODO: store data
        });

        vpAdapter.addFragment(vwDreamContentEditor, "");
        vpAdapter.addFragment(vwDreamRatingsEditor, "");
        viewPager2.setAdapter(vpAdapter);
        viewPager2.setOffscreenPageLimit(3);

        tabLayout.addOnTabSelectedListener(tabSelected());
        viewPager2.registerOnPageChangeCallback(changeTab());
    }

    @NonNull
    private ViewPager2.OnPageChangeCallback changeTab() {
        return new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
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