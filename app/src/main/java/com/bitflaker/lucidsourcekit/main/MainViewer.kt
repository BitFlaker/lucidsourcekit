package com.bitflaker.lucidsourcekit.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.data.export.ExportDialog
import com.bitflaker.lucidsourcekit.data.export.SimpleActivityLauncher
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.resulttables.DreamJournalEntry
import com.bitflaker.lucidsourcekit.databinding.ActivityMainViewerBinding
import com.bitflaker.lucidsourcekit.databinding.DialogProgressBinding
import com.bitflaker.lucidsourcekit.main.about.AboutActivity
import com.bitflaker.lucidsourcekit.main.binauralbeats.BinauralBeatsView
import com.bitflaker.lucidsourcekit.main.dreamjournal.DreamJournalView
import com.bitflaker.lucidsourcekit.main.goals.GoalsView
import com.bitflaker.lucidsourcekit.main.overview.MainOverviewView
import com.bitflaker.lucidsourcekit.main.statistics.StatisticsView
import com.bitflaker.lucidsourcekit.setup.ViewPagerAdapter
import com.bitflaker.lucidsourcekit.utils.BackupTask
import com.bitflaker.lucidsourcekit.utils.BackupTask.Companion.deleteOldBeforeImportBackup
import com.bitflaker.lucidsourcekit.utils.BackupTaskCallback
import com.bitflaker.lucidsourcekit.utils.Tools
import com.bitflaker.lucidsourcekit.utils.generateFileName
import com.bitflaker.lucidsourcekit.utils.showToastLong
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale


private const val PAGE_OVERVIEW = "overview"
private const val PAGE_LOGGING = "journal"
private const val PAGE_STATS = "statistics"
private const val PAGE_GOALS = "goals"
private const val PAGE_BINAURAL_BEATS = "binaural"

class MainViewer : AppCompatActivity() {
    private val backupSaveDialogLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(StartActivityForResult(), this::backupSaveDialogResult)
    private val backupLoadDialogLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(StartActivityForResult(), this::backupLoadDialogResult)

    private lateinit var vpAdapter: ViewPagerAdapter

    private val vwOverview: MainOverviewView = MainOverviewView()
    private val vwLogging: DreamJournalView = DreamJournalView()
    private val vwPageStats: StatisticsView = StatisticsView()
    private val vwPageGoals: GoalsView = GoalsView()
    private val vwPageBinauralBeats: BinauralBeatsView = BinauralBeatsView()

    private lateinit var binding: ActivityMainViewerBinding
    private lateinit var launcher: SimpleActivityLauncher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vpAdapter = ViewPagerAdapter(supportFragmentManager, lifecycle)
        launcher = SimpleActivityLauncher(this, 2)
        binding = ActivityMainViewerBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Register page fragments in the desired order
        vpAdapter.addFragment(vwOverview, PAGE_OVERVIEW)
        vpAdapter.addFragment(vwLogging, PAGE_LOGGING)
        vpAdapter.addFragment(vwPageGoals, PAGE_GOALS)
        vpAdapter.addFragment(vwPageBinauralBeats, PAGE_BINAURAL_BEATS)
        vpAdapter.addFragment(vwPageStats, PAGE_STATS)

        // Set the adapter and never unload fragments
        binding.viewpager.setAdapter(vpAdapter)
        binding.viewpager.setOffscreenPageLimit(vpAdapter.itemCount + 1)
        binding.viewpager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.tablayout.selectTab(binding.tablayout.getTabAt(position))
            }
        })

        binding.tablayout.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabUnselected(tab: TabLayout.Tab) { }
            override fun onTabReselected(tab: TabLayout.Tab) { }
            override fun onTabSelected(tab: TabLayout.Tab) {
                binding.viewpager.currentItem = tab.position
                vwLogging.pageChanged()
            }
        })

        // Create popup context menu when clicking on `more` button
        binding.btnMoreOptions.setOnClickListener { e ->
            val popup = PopupMenu(ContextThemeWrapper(this, R.style.Theme_LucidSourceKit_PopupMenu), binding.btnMoreOptions)
            popup.menuInflater.inflate(R.menu.more_options_popup_menu, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item!!.itemId) {
                     R.id.itm_export_data -> {
                        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            setType("application/zip")
                            putExtra(Intent.EXTRA_TITLE, generateFileName("LucidSourceKit_Backup", "zip"))
                        }
                        backupSaveDialogLauncher.launch(intent)
                    }
                    R.id.itm_import_data -> {
                        promptImportBackup()
                    }
                    R.id.itm_export -> {
                        lifecycleScope.launch(Dispatchers.IO) {
                            ExportDialog.promptExportData(this@MainViewer, launcher)
                        }
                    }
                    R.id.itm_about -> {
                        startActivity(Intent(this, AboutActivity::class.java))
                    }
                    else -> {
                        Tools.showPlaceholderDialog(this)
                    }
                }
                true
            }
            popup.show()
        }

        // Handle the case to show a different page at startup
        val initialPage = intent.getStringExtra("INITIAL_PAGE")
        if (!initialPage.isNullOrEmpty()) {
            binding.viewpager.currentItem = vpAdapter.getTabIndex(initialPage)
            binding.tablayout.selectTab(binding.tablayout.getTabAt(binding.viewpager.currentItem))
            if (initialPage.equals(PAGE_LOGGING, ignoreCase = true)) {
                val ordinal = intent.getIntExtra("DREAM_JOURNAL_TYPE", -1)
                if (ordinal >= 0 && ordinal < DreamJournalEntry.EntryType.entries.size) {
                    vwLogging.showJournalCreatorWhenLoaded(DreamJournalEntry.EntryType.entries[ordinal])
                }
            }
        }

        // Add handler for click on remember journal entry
        vwOverview.rememberJournalEntryClickedListener = { entry ->
            binding.tablayout.selectTab(binding.tablayout.getTabAt(vpAdapter.getTabIndex(PAGE_LOGGING)))
            vwLogging.openJournalEntry(entry, true)
        }

        // Remove the old application data backup only if the app is stable for at least 5 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            deleteOldBeforeImportBackup(this)
            Log.i("BackupCleaner", "Deleted old application data backup from before import")
        }, 5000)
    }

    private fun promptImportBackup() {
        MaterialAlertDialogBuilder(this, R.style.Theme_LucidSourceKit_ThemedDialog)
            .setTitle("Import backup")
            .setMessage("Importing a backup will erase all of your current data! Are you sure you want to proceed?")
            .setPositiveButton(getResources().getString(R.string.yes)) { dialog, which ->
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    setType("application/zip")
                }
                backupLoadDialogLauncher.launch(intent)
            }
            .setNegativeButton(getResources().getString(R.string.no), null)
            .show()
    }

    private fun backupSaveDialogResult(result: ActivityResult) {
        val data: Intent? = result.data
        val uri: Uri? = data?.data

        // Try to get the Uri to the backup path
        if (result.resultCode != RESULT_OK || data == null || uri == null) {
            Toast.makeText(this, "Failed to get backup file path", Toast.LENGTH_SHORT).show()
            return
        }

        // Create the export progress dialog
        val dBinding = DialogProgressBinding.inflate(layoutInflater, null, false)
        val dialog = MaterialAlertDialogBuilder(this, R.style.Theme_LucidSourceKit_ThemedDialog)
            .setTitle("Exporting")
            .setCancelable(false)
            .setView(dBinding.getRoot())
            .create()
        dialog.show()

        // Try to create a backup
        val context: Context = this
        BackupTask.startBackup(this, uri, object : BackupTaskCallback {
            override fun onCompleted() {
                dialog.dismiss()
                runOnUiThread {
                    showToastLong(context, "Backup done")
                }
            }

            override fun onError(cause: Throwable) {
                dialog.dismiss()
                runOnUiThread {
                    showToastLong(context, "Backup failed: " + cause.message)
                }
            }

            override fun onProgress(fileName: String, finished: Int, total: Int) {
                runOnUiThread {
                    dBinding.prgProgress.progress = 100 * finished / total
                    dBinding.txtProgressText.text = fileName
                    dBinding.txtProgressPercentage.text = String.format(Locale.getDefault(), "%d %%", dBinding.prgProgress.progress)
                }
            }
        })
    }

    private fun backupLoadDialogResult(result: ActivityResult) {
        val data: Intent? = result.data
        val uri: Uri? = data?.data

        // Try to get the Uri to the backup path
        if (result.resultCode != RESULT_OK || data == null || uri == null) {
            Toast.makeText(this, "Failed to get backup file path", Toast.LENGTH_SHORT).show()
            return
        }

        // Create the import progress dialog
        val dBinding = DialogProgressBinding.inflate(layoutInflater, null, false)
        val dialog = MaterialAlertDialogBuilder(this, R.style.Theme_LucidSourceKit_ThemedDialog)
            .setTitle("Importing")
            .setCancelable(false)
            .setView(dBinding.getRoot())
            .create()
        dialog.show()

        dBinding.txtProgressPercentage.visibility = View.GONE
        dBinding.prgProgress.isIndeterminate = true

        // Try to restore the backup
        val context: Context = this
        val restartHandler = Handler(Looper.getMainLooper())
        BackupTask.startRestore(this, uri, object : BackupTaskCallback {
            override fun onCompleted() {
                runOnUiThread {
                    dBinding.txtProgressText.text = "Done! Restarting..."
                    dBinding.prgProgress.isIndeterminate = false
                    dBinding.prgProgress.progress = 100
                }

                // Restart app after 2 seconds to apply changes
                restartHandler.postDelayed({ Tools.restartApp(context) }, 2000)
            }

            override fun onError(cause: Throwable) {
                dialog.dismiss()
                runOnUiThread {
                    showToastLong(context, "Import failed: " + cause.message)
                }
            }

            override fun onProgress(fileName: String, finished: Int, total: Int) {
                runOnUiThread {
                    dBinding.txtProgressText.text = fileName
                }
            }
        })
    }
}