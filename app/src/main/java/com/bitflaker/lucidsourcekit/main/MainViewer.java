package com.bitflaker.lucidsourcekit.main;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.viewpager2.widget.ViewPager2;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.resulttables.DreamJournalEntry;
import com.bitflaker.lucidsourcekit.databinding.ActivityMainViewerBinding;
import com.bitflaker.lucidsourcekit.databinding.DialogProgressBinding;
import com.bitflaker.lucidsourcekit.main.binauralbeats.BinauralBeatsView;
import com.bitflaker.lucidsourcekit.main.dreamjournal.DreamJournalView;
import com.bitflaker.lucidsourcekit.main.goals.GoalsView;
import com.bitflaker.lucidsourcekit.main.overview.MainOverviewView;
import com.bitflaker.lucidsourcekit.main.statistics.StatisticsView;
import com.bitflaker.lucidsourcekit.setup.ViewPagerAdapter;
import com.bitflaker.lucidsourcekit.utils.BackupTask;
import com.bitflaker.lucidsourcekit.utils.BackupTaskCallback;
import com.bitflaker.lucidsourcekit.utils.Tools;
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;

import java.util.Locale;

public class MainViewer extends AppCompatActivity {
    private static final String PAGE_OVERVIEW = "overview";
    private static final String PAGE_LOGGING = "journal";
    private static final String PAGE_STATS = "statistics";
    private static final String PAGE_GOALS = "goals";
    private static final String PAGE_BINAURAL_BEATS = "binaural";

    private ActivityResultLauncher<Intent> backupSaveDialogLauncher;
    private ActivityResultLauncher<Intent> backupLoadDialogLauncher;

    private ViewPagerAdapter vpAdapter;

    private MainOverviewView vwOverview;
    private DreamJournalView vwLogging;
    private StatisticsView vwPageStats;
    private GoalsView vwPageGoals;
    private BinauralBeatsView vwPageBinauralBeats;

    private ActivityMainViewerBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainViewerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Tools.makeStatusBarTransparent(this);
        initVars();

        backupSaveDialogLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), this::backupSaveDialogResult);
        backupLoadDialogLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), this::backupLoadDialogResult);

        vpAdapter.addFragment(vwOverview, PAGE_OVERVIEW);
        vpAdapter.addFragment(vwLogging, PAGE_LOGGING);
        vpAdapter.addFragment(vwPageGoals, PAGE_GOALS);
        vpAdapter.addFragment(vwPageBinauralBeats, PAGE_BINAURAL_BEATS);
        vpAdapter.addFragment(vwPageStats, PAGE_STATS);
        binding.viewpager.setAdapter(vpAdapter);
        binding.viewpager.setOffscreenPageLimit(6);

        binding.tablayout.addOnTabSelectedListener(tabSelected());
        binding.viewpager.registerOnPageChangeCallback(changeTab());
        binding.btnMoreOptions.setOnClickListener(e -> {
            androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(new ContextThemeWrapper(this, R.style.Theme_LucidSourceKit_PopupMenu), binding.btnMoreOptions);
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

        if (getIntent().hasExtra("INITIAL_PAGE")) {
            String title = getIntent().getStringExtra("INITIAL_PAGE");
            int position = vpAdapter.getTabIndex(title);
            binding.tablayout.selectTab(binding.tablayout.getTabAt(position));
            binding.viewpager.setCurrentItem(position);
            if(title != null && title.equalsIgnoreCase(PAGE_LOGGING)) {
                int ordinal = getIntent().getIntExtra("DREAM_JOURNAL_TYPE", -1);
                if(ordinal >= 0 && ordinal < DreamJournalEntry.EntryType.values().length) {
                    vwLogging.showJournalCreatorWhenLoaded(DreamJournalEntry.EntryType.values()[ordinal]);
                }
            }
        }

        // Add handler for click on remember journal entry
        vwOverview.setRememberJournalEntryClickedListener(entry -> {
            binding.tablayout.selectTab(binding.tablayout.getTabAt(vpAdapter.getTabIndex(PAGE_LOGGING)));
            vwLogging.openJournalEntry(entry, true);
        });

        // Remove the old application data backup only if the app is stable for at least 5 seconds
        new Handler().postDelayed(() -> {
            BackupTask.Companion.deleteOldBeforeImportBackup(this);
            Log.i("BackupCleaner", "Deleted old application data backup from before last import as the application has been running for at least 5 seconds");
        }, 5000);
    }

    private void backupSaveDialogResult(ActivityResult result) {
        Intent data;
        Uri uri;

        // Try to get the Uri to the backup path
        if (result.getResultCode() != Activity.RESULT_OK || (data = result.getData()) == null || (uri = data.getData()) == null) {
            Toast.makeText(this, "Failed to get backup file path", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create the export progress dialog
        DialogProgressBinding dBinding = DialogProgressBinding.inflate(getLayoutInflater(), null, false);
        AlertDialog dialog = new MaterialAlertDialogBuilder(this, R.style.Theme_LucidSourceKit_ThemedDialog)
                .setTitle("Exporting")
                .setCancelable(false)
                .setView(dBinding.getRoot())
                .create();
        dialog.show();

        // Try to start the backup
        Context context = this;
        BackupTask.Companion.startBackup(this, uri, new BackupTaskCallback() {
            @Override
            public void onCompleted() {
                runOnUiThread(() -> Toast.makeText(context, "Backup done", Toast.LENGTH_LONG).show());
                dialog.dismiss();
            }

            @Override
            public void onError(@NonNull Throwable cause) {
                runOnUiThread(() -> Toast.makeText(context, "Backup failed: " + cause.getMessage(), Toast.LENGTH_LONG).show());
                dialog.dismiss();
            }

            @Override
            public void onProgress(@NonNull String fileName, int finished, int total) {
                int progress = 100 * finished / total;
                runOnUiThread(() -> {
                    dBinding.txtProgressText.setText(fileName);
                    dBinding.txtProgressPercentage.setText(String.format(Locale.getDefault(), "%d %%", progress));
                    dBinding.prgProgress.setProgress(progress);
                });
            }
        });
    }

    private void backupLoadDialogResult(ActivityResult result) {
        Intent data;
        Uri uri;

        // Try to get the Uri to the backup path
        if (result.getResultCode() != Activity.RESULT_OK || (data = result.getData()) == null || (uri = data.getData()) == null) {
            Toast.makeText(this, "Failed to get backup file path", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create the import progress dialog
        DialogProgressBinding dBinding = DialogProgressBinding.inflate(getLayoutInflater(), null, false);
        AlertDialog dialog = new MaterialAlertDialogBuilder(this, R.style.Theme_LucidSourceKit_ThemedDialog)
                .setTitle("Importing")
                .setCancelable(false)
                .setView(dBinding.getRoot())
                .create();
        dialog.show();

        dBinding.txtProgressPercentage.setVisibility(View.GONE);
        dBinding.prgProgress.setIndeterminate(true);

        Context context = this;
        Handler restartHandler = new Handler();
        BackupTask.Companion.startRestore(this, uri, new BackupTaskCallback() {
            @Override
            public void onCompleted() {
                runOnUiThread(() -> {
                    dBinding.txtProgressText.setText("Done! Restarting...");
                    dBinding.prgProgress.setIndeterminate(false);
                    dBinding.prgProgress.setProgress(100);
                });

                // Restart app after 2 seconds to apply changes
                restartHandler.postDelayed(() -> Tools.restartApp(context), 2000);
            }

            @Override
            public void onError(@NonNull Throwable cause) {
                runOnUiThread(() -> Toast.makeText(context, "Import failed: " + cause.getMessage(), Toast.LENGTH_LONG).show());
                dialog.dismiss();
            }

            @Override
            public void onProgress(@NonNull String fileName, int finished, int total) {
                runOnUiThread(() -> dBinding.txtProgressText.setText(fileName));
            }
        });
    }

    @NonNull
    private ViewPager2.OnPageChangeCallback changeTab() {
        return new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                binding.tablayout.selectTab(binding.tablayout.getTabAt(position));
            }
        };
    }

    @NonNull
    private TabLayout.OnTabSelectedListener tabSelected() {
        return new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                binding.viewpager.setCurrentItem(tab.getPosition());
                vwLogging.pageChanged();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }

            @Override
            public void onTabReselected(TabLayout.Tab tab) { }
        };
    }

    private void initVars() {
        vpAdapter = new ViewPagerAdapter(getSupportFragmentManager(), getLifecycle());
        vwOverview = new MainOverviewView();
        vwLogging = new DreamJournalView();
        vwPageStats = new StatisticsView();
        vwPageGoals = new GoalsView();
        vwPageBinauralBeats = new BinauralBeatsView();
    }
}