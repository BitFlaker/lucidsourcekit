package com.bitflaker.lucidsourcekit.main.dreamjournal.views

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.database.MainDatabase
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntryHasTag
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.results.DreamJournalEntry
import com.bitflaker.lucidsourcekit.databinding.ActivityJournalEditorBinding
import com.bitflaker.lucidsourcekit.setup.ViewPagerAdapter
import com.bitflaker.lucidsourcekit.utils.insetNoTop
import com.bitflaker.lucidsourcekit.utils.onBackPressed
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class DreamJournalEditorView : AppCompatActivity() {
    private lateinit var binding: ActivityJournalEditorBinding
    private lateinit var db: MainDatabase
    private var journalEntryId = 0
    private var isSaved = false
    private lateinit var entry: DreamJournalEntry

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = MainDatabase.getInstance(this)
        binding = ActivityJournalEditorBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        binding.root.insetNoTop()

        // Parse journal entry type from extras
        var type = DreamJournalEntry.EntryType.PLAIN_TEXT
        val journalTypeId = intent.getIntExtra("DREAM_JOURNAL_TYPE", DreamJournalEntry.EntryType.PLAIN_TEXT.ordinal)
        if (journalTypeId >= 0 && journalTypeId < DreamJournalEntry.EntryType.entries.size) {
            type = DreamJournalEntry.EntryType.entries[journalTypeId]
        }

        // Parse journal entry id from extras
        journalEntryId = intent.getIntExtra("JOURNAL_ENTRY_ID", -1)
        lifecycleScope.launch(Dispatchers.IO) {
            entry = if (journalEntryId == -1) {
                DreamJournalEntry.createDefault()
            } else {
                db.journalEntryDao.getEntryDataById(journalEntryId)
            }
            loadContent(type)
        }

        // Set handler to request confirmation whether or not to discard changes
        onBackPressed {
            promptDiscardChanges()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun loadContent(type: DreamJournalEntry.EntryType) {
        // Configure journal entry rating view
        val vwDreamRatingsEditor = DreamJournalEditorRatingView(entry).apply {
            onBackButtonListener = {
                binding.tlDjEditorLayout.selectTab(binding.tlDjEditorLayout.getTabAt(0))
            }
            onDoneButtonListener = {
                lifecycleScope.launch(Dispatchers.IO) {
                    storeEntry()
                }
            }
        }

        // Configure journal content editor view
        val vwDreamContentEditor = DreamJournalEditorContentView(entry, type).apply {
            onContinueButtonClicked = {
                binding.tlDjEditorLayout.selectTab(binding.tlDjEditorLayout.getTabAt(1))
                vwDreamRatingsEditor.updatePreview()
            }
            onCloseButtonClicked = {
                finish()
            }
        }

        // Configure view pager
        runOnUiThread {
            val vpAdapter = ViewPagerAdapter(supportFragmentManager, lifecycle)
            vpAdapter.addFragment(vwDreamContentEditor, "")
            vpAdapter.addFragment(vwDreamRatingsEditor, "")
            binding.vpDjEditor.setAdapter(vpAdapter)
            binding.vpDjEditor.setOffscreenPageLimit(vpAdapter.itemCount + 1)
            binding.vpDjEditor.registerOnPageChangeCallback(changeTab())
            binding.vpDjEditor.setUserInputEnabled(false)
            binding.vpDjEditor.setOnTouchListener { _, _ -> true }
            binding.tlDjEditorLayout.addOnTabSelectedListener(tabSelected())
        }
    }

    private suspend fun storeEntry() {
        // For simplicity remove all previously created references if they are present
        clearReferences(entry.journalEntry.entryId)

        // Insert / replace current journal entry in db
        val entryId = db.journalEntryDao.insert(entry.journalEntry).toInt()

        // Assign dream types to the current entry
        db.getJournalEntryIsTypeDao().insertAll(entry.getDreamTypeEntries(entryId))

        // Insert newly added tags into db
        db.journalEntryTagDao.insertAll(entry.journalEntryTags)

        // Query tags by description to get their id
        val journalEntryTags = db.journalEntryTagDao.getByDescription(entry.journalEntryTags.map {
            it.description
        }.toList())

        // Assign the tags to the entry by inserting them into db
        db.getJournalEntryHasTagDao().insertAll(journalEntryTags.map {
            JournalEntryHasTag(entryId, it.tagId)
        }.toList())

        // Insert all added audio locations
        db.getAudioLocationDao().insertAll(entry.getAudioLocations(entryId))

        // Delete removed audio recordings after successfully saving everything else
        entry.deleteAllRemovedAudioLocations()

        // Finish the editor activity
        isSaved = true
        runOnUiThread {
            setResult(RESULT_OK, Intent().apply {
                putExtra("entryId", entryId)
            })
            finish()
        }
    }

    private suspend fun clearReferences(id: Int) {
        if (id != -1) {
            db.journalEntryHasTagDao.deleteAll(id)
            db.journalEntryIsTypeDao.deleteAll(id)
            db.audioLocationDao.deleteAll(id)
        }
    }

    private fun promptDiscardChanges() {
        MaterialAlertDialogBuilder(this, R.style.Theme_LucidSourceKit_ThemedDialog)
            .setTitle("Discard changes")
            .setMessage("Do you really want to discard all changes")
            .setPositiveButton(getResources().getString(R.string.yes)) { _, _ -> finish() }
            .setNegativeButton(getResources().getString(R.string.no), null)
            .show()
    }

    override fun onStop() {
        // In case the entry creation was aborted and this was a new journal entry,
        // delete all created audio recordings to prevent dangling audio recordings
        if (!isSaved && journalEntryId == -1) {
            entry.audioLocations.forEach {
                File(it.audioPath).delete()
            }
        }
        // TODO: Also delete recordings from `Edit` mode when editing was aborted
        super.onStop()
    }

    private fun changeTab(): OnPageChangeCallback = object : OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            binding.tlDjEditorLayout.selectTab(binding.tlDjEditorLayout.getTabAt(position))
        }
    }

    private fun tabSelected(): OnTabSelectedListener = object : OnTabSelectedListener {
        override fun onTabUnselected(tab: TabLayout.Tab) { }
        override fun onTabReselected(tab: TabLayout.Tab) { }
        override fun onTabSelected(tab: TabLayout.Tab) {
            binding.vpDjEditor.currentItem = tab.position
        }
    }
}