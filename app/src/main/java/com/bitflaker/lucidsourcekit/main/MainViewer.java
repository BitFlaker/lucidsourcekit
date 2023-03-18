package com.bitflaker.lucidsourcekit.main;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.viewpager2.widget.ViewPager2;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.general.JournalTypes;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.bitflaker.lucidsourcekit.setup.ViewPagerAdapter;
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity;
import com.google.android.material.tabs.TabLayout;

public class MainViewer extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager2;
    private ImageButton moreOptions;
    private final String pageOverview = "overview";
    private final String pageLogging = "journal";
    private final String pageStats = "statistics";
    private final String pageGoals = "goals";
    private final String pageBinauralBeats = "binaural";

    private ViewPagerAdapter vpAdapter;

    private MainOverview vwOverview;
    private DreamJournal vwLogging;
    private Statistics vwPageStats;
    private Goals vwPageGoals;
    private BinauralBeatsView vwPageBinauralBeats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Tools.getTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_viewer);
        Tools.makeStatusBarTransparent(this);
        initVars();

        vpAdapter.addFragment(vwOverview, pageOverview);
        vpAdapter.addFragment(vwLogging, pageLogging);
        vpAdapter.addFragment(vwPageStats, pageStats);
        vpAdapter.addFragment(vwPageGoals, pageGoals);
        vpAdapter.addFragment(vwPageBinauralBeats, pageBinauralBeats);
        viewPager2.setAdapter(vpAdapter);
        viewPager2.setOffscreenPageLimit(6);

        tabLayout.addOnTabSelectedListener(tabSelected());
        viewPager2.registerOnPageChangeCallback(changeTab());
        moreOptions.setOnClickListener(e -> {
            androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(new ContextThemeWrapper(this, Tools.getPopupTheme()), moreOptions);
            popup.getMenuInflater().inflate(R.menu.more_options_popup_menu, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                String itemTitle = item.getTitle().toString();
                if(itemTitle.equals(MainViewer.this.getResources().getString(R.string.third_party_licenses))) {
                    startActivity(new Intent(this, OssLicensesMenuActivity.class));
                }
                return true;
            });
            popup.show();
        });

        if(getIntent().hasExtra("INITIAL_PAGE")) {
            String title = getIntent().getStringExtra("INITIAL_PAGE");
            System.out.println("INITIAL_PAGE: " + title);
            int position = vpAdapter.getTabIndex(title);
            tabLayout.selectTab(tabLayout.getTabAt(position));
            viewPager2.setCurrentItem(position);
            if(title.equalsIgnoreCase(pageLogging)) {
                int ordinal = getIntent().getIntExtra("type", -1);
                if(ordinal >= 0 && ordinal < JournalTypes.values().length) {
                    vwLogging.showJournalCreatorWhenLoaded(JournalTypes.values()[ordinal]);
                }
            }
        }
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
                vwLogging.pageChanged();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }

            @Override
            public void onTabReselected(TabLayout.Tab tab) { }
        };
    }

    private void initVars() {
        tabLayout = findViewById(R.id.tablayout);
        viewPager2 = findViewById(R.id.viewpager);
        moreOptions = findViewById(R.id.btn_more_options);

        vpAdapter = new ViewPagerAdapter(getSupportFragmentManager(), getLifecycle());
        vwOverview = new MainOverview();
        vwLogging = new DreamJournal();
        vwPageStats = new Statistics();
        vwPageGoals = new Goals(viewPager2);
        vwPageBinauralBeats = new BinauralBeatsView();
    }
}