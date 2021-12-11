package com.bitflaker.lucidsourcekit.main;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.setup.SetupOpenSource;
import com.bitflaker.lucidsourcekit.setup.ViewPagerAdapter;
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity;
import com.google.android.material.tabs.TabLayout;

public class MainViewer extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager2;
    private ImageButton moreOptions;

    private final String pageOverview = "";
    private final String pageLogging = "";
    private final String pageStats = "";
    private final String pageGoals = "";
    private final String pageEvents = "";

    private ViewPagerAdapter vpAdapter;

    private MainOverview vwOverview;
    private SetupOpenSource vwLogging;
    private SetupOpenSource vwPageStats;
    private SetupOpenSource vwPageGoals;
    private SetupOpenSource vwPageEvents;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_viewer);
        initVars();

        vpAdapter.addFragment(vwOverview, pageOverview);
        vpAdapter.addFragment(vwLogging, pageLogging);
        vpAdapter.addFragment(vwPageStats, pageStats);
        vpAdapter.addFragment(vwPageGoals, pageGoals);
        vpAdapter.addFragment(vwPageEvents, pageEvents);
        viewPager2.setAdapter(vpAdapter);

        tabLayout.addOnTabSelectedListener(tabSelected());
        viewPager2.registerOnPageChangeCallback(changeTab());
        moreOptions.setOnClickListener(e -> {
            startActivity(new Intent(this, OssLicensesMenuActivity.class));
        });
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

    private void initVars() {
        tabLayout = findViewById(R.id.tablayout);
        viewPager2 = findViewById(R.id.viewpager);
        moreOptions = findViewById(R.id.btn_more_options);

        vpAdapter = new ViewPagerAdapter(getSupportFragmentManager(), getLifecycle());
        vwOverview = new MainOverview();
        vwLogging = new SetupOpenSource();
        vwPageStats = new SetupOpenSource();
        vwPageGoals = new SetupOpenSource();
        vwPageEvents = new SetupOpenSource();
    }
}