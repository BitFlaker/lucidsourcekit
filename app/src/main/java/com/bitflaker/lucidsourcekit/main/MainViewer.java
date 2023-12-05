package com.bitflaker.lucidsourcekit.main;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.viewpager2.widget.ViewPager2;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.general.JournalTypes;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.bitflaker.lucidsourcekit.setup.ViewPagerAdapter;
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity;
import com.google.android.material.tabs.TabLayout;

public class MainViewer extends AppCompatActivity {
    private static final String PAGE_OVERVIEW = "overview";
    private static final String PAGE_LOGGING = "journal";
    private static final String PAGE_STATS = "statistics";
    private static final String PAGE_GOALS = "goals";
    private static final String PAGE_BINAURAL_BEATS = "binaural";

    private ActivityResultLauncher<Intent> backupSaveDialogLauncher;
    private ActivityResultLauncher<Intent> backupLoadDialogLauncher;

    private TabLayout tabLayout;
    private ViewPager2 viewPager2;
    private ImageButton moreOptions;
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

        backupSaveDialogLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), this::backupSaveDialogResult);
        backupLoadDialogLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), this::backupLoadDialogResult);

        vpAdapter.addFragment(vwOverview, PAGE_OVERVIEW);
        vpAdapter.addFragment(vwLogging, PAGE_LOGGING);
        vpAdapter.addFragment(vwPageStats, PAGE_STATS);
        vpAdapter.addFragment(vwPageGoals, PAGE_GOALS);
        vpAdapter.addFragment(vwPageBinauralBeats, PAGE_BINAURAL_BEATS);
        viewPager2.setAdapter(vpAdapter);
        viewPager2.setOffscreenPageLimit(6);

        tabLayout.addOnTabSelectedListener(tabSelected());
        viewPager2.registerOnPageChangeCallback(changeTab());
        moreOptions.setOnClickListener(e -> {
            androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(new ContextThemeWrapper(this, Tools.getPopupTheme()), moreOptions);
            popup.getMenuInflater().inflate(R.menu.more_options_popup_menu, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.itm_third_party) {
                    startActivity(new Intent(this, OssLicensesMenuActivity.class));
                }
                else if (item.getItemId() == R.id.itm_export_data) {
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("application/zip");
                    intent.putExtra(Intent.EXTRA_TITLE, "backupdata.zip");
                    backupSaveDialogLauncher.launch(intent);
                }
                else if (item.getItemId() == R.id.itm_import_data) {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("application/zip");
                    backupLoadDialogLauncher.launch(intent);
                }
                return true;
            });
            popup.show();
        });

        if(getIntent().hasExtra("INITIAL_PAGE")) {
            String title = getIntent().getStringExtra("INITIAL_PAGE");
            int position = vpAdapter.getTabIndex(title);
            tabLayout.selectTab(tabLayout.getTabAt(position));
            viewPager2.setCurrentItem(position);
            if(title != null && title.equalsIgnoreCase(PAGE_LOGGING)) {
                int ordinal = getIntent().getIntExtra("type", -1);
                if(ordinal >= 0 && ordinal < JournalTypes.values().length) {
                    vwLogging.showJournalCreatorWhenLoaded(JournalTypes.values()[ordinal]);
                }
            }
        }
    }

    private void backupSaveDialogResult(ActivityResult result) {
        if(result.getResultCode() == Activity.RESULT_OK) {
            Intent data = result.getData();
            Uri uri;
            if(data != null && (uri = data.getData()) != null) {
                if(!MainDatabase.getInstance(this).backupDatabase(this, uri)) {
                    Toast.makeText(this, "Backup failed!", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
        Toast.makeText(this, "Failed to get backup file path", Toast.LENGTH_SHORT).show();
    }

    private void backupLoadDialogResult(ActivityResult result) {
        if(result.getResultCode() == Activity.RESULT_OK) {
            Intent data = result.getData();
            Uri uri;
            if(data != null && (uri = data.getData()) != null) {
                if(!MainDatabase.getInstance(this).restoreDatabase(this, uri)) {
                    Toast.makeText(this, "Backup restore failed!", Toast.LENGTH_SHORT).show();
                }
                else {
                    Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
                    if(intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                    }
                    else {
                        Log.w("MainViewer_Backup_Restore", "Unable to restart app after successful backup restore. Intent was null");
                        Toast.makeText(this, "Unable to restart app. Open it again manually", Toast.LENGTH_LONG).show();
                    }
                    System.exit(0);
                }
                return;
            }
        }
        Toast.makeText(this, "Failed to get backup file path", Toast.LENGTH_SHORT).show();
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