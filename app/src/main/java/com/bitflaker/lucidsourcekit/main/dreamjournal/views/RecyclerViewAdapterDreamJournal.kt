package com.bitflaker.lucidsourcekit.main.dreamjournal.views

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.icu.text.DateFormat
import android.media.MediaPlayer
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.children
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil.ItemCallback
import androidx.recyclerview.widget.RecyclerView
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.main.dreamjournal.rating.DreamMoods
import com.bitflaker.lucidsourcekit.main.dreamjournal.rating.DreamTypes
import com.bitflaker.lucidsourcekit.database.MainDatabase
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.AudioLocation
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.DreamClarity
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.DreamMood
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.SleepQuality
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.results.DreamJournalEntry
import com.bitflaker.lucidsourcekit.databinding.EntryJournalBinding
import com.bitflaker.lucidsourcekit.databinding.SheetJournalEntryBinding
import com.bitflaker.lucidsourcekit.main.dreamjournal.AppliedFilter
import com.bitflaker.lucidsourcekit.main.dreamjournal.views.RecyclerViewAdapterDreamJournal.MainViewHolder
import com.bitflaker.lucidsourcekit.main.dreamjournal.SortBy
import com.bitflaker.lucidsourcekit.utils.RecordingObjectTools
import com.bitflaker.lucidsourcekit.utils.Tools
import com.bitflaker.lucidsourcekit.utils.attrColor
import com.bitflaker.lucidsourcekit.utils.attrColorStateList
import com.bitflaker.lucidsourcekit.utils.dpToPx
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Stack
import java.util.function.Consumer

class RecyclerViewAdapterDreamJournal(
    private val lifecycleOwner: LifecycleOwner,
    private val activity: ComponentActivity,
    private val journalFragment: DreamJournalView?,
    entries: List<DreamJournalEntry>
) : RecyclerView.Adapter<MainViewHolder>() {
    class MainViewHolder(var binding: EntryJournalBinding) : RecyclerView.ViewHolder(binding.root) {
        var descriptionLineHeight = binding.txtDescription.lineHeight.toFloat()

        init {
            val cs = ConstraintSet()
            cs.clone(binding.clMainContent)
            cs.constrainMinHeight(binding.txtDescription.id, descriptionLineHeight.toInt() * 2)
            cs.applyTo(binding.clMainContent)

            binding.txtDescription.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                val maxLines = (binding.txtDescription.height / descriptionLineHeight).toInt()
                if (binding.txtDescription.lineCount != maxLines) {
                    binding.txtDescription.setLines(maxLines)
                    binding.txtDescription.text = binding.txtDescription.getText()
                }
            }
        }
    }

    private val db: MainDatabase = MainDatabase.getInstance(activity)
    private val recordingTools: RecordingObjectTools = RecordingObjectTools(activity)
    private val dfWeekday = SimpleDateFormat("EEEE", Locale.getDefault())
    private val dateFormat = DateFormat.getDateInstance(DateFormat.SHORT)
    private var isSortDescending = true
    private var currentSort = SortBy.Timestamp
    private var itemsBeforeFilter: MutableList<DreamJournalEntry>? = null
    private var currentFilter: AppliedFilter? = null
    private var mPlayer = MediaPlayer()
    private var currentPlayingImageButton: ImageButton? = null
    private val listChangedCallbacks = Stack<Runnable>()
    var onEntryCountChangedListener: ((Int) -> Unit)? = null
    var onQuestionnaireAddClickListener: ((DreamJournalEntry) -> Unit)? = null
    var onEntryClickListener: ((DreamJournalEntry) -> Unit)? = null
    var isCompactMode: Boolean = false
        set(value) {
            field = value
            notifyItemRangeChanged(0, itemCount)
        }
    private val differ = AsyncListDiffer(this, object : ItemCallback<DreamJournalEntry>() {
        override fun areItemsTheSame(oldItem: DreamJournalEntry, newItem: DreamJournalEntry): Boolean = oldItem.journalEntry.entryId == newItem.journalEntry.entryId
        override fun areContentsTheSame(oldItem: DreamJournalEntry, newItem: DreamJournalEntry): Boolean = oldItem == newItem
    })

    init {
        differ.submitList(entries)
        differ.addListListener { _, currentList ->
            notifyItemRangeChanged(0, currentList.size)
            while (listChangedCallbacks.isNotEmpty()) {
                listChangedCallbacks.pop().run()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
        val binding = EntryJournalBinding.inflate(LayoutInflater.from(activity), parent, false)
        return MainViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
        val currentList = differ.getCurrentList()
        val current = currentList[position]

        // Set top margin to 0 for the very first entry to prevent oddly large spacing
        holder.binding.llTopDayHeader.updateLayoutParams<LinearLayout.LayoutParams> {
            topMargin = if (position == 0) 0 else 14.dpToPx
        }

        // Show entry time if current position is the first entry for the day
        val currentEntryTime = Tools.calendarFromMillis(current.journalEntry.timeStamp)
        val isFirstEntryOfDay = entryIndexOfDay(position, currentList, currentEntryTime) == 0
        if (isFirstEntryOfDay) {
            holder.binding.txtJournalEntryFirstDateIndicatorName.text = dfWeekday.format(currentEntryTime.time)
            holder.binding.txtJournalEntryFirstDateIndicatorDate.text = dateFormat.format(currentEntryTime.time)
            holder.binding.llTopDayHeader.visibility = View.VISIBLE
        }
        else {
            holder.binding.llTopDayHeader.visibility = View.GONE
        }

        // In case the view is in compact mode (i.e. on the main overview) hide the header
        if (isCompactMode) {
            holder.binding.llTopDayHeader.visibility = View.GONE
        }

        // Add all special dream icons
        holder.binding.llTitleIcons.removeAllViews()
        holder.binding.llTitleIcons.visibility = if (current.dreamTypes.isEmpty()) View.GONE else View.VISIBLE
        current.dreamTypes.forEach {
            holder.binding.llTitleIcons.addView(getSpecialTypeIcon(it))
        }

        // Set title and description or placeholder
        val hasTextContent = !current.journalEntry.description.isNullOrBlank()
        holder.binding.txtTitle.text = current.journalEntry.title
        holder.binding.txtDescription.text = if (hasTextContent) current.journalEntry.description else "This dream journal entry contains no text. How about adding some content now?"
        holder.binding.txtDescription.setTypeface(null, if (hasTextContent) Typeface.NORMAL else Typeface.ITALIC)
        holder.binding.txtDescription.setTextColor(activity.attrColor(if (hasTextContent) R.attr.secondaryTextColor else R.attr.tertiaryTextColor))

        // Set audio recordings count
        holder.binding.txtRecordingsCount.text = String.format(Locale.getDefault(), "%d", current.audioLocations.size)
        holder.binding.txtRecordingsCount.visibility = if (current.audioLocations.isEmpty()) View.GONE else View.VISIBLE

        // Show tag list
        holder.binding.llTagsHolder.removeAllViews()
        setTagList(holder.binding, 1, current.stringTags, activity)

        // Set handler for opening selected journal entry
        holder.binding.crdJournalEntryCard.setOnClickListener {
            val clickListener = onEntryClickListener ?: ::viewDreamJournalEntry
            clickListener.invoke(current)
        }

        // Set count for questionnaires of current day
        val hasQuestionnaires = current.questionnaireCount > 0
        holder.binding.btnFilledOutQuestionnaires.text = if (current.questionnaireCount != 1) {
            String.format(Locale.getDefault(), "%d Questionnaires", current.questionnaireCount)
        } else {
            String.format(Locale.getDefault(),"%d Questionnaire", current.questionnaireCount)
        }

        // Set questionnaire counter styles and click listener
        holder.binding.btnFilledOutQuestionnaires.setTextColor(activity.attrColor(if (hasQuestionnaires) R.attr.colorPrimary else R.attr.tertiaryTextColor))
        holder.binding.btnFilledOutQuestionnaires.setIconTint(activity.attrColorStateList(if (hasQuestionnaires) R.attr.colorPrimary else R.attr.tertiaryTextColor))
        holder.binding.btnFilledOutQuestionnaires.setOnClickListener {
            onQuestionnaireAddClickListener?.invoke(current)
        }
    }

    private fun getSpecialTypeIcon(dreamType: DreamTypes): ImageView? = when (dreamType) {
        DreamTypes.Lucid -> generateIconHighlight(R.drawable.rounded_award_star_24)
        DreamTypes.Nightmare -> generateIconHighlight(R.drawable.rounded_sentiment_stressed_24)
        DreamTypes.FalseAwakening -> generateIconHighlight(R.drawable.rounded_cinematic_blur_24)
        DreamTypes.SleepParalysis -> generateIconHighlight(R.drawable.ic_baseline_accessibility_new_24)
        DreamTypes.Recurring -> generateIconHighlight(R.drawable.ic_round_loop_24)
        DreamTypes.None -> null
    }

    private fun generateIconHighlight(iconId: Int): ImageView = ImageView(activity).apply {
        setImageResource(iconId)
        imageTintList = activity.attrColorStateList(R.attr.secondaryTextColor)
        layoutParams = LinearLayout.LayoutParams(20.dpToPx, 20.dpToPx)
    }

    fun viewDreamJournalEntry(current: DreamJournalEntry) {
        val bsd = BottomSheetDialog(activity, R.style.BottomSheetDialogStyle)
        val binding = SheetJournalEntryBinding.inflate(LayoutInflater.from(activity))
        bsd.setContentView(binding.root)

        val df = DateFormat.getDateInstance(DateFormat.SHORT)
        val tf = DateFormat.getTimeInstance(DateFormat.SHORT)

        // Set entry timestamp, title and description values
        val entryDate = Tools.calendarFromMillis(current.journalEntry.timeStamp).getTime()
        binding.txtEntryTimestamp.text = String.format(Locale.getDefault(), "%s • %s", df.format(entryDate), tf.format(entryDate.time))
        binding.txtEntryTitle.text = current.journalEntry.title
        binding.txtEntryDreamContent.text = current.journalEntry.description

        // Show/Hide special dream types
        binding.btnIconRecurring.isVisible = current.hasSpecialType("REC")
        binding.btnIconLucid.isVisible = current.hasSpecialType("LCD")
        binding.btnIconSleepParalysis.isVisible = current.hasSpecialType("SPL")
        binding.btnIconFalseAwakening.isVisible = current.hasSpecialType("FAW")
        binding.btnIconNightmare.isVisible = current.hasSpecialType("NTM")

        // Show all recordings
        binding.llRecordingsContainer.isVisible = current.audioLocations.isNotEmpty()
        for (recData in current.audioLocations) {
            binding.llRecordingsContainer.addView(generateRecordingsPlayer(recData))
        }

        // Show all tags
        binding.fblTags.isVisible = current.journalEntryTags.isNotEmpty()
        for (tag in current.stringTags) {
            binding.fblTags.addView(generateTagView(tag))
        }

        // Set values to display for ratings
        binding.rpDreamMood.label = "DREAM MOOD"
        binding.rpDreamMood.setValue(DreamMood.valueOf(current.journalEntry.moodId).toFloat(), 4f)
        binding.rpDreamMood.setBackgroundAttrColor(R.attr.colorSurfaceContainerHigh)
        binding.rpDreamMood.icon = Tools.resolveIconDreamMood(activity, current.journalEntry.moodId)
        binding.rpDreamMood.invalidate()

        binding.rpDreamClarity.label = "DREAM CLARITY"
        binding.rpDreamClarity.setValue(DreamClarity.valueOf(current.journalEntry.clarityId).toFloat(), 3f)
        binding.rpDreamClarity.setBackgroundAttrColor(R.attr.colorSurfaceContainerHigh)
        binding.rpDreamClarity.icon = Tools.resolveIconDreamClarity(activity, current.journalEntry.clarityId)
        binding.rpDreamClarity.invalidate()

        binding.rpSleepQuality.label = "SLEEP QUALITY"
        binding.rpSleepQuality.setValue(SleepQuality.valueOf(current.journalEntry.qualityId).toFloat(), 3f)
        binding.rpSleepQuality.setBackgroundAttrColor(R.attr.colorSurfaceContainerHigh)
        binding.rpSleepQuality.icon = Tools.resolveIconSleepQuality(activity, current.journalEntry.qualityId)
        binding.rpSleepQuality.invalidate()

        // Set handler to delete the journal entry
        binding.btnDeleteEntry.setOnClickListener {
            showRequestDeleteConfirmation(current, bsd)
        }

        // Set handler to edit the journal entry
        binding.btnEditEntry.setOnClickListener {
            journalFragment?.journalEditorActivityResultLauncher?.launch(Intent(activity, DreamJournalEditorView::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("JOURNAL_ENTRY_ID", current.journalEntry.entryId)
            })
            bsd.dismiss()
        }

        bsd.show()
    }

    private fun showRequestDeleteConfirmation(current: DreamJournalEntry, bsd: BottomSheetDialog) {
        MaterialAlertDialogBuilder(activity, R.style.Theme_LucidSourceKit_ThemedDialog)
            .setTitle(activity.resources.getString(R.string.entry_delete_header))
            .setMessage(activity.resources.getString(R.string.entry_delete_message))
            .setPositiveButton(activity.resources.getString(R.string.yes)) { _, _ ->
                lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    deleteJournalEntry(current.journalEntry.entryId)
                    activity.runOnUiThread {
                        bsd.dismiss()
                    }
                }
            }
            .setNegativeButton(activity.resources.getString(R.string.no), null)
            .show()
    }

    private suspend fun deleteJournalEntry(entryId: Int) {
        // Get entry and remove it from the database
        val entry = db.getJournalEntryDao().getEntryById(entryId)
        db.getJournalEntryDao().delete(entry)

        // Update the current entries and the unfiltered entries
        val removed = differ.getCurrentList().filter { it.journalEntry.entryId != entryId }
        itemsBeforeFilter = itemsBeforeFilter?.filter { it.journalEntry.entryId != entryId }?.toMutableList()
        differ.submitList(removed)
        onEntryCountChangedListener?.invoke(removed.size)
    }

    private fun generateRecordingsPlayer(audioLocation: AudioLocation): View {
        val entryContainer = recordingTools.generateContainerLayout()

        // Generate and add play button
        val playButton = recordingTools.generatePlayButton()
        playButton.setOnClickListener {
            handlePlayPauseMediaPlayer(audioLocation, playButton)
        }
        entryContainer.addView(playButton)

        // Generate and add labels
        val labelsContainer = recordingTools.generateLabelsContainer()
        labelsContainer.addView(recordingTools.generateHeading())
        labelsContainer.addView(recordingTools.generateTimestamp(audioLocation))
        entryContainer.addView(labelsContainer)

        // Generate and add duration
        entryContainer.addView(recordingTools.generateDuration(audioLocation, true))

        return entryContainer
    }

    private fun handlePlayPauseMediaPlayer(currentRecording: AudioLocation, playButton: ImageButton) {
        if (mPlayer.isPlaying && currentPlayingImageButton === playButton) {
            mPlayer.pause()
            playButton.setImageResource(R.drawable.ic_baseline_play_arrow_24)
        } else if (!mPlayer.isPlaying && currentPlayingImageButton === playButton) {
            mPlayer.start()
            playButton.setImageResource(R.drawable.ic_baseline_pause_24)
        } else if (mPlayer.isPlaying) {
            stopCurrentPlayback()
            playButton.setImageResource(R.drawable.ic_baseline_pause_24)
            setupAudioPlayer(currentRecording.audioPath)
            currentPlayingImageButton = playButton
        } else {
            playButton.setImageResource(R.drawable.ic_baseline_pause_24)
            setupAudioPlayer(currentRecording.audioPath)
            currentPlayingImageButton = playButton
        }
    }

    private fun setupAudioPlayer(audioFile: String?) {
        try {
            mPlayer.reset()
            mPlayer.setDataSource(audioFile)
            mPlayer.setOnCompletionListener { stopCurrentPlayback() }
            mPlayer.prepare()
            mPlayer.start()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun stopCurrentPlayback() {
        currentPlayingImageButton?.setImageResource(R.drawable.ic_baseline_play_arrow_24)
        currentPlayingImageButton = null
        mPlayer.stop()
    }

    override fun getItemCount(): Int = differ.getCurrentList().size

    fun filter(filter: AppliedFilter): Int {
        currentFilter = filter

        // Store items before ever starting to filter to save original unfiltered state
        if (itemsBeforeFilter == null) {
            itemsBeforeFilter = differ.getCurrentList().toMutableList()
        }

        // Submit list of entries complying with filters
        val filteredEntries = itemsBeforeFilter!!.filter { it.compliesWithFilter(filter) }
        differ.submitList(filteredEntries)
        return filteredEntries.size
    }

    fun resetFilters(): Int {
        val itemsCount = itemsBeforeFilter!!.size
        submitSortedEntries(currentSort, isSortDescending, itemsBeforeFilter!!)
        currentFilter = null
        itemsBeforeFilter = null
        return itemsCount
    }

    fun resetFilters(callback: Runnable?): Int {
        listChangedCallbacks.push(callback)
        return resetFilters()
    }

    fun submitSortedEntries(sortBy: SortBy, descending: Boolean, commitCallback: ((List<DreamJournalEntry>) -> Unit)? = null) {
        submitSortedEntries(sortBy, descending, differ.getCurrentList().toMutableList(), commitCallback)
    }

    fun submitSortedEntries(sortBy: SortBy, descending: Boolean, entries: List<DreamJournalEntry>, commitCallback: ((List<DreamJournalEntry>) -> Unit)? = null) {
        currentSort = sortBy
        isSortDescending = descending

        // Sort list by type and in correct order
        val sorted = when {
            !descending && sortBy == SortBy.Timestamp -> entries.sortedBy { it.journalEntry.timeStamp }
            descending && sortBy == SortBy.Timestamp -> entries.sortedByDescending { it.journalEntry.timeStamp }
            !descending && sortBy == SortBy.Title -> entries.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.journalEntry.title })
            descending && sortBy == SortBy.Title -> entries.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.journalEntry.title })
            !descending && sortBy == SortBy.Description -> entries.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.journalEntry.description })
            descending && sortBy == SortBy.Description -> entries.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.journalEntry.description })
            else -> throw IllegalStateException("Unable to sort by unknown type")
        }

        // Commit the list with or without the optional commit callback
        if (commitCallback != null) {
            differ.submitList(sorted, {
                commitCallback.invoke(sorted)
            })
        } else {
            differ.submitList(sorted)
        }
    }

    // TODO: Find better way for enum to be used here
    fun getCurrentFilter(): AppliedFilter = currentFilter ?: AppliedFilter(
        ArrayList(),
        ArrayList(),
        DreamMoods.None,
        com.bitflaker.lucidsourcekit.main.dreamjournal.rating.DreamClarity.None,
        com.bitflaker.lucidsourcekit.main.dreamjournal.rating.SleepQuality.None
    )

    fun updateDataForEntry(newData: DreamJournalEntry, scrollToPositionCallback: Consumer<Int>) {
        updateDataForEntry(listOf(newData), scrollToPositionCallback)
    }

    fun updateDataForEntry(newData: List<DreamJournalEntry>, scrollToPositionCallback: Consumer<Int>?) {
        val currentEntries = differ.getCurrentList().toMutableList()
        itemsBeforeFilter?.let {
            for (entry in newData) {
                addOrUpdate(entry, it)
            }
        }

        // Add and update all current entries
        var anyAdded = false
        for (entry in newData) {
            anyAdded = anyAdded || addOrUpdate(entry, currentEntries) == -1
        }

        // Notify about change of entry count
        if (anyAdded) {
            onEntryCountChangedListener?.invoke(currentEntries.size)
        }

        // Submit list and invoke callback
        submitSortedEntries(currentSort, isSortDescending, currentEntries) { sorted ->
            if (newData.size > 1 || !anyAdded) return@submitSortedEntries
            val sortedIndex = sorted.indexOf(newData[0])
            scrollToPositionCallback?.accept(sortedIndex)
        }
    }

    private fun generateTagView(content: String): TextView {
        val dp8 = 8.dpToPx
        return TextView(activity).apply {
            setTextColor(activity.attrColorStateList(R.attr.primaryTextColor))
            setPadding(dp8, dp8 / 2, dp8, dp8 / 2)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            background = ResourcesCompat.getDrawable(activity.resources, R.drawable.small_rounded_rectangle, activity.theme)
            backgroundTintList = activity.attrColorStateList(R.attr.colorSurfaceContainerHigh)
            isSingleLine = true
            text = content
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, dp8 / 4, dp8 / 2, dp8 / 4)
            }
        }
    }

    fun indexOfEntry(entry: DreamJournalEntry): Int = differ.getCurrentList().indexOf(entry)

    fun getEntriesByDate(timestamp: Long): List<Int> {
        val dayFrom = Tools.getMidnightMillis(timestamp)
        val dayTo = dayFrom + 24 * 60 * 60 * 1000
        val entries = getEntriesInTimeFrame(differ.getCurrentList(), dayFrom, dayTo)
        itemsBeforeFilter?.let {
            entries.addAll(getEntriesInTimeFrame(it, dayFrom, dayTo))
            return entries.distinct()
        }
        return entries
    }

    fun getEntriesInTimeFrame(entries: List<DreamJournalEntry>, dayFrom: Long, dayTo: Long): MutableList<Int> = entries
            .filter { it.journalEntry.timeStamp >= dayFrom && it.journalEntry.timeStamp < dayTo }
            .map { it.journalEntry.entryId }
            .toMutableList()

    fun calculateAdditionalContainerPadding(binding: EntryJournalBinding): Int {
        // Seems to ignore the compound drawable at the start, therefore manually add it below
        var recordingsWidth = getViewWidth(binding.txtRecordingsCount)
        if (binding.txtRecordingsCount.isGone) {
            // TODO: find a way not to hardcode this: get the sum of all horizontal compound drawable widths (.intrinsicWidth(), .bounds().width() are all 0 at this point)
            recordingsWidth += 20.dpToPx
        }
        return recordingsWidth + getViewWidth(binding.llTitleIcons)
    }

    fun setTagList(binding: EntryJournalBinding, elevationLevel: Int, tagItems: List<String>, activity: Activity) {
        val container = binding.llTagsHolder
        val context = container.context
        val padding = calculateAdditionalContainerPadding(binding)

        // Check if there are any tags at all
        if (tagItems.isEmpty()) {
            container.addView(generateTagInfo(context, "No tags available", elevationLevel))
            return
        }

        // Get static values
        val color = if (elevationLevel == 0) R.attr.colorSurfaceContainer else R.attr.colorSurfaceContainerHigh
        val layoutWidth = getContainerWidth(container, activity, padding)
        val dividerSpacing = 4.dpToPx

        // Generate tag items
        var totalTagsWidth = -dividerSpacing
        for (i in tagItems.indices) {
            val tag = generateTagView(context, tagItems[i], color, elevationLevel)
            val newWidth = totalTagsWidth + getViewWidth(tag) + dividerSpacing

            // Check if the tag would overflow. if so, make place and collapse the rest of the tags
            if (newWidth > layoutWidth) {
                var collapsedTagCount = generateCollapsedTagCountView(context, tagItems.size - i, elevationLevel)
                var removeCount = 0

                // Check if updated collapsed tag would overflow. If so, remove last view until enough space is available
                while (totalTagsWidth + getViewWidth(collapsedTagCount) + dividerSpacing > layoutWidth) {
                    val lastView = container.children.last()
                    container.removeView(lastView)
                    totalTagsWidth -= getViewWidth(lastView) + dividerSpacing

                    // Update the view with the incremented number to be able to get its new layout size
                    collapsedTagCount = generateCollapsedTagCountView(context, tagItems.size - i + ++removeCount, elevationLevel)
                }

                // Finally add the indicator for collapsed tag count
                container.addView(collapsedTagCount)
                break
            }

            totalTagsWidth = newWidth
            container.addView(tag)
        }
    }

    private fun getContainerWidth(container: ViewGroup, activity: Activity, additionalPadding: Int): Int {
        val totalWidth = activity.window.decorView.measuredWidth
        var totalHorizontalMargins = getHorizontalSpacing(container)
        var viewParent = container.parent
        while (viewParent is View) {
            val view = viewParent as View
            totalHorizontalMargins += getHorizontalSpacing(view)
            viewParent = view.parent
        }
        return totalWidth - totalHorizontalMargins - additionalPadding
    }

    private fun generateCollapsedTagCountView(context: Context, count: Int, elevationLevel: Int): TextView {
        return generateTagInfo(context, String.format(Locale.getDefault(), "+ %d", count), elevationLevel)
    }

    private fun generateTagInfo(context: Context, content: String, elevationLevel: Int): TextView {
        val dp8 = 8.dpToPx
        val background = if (elevationLevel == 0) R.drawable.round_border_dashed_surface else R.drawable.round_border_dashed
        val foreground = if (elevationLevel == 0) R.attr.tertiaryTextColor else R.attr.secondaryTextColor
        return TextView(context).apply {
            setBackgroundResource(background)
            setPadding(dp8, dp8 / 2, dp8, dp8 / 2)
            setTextColor(context.attrColor(foreground))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setGravity(Gravity.CENTER_VERTICAL)
            text = content
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }
    }

    private fun getViewWidth(view: View): Int {
        if (view.isGone) return 0
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        return view.measuredWidth
    }

    private fun generateTagView(context: Context, content: String, color: Int, elevationLevel: Int): TextView {
        val dp8 = 8.dpToPx
        val textColor = if (elevationLevel == 0) R.attr.secondaryTextColor else R.attr.primaryTextColor
        return TextView(context).apply {
            setTextColor(context.attrColorStateList(textColor))
            setPadding(dp8, dp8 / 2, dp8, dp8 / 2)
            isSingleLine = true
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            text = content
            background = ResourcesCompat.getDrawable(context.resources, R.drawable.small_rounded_rectangle, context.theme)
            backgroundTintList = context.attrColorStateList(color)
        }
    }

    private fun entryIndexOfDay(position: Int, currentList: List<DreamJournalEntry>, current: Calendar): Int {
        val cal = Calendar.getInstance()
        val currentYear = current.get(Calendar.YEAR)
        val currentDayOfYear = current.get(Calendar.DAY_OF_YEAR)
        for (i in 0..<position) {
            cal.timeInMillis = currentList[position - i - 1].journalEntry.timeStamp
            if (currentDayOfYear != cal.get(Calendar.DAY_OF_YEAR) || currentYear != cal.get(Calendar.YEAR)) {
                return i
            }
        }
        return position
    }

    private fun getHorizontalSpacing(view: View): Int {
        val paddings = view.paddingLeft + view.paddingRight
        val layoutParams = (view.layoutParams as? MarginLayoutParams) ?: return paddings
        return layoutParams.leftMargin + layoutParams.rightMargin + paddings
    }

    private fun addOrUpdate(newData: DreamJournalEntry, currentEntries: MutableList<DreamJournalEntry>): Int {
        val index = currentEntries.indexOfFirst { it.journalEntry.entryId == newData.journalEntry.entryId }
        if (index == -1) {
            currentEntries.add(newData)
        } else {
            currentEntries[index] = newData
        }
        return index
    }

    // TODO: Ensure this is always called when the containing view is stopped
    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        mPlayer.release()
    }
}
