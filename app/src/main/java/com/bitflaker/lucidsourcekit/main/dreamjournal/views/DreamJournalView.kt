package com.bitflaker.lucidsourcekit.main.dreamjournal.views

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.annotation.ColorInt
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.database.MainDatabase
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.results.DreamJournalEntry
import com.bitflaker.lucidsourcekit.databinding.FragmentMainJournalBinding
import com.bitflaker.lucidsourcekit.databinding.SheetQuestionnaireListBinding
import com.bitflaker.lucidsourcekit.main.questionnaire.views.CompletedQuestionnaireViewerActivity
import com.bitflaker.lucidsourcekit.main.questionnaire.views.QuestionnaireEditorActivity
import com.bitflaker.lucidsourcekit.main.questionnaire.views.QuestionnaireView
import com.bitflaker.lucidsourcekit.main.questionnaire.views.RecyclerViewFilledOutQuestionnaires
import com.bitflaker.lucidsourcekit.utils.Tools
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DateFormat
import androidx.core.view.isVisible
import com.bitflaker.lucidsourcekit.main.dreamjournal.AppliedFilter
import com.bitflaker.lucidsourcekit.main.dreamjournal.SortBy
import com.bitflaker.lucidsourcekit.main.dreamjournal.SortEntry
import com.bitflaker.lucidsourcekit.utils.attrColor
import com.bitflaker.lucidsourcekit.utils.insetDefault
import com.bitflaker.lucidsourcekit.utils.showToastLong
import kotlinx.coroutines.coroutineScope

class DreamJournalView : Fragment() {
    private lateinit var binding: FragmentMainJournalBinding
    private lateinit var rvAdapterDreamJournal: RecyclerViewAdapterDreamJournal
    private lateinit var db: MainDatabase
    private lateinit var fabOpen: Animation
    private lateinit var fabClose: Animation
    private lateinit var rotateForward: Animation
    private lateinit var rotateBackward: Animation
    private var autoOpenJournalTypeCreator: DreamJournalEntry.EntryType? = null
    private var questionnaireAdapter: RecyclerViewFilledOutQuestionnaires? = null
    private var questionnaireSheetBinding: SheetQuestionnaireListBinding? = null
    private var entryIdToUpdateQuestionnaires = -1
    private var isFabOpen = false
    private var sortBy = 0

    var journalEditorActivityResultLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        lifecycleScope.launch(Dispatchers.IO) {
            val entryId = result.data?.getIntExtra("entryId", -1)
            if (result.resultCode == Activity.RESULT_OK && entryId != null) {
                reloadEntryData(entryId)
            }
        }
    }

    private var editorLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        lifecycleScope.launch(Dispatchers.IO) {
            val id = result.data?.getIntExtra("COMPLETED_QUESTIONNAIRE_ID", -1)
            if (result.resultCode == Activity.RESULT_OK && id != null && id != -1) {
                if (entryIdToUpdateQuestionnaires != -1) {
                    reloadEntryData(entryIdToUpdateQuestionnaires)
                } else {
                    reloadEntryDataByCompleted(id)
                }

                questionnaireAdapter?.let {
                    val completed = db.getCompletedQuestionnaireDao().getDetailsById(id).blockingGet()
                    requireActivity().runOnUiThread {
                        it.addCompletedQuestionnaire(completed)
                        questionnaireSheetBinding!!.txtNoQuestionnairesTitle.visibility = View.GONE
                        questionnaireSheetBinding!!.txtNoQuestionnairesSubTitle.visibility = View.GONE
                        questionnaireSheetBinding = null
                    }
                }

                questionnaireAdapter = null
                entryIdToUpdateQuestionnaires = -1
            }
        }
    }

    private val sortEntryValues = listOf(
        SortEntry("Timestamp - newest first", SortBy.Timestamp, true),
        SortEntry("Timestamp - oldest first", SortBy.Timestamp, false),
        SortEntry("Title - A to Z", SortBy.Title, false),
        SortEntry("Title - Z to A", SortBy.Title, true),
        SortEntry("Description - A to Z", SortBy.Description, false),
        SortEntry("Description - Z to A", SortBy.Description, true)
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMainJournalBinding.inflate(inflater, container, false)
        binding.root.insetDefault()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = MainDatabase.getInstance(context)
        fabOpen = AnimationUtils.loadAnimation(context, R.anim.add_open)
        fabClose = AnimationUtils.loadAnimation(context, R.anim.add_close)
        rotateForward = AnimationUtils.loadAnimation(context, R.anim.rotate_forward)
        rotateBackward = AnimationUtils.loadAnimation(context, R.anim.rotate_backward)

        // Load all journal entries and init data
        val activity = requireActivity()
        lifecycleScope.launch(Dispatchers.IO) {
            val journalEntries = db.journalEntryDao.getAll()

            // Load the questionnaire count for each entry
            // TODO: Cache the questionnaire counts per day e.g. 1 day with 4 entries does not need to
            //       query the count 4 times
            journalEntries.forEach({ e ->
                val dayFrom = Tools.getMidnightMillis(e.journalEntry.timeStamp)
                val dayTo = dayFrom + 24 * 60 * 60 * 1000
                e.questionnaireCount = db.getCompletedQuestionnaireDao().getQuestionnaireCount(dayFrom, dayTo).blockingGet()
            })

            // Setup recyclerview and show stored journal entries
            activity.runOnUiThread {
                rvAdapterDreamJournal = RecyclerViewAdapterDreamJournal(this@DreamJournalView, activity, this@DreamJournalView, journalEntries)
                rvAdapterDreamJournal.onEntryCountChangedListener = { itemCount -> activity.runOnUiThread { setNoEntriesVisibility(itemCount) } }
                rvAdapterDreamJournal.onQuestionnaireAddClickListener = ::viewQuestionnaires
                binding.recyclerView.setLayoutManager(LinearLayoutManager(context))
                binding.recyclerView.setAdapter(rvAdapterDreamJournal)
                setNoEntriesVisibility(journalEntries.size)
            }
        }

        // Setup filter button
        binding.btnFilter.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                showFilterDialog()
            }
        }
        binding.btnFilterOff.setOnClickListener {
            binding.btnFilterOff.visibility = View.GONE
            setNoEntriesVisibility(rvAdapterDreamJournal.resetFilters())
        }

        // Setup sort button
        binding.btnSort.setOnClickListener {
            showSortDialog()
        }

        // Setup the FAB add button
        binding.btnAddJournalEntry.setOnClickListener { animateFab() }
        binding.fabText.setOnClickListener { showJournalCreator(DreamJournalEntry.EntryType.PLAIN_TEXT) }
        binding.fabForms.setOnClickListener { showJournalCreator(DreamJournalEntry.EntryType.FORMS_TEXT) }
        binding.fabQuestionnaire.setOnClickListener { showQuestionnaireCreator() }

        // Immediately open journal entry creator when requested
        autoOpenJournalTypeCreator?.let {
            // TODO when an entry was created after the editor was opened by the alarm quick action, the list of entries in the MainViewer does not get updated
            showJournalCreator(autoOpenJournalTypeCreator!!)
        }

        // Set handler for listing all questionnaires
        binding.crdAllQuestionnaires.setOnClickListener {
            startActivity(Intent(context, CompletedQuestionnaireViewerActivity::class.java))
        }
    }

    private fun viewQuestionnaires(entry: DreamJournalEntry) {
        val context = requireContext()
        val timestamp = entry.journalEntry.timeStamp
        val bsd = BottomSheetDialog(context, R.style.BottomSheetDialogStyle)
        val sBinding = SheetQuestionnaireListBinding.inflate(layoutInflater)
        bsd.setContentView(sBinding.root)

        // Load all completed questionnaires of selected day
        val dayFrom = Tools.getMidnightMillis(timestamp)
        val dayTo = dayFrom + 24 * 60 * 60 * 1000
        val completed = db.completedQuestionnaireDao.getByTimeFrame(dayFrom, dayTo).blockingGet()

        // Set selected date
        sBinding.txtQuestionnairesDate.text = DateFormat.getDateInstance(DateFormat.MEDIUM).format(timestamp)

        // Set visibility of placeholder in case no questionnaires have been completed on selected day
        val emptyVisibility = if (completed.isEmpty()) View.VISIBLE else View.GONE
        sBinding.txtNoQuestionnairesTitle.visibility = emptyVisibility
        sBinding.txtNoQuestionnairesSubTitle.visibility = emptyVisibility

        // Configure recycler view for viewing completed questionnaires of selected day
        val adapter = RecyclerViewFilledOutQuestionnaires(context, completed)
        adapter.onQuestionnaireClickListener = { completedId ->
            startActivity(Intent(context, QuestionnaireEditorActivity::class.java).apply {
                putExtra("COMPLETED_QUESTIONNAIRE_ID", completedId)
            })
        }
        sBinding.rcvQuestionnairesFilledOut.setLayoutManager(LinearLayoutManager(context))
        sBinding.rcvQuestionnairesFilledOut.setAdapter(adapter)
        questionnaireAdapter = adapter

        // Set handler for filling out new questionnaire on selected day
        sBinding.btnFillOutQuestionnaire.setOnClickListener {
            entryIdToUpdateQuestionnaires = entry.journalEntry.entryId
            editorLauncher.launch(Intent(context, QuestionnaireView::class.java).apply {
                putExtra("USE_SPECIFIC_DATE", Tools.getDateInMillis(timestamp))
            })
        }

        questionnaireSheetBinding = sBinding
        bsd.show()
    }

    private fun resetFilters(callback: Runnable? = null) {
        binding.btnFilterOff.setVisibility(View.GONE)
        setNoEntriesVisibility(rvAdapterDreamJournal.resetFilters(callback))
    }

    private suspend fun showFilterDialog() {
        val journalEntryTags = db.journalEntryTagDao.getAll()
        val availableTags = journalEntryTags.map { it.description }.toTypedArray()
        val dialog = FilterDialog(requireContext(), availableTags, rvAdapterDreamJournal.getCurrentFilter())
        dialog.setOnClickPositiveButton { _, _ ->
            val filters = dialog.filters
            requireActivity().runOnUiThread {
                if (!AppliedFilter.isEmptyFilter(filters)) {
                    setNoEntriesVisibility(rvAdapterDreamJournal.filter(filters))
                    binding.btnFilterOff.setVisibility(View.VISIBLE)
                } else if (binding.btnFilterOff.isVisible) {
                    resetFilters()
                }
            }
            dialog.dismiss()
        }
        dialog.show(getParentFragmentManager(), "filter-dialog")
    }

    private fun showSortDialog() {
        val entries = sortEntryValues.map(SortEntry::sortText).toTypedArray()
        MaterialAlertDialogBuilder(requireContext(), R.style.Theme_LucidSourceKit_ThemedDialog)
            .setTitle("Sort entries")
            .setSingleChoiceItems(entries, sortBy) { dialog, which ->
                sortBy = which
                val sortEntry = sortEntryValues[sortBy]
                rvAdapterDreamJournal.submitSortedEntries(sortEntry.sortBy, sortEntry.isDescending) {
                    binding.recyclerView.scrollToPosition(0)
                }
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun showQuestionnaireCreator() {
        animateFab()
        editorLauncher.launch(Intent(context, QuestionnaireView::class.java))
    }

    private fun setNoEntriesVisibility(itemCount: Int) {
        binding.txtNoJournalEntriesTitle.visibility = if (itemCount == 0) View.VISIBLE else View.GONE
        binding.txtNoJournalEntriesSubTitle.visibility = if (itemCount == 0) View.VISIBLE else View.GONE
    }

    fun showJournalCreator(type: DreamJournalEntry.EntryType) {
        animateFab()
        val intent = Intent(requireContext(), DreamJournalEditorView::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra("DREAM_JOURNAL_TYPE", type.ordinal)
        journalEditorActivityResultLauncher.launch(intent)
    }

    private fun animateFab() {
        val context = requireContext()
        @ColorInt val colorClosed = context.attrColor(R.attr.colorPrimaryContainer)
        @ColorInt val colorOnClosed = context.attrColor(R.attr.colorOnPrimaryContainer)
        @ColorInt val colorOpen = context.attrColor(R.attr.colorSurfaceContainerHigh)
        @ColorInt val colorOnOpen = context.attrColor(R.attr.colorOnSurface)

        if (isFabOpen) {
            binding.btnAddJournalEntry.startAnimation(rotateForward)
            binding.fabText.startAnimation(fabClose)
            binding.fabForms.startAnimation(fabClose)
            binding.fabQuestionnaire.startAnimation(fabClose)

            Tools.animateBackgroundTint(binding.btnAddJournalEntry, colorOpen, colorClosed, 300)
            Tools.animateImageTint(binding.btnAddJournalEntry, colorOnOpen, colorOnClosed, 300)
            binding.fabText.isClickable = false
            binding.fabForms.isClickable = false
            binding.fabQuestionnaire.isClickable = false
            isFabOpen = false
        } else {
            binding.btnAddJournalEntry.startAnimation(rotateBackward)
            binding.fabText.startAnimation(fabOpen)
            binding.fabForms.startAnimation(fabOpen)
            binding.fabQuestionnaire.startAnimation(fabOpen)

            Tools.animateBackgroundTint(binding.btnAddJournalEntry, colorClosed, colorOpen, 300)
            Tools.animateImageTint(binding.btnAddJournalEntry, colorOnClosed, colorOnOpen, 300)
            binding.fabText.isClickable = true
            binding.fabForms.isClickable = true
            binding.fabQuestionnaire.isClickable = true
            isFabOpen = true
        }
    }

    fun pageChanged() {
        isFabOpen = false
    }

    private suspend fun reloadEntryDataByCompleted(id: Int) = coroutineScope {
        val completed = db!!.getCompletedQuestionnaireDao().getById(id).blockingGet()
        val idsToReload = rvAdapterDreamJournal.getEntriesByDate(completed.timestamp)
        val entriesToReload = arrayListOf<DreamJournalEntry>()
        for (id in idsToReload) {
            entriesToReload.add(getDreamJournalEntry(id))
        }
        rvAdapterDreamJournal.updateDataForEntry(entriesToReload, null)
    }

    private suspend fun reloadEntryData(entryId: Int) {
        rvAdapterDreamJournal.updateDataForEntry(getDreamJournalEntry(entryId)) { insertedIndex ->
            if (insertedIndex != -1) {
                binding.recyclerView.scrollToPosition(insertedIndex)
            }
        }
    }

    private suspend fun getDreamJournalEntry(entryId: Int): DreamJournalEntry {
        val entry = db.journalEntryDao.getEntryDataById(entryId)
        val dayFrom = Tools.getMidnightMillis(entry.journalEntry.timeStamp)
        val dayTo = dayFrom + 24 * 60 * 60 * 1000
        entry.questionnaireCount = db.getCompletedQuestionnaireDao().getQuestionnaireCount(dayFrom, dayTo).blockingGet()
        return entry
    }

    fun openJournalEntry(entry: DreamJournalEntry, tryResetFilters: Boolean) {
        // Check if the entry complies with current filters otherwise reset filters
        if (tryResetFilters && !entry.compliesWithFilter(rvAdapterDreamJournal.getCurrentFilter())) {
            resetFilters { openJournalEntry(entry, false) }
            return
        }

        // Try to get the position of the entry
        val entryPosition = rvAdapterDreamJournal.indexOfEntry(entry)
        if (entryPosition == -1) {
            showToastLong(requireContext(), "Error finding journal entry")
            return
        }

        // Check if the position is already visible and open the entry if that is the case
        val lm = binding.recyclerView.layoutManager
        if (lm is LinearLayoutManager && isPositionVisible(lm, entryPosition)) {
            openDreamJournalEntry(entryPosition, entry)
            return
        }

        // Scroll to position and open entry after scrolling finished
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    binding.recyclerView.removeOnScrollListener(this)
                    openDreamJournalEntry(entryPosition, entry)
                }
            }
        })
        binding.recyclerView.smoothScrollToPosition(entryPosition)
    }

    private fun openDreamJournalEntry(entryPosition: Int, entry: DreamJournalEntry) {
        val holder = binding.recyclerView.findViewHolderForLayoutPosition(entryPosition) as RecyclerViewAdapterDreamJournal.MainViewHolder
        holder.binding.crdJournalEntryCard.setPressed(true)
        binding.recyclerView.postOnAnimationDelayed({
            holder.binding.crdJournalEntryCard.setPressed(false)
            rvAdapterDreamJournal.viewDreamJournalEntry(entry)
        }, 192)
    }

    private fun isPositionVisible(layoutManager: LinearLayoutManager, position: Int): Boolean {
        val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
        val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
        return firstVisiblePosition <= position && position <= lastVisiblePosition
    }

    fun showJournalCreatorWhenLoaded(type: DreamJournalEntry.EntryType?) {
        autoOpenJournalTypeCreator = type
    }
}