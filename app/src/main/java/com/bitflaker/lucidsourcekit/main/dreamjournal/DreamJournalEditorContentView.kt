package com.bitflaker.lucidsourcekit.main.dreamjournal

import android.Manifest.permission
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.res.ColorStateList
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.database.MainDatabase
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.AudioLocation
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntryTag
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.results.DreamJournalEntry
import com.bitflaker.lucidsourcekit.databinding.FragmentJournalEditorContentBinding
import com.bitflaker.lucidsourcekit.databinding.SheetJournalRecordingBinding
import com.bitflaker.lucidsourcekit.databinding.SheetJournalTagsEditorBinding
import com.bitflaker.lucidsourcekit.databinding.SheetJournalTimestampBinding
import com.bitflaker.lucidsourcekit.utils.RecordingObjectTools
import com.bitflaker.lucidsourcekit.utils.Tools
import com.bitflaker.lucidsourcekit.utils.attrColorStateList
import com.bitflaker.lucidsourcekit.utils.dpToPx
import com.bitflaker.lucidsourcekit.utils.isPermissionGranted
import com.bitflaker.lucidsourcekit.utils.showToastLong
import com.bitflaker.lucidsourcekit.utils.singleLine
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.text.DateFormat
import java.util.Calendar
import java.util.UUID

const val REQUEST_AUDIO_PERMISSION_CODE: Int = 1

class DreamJournalEditorContentView(private val entry: DreamJournalEntry, private val entryType: DreamJournalEntry.EntryType) : Fragment() {
    private lateinit var db: MainDatabase
    private lateinit var recordingTools: RecordingObjectTools
    private lateinit var binding: FragmentJournalEditorContentBinding
    private lateinit var allAvailableTags: List<String>
    private lateinit var mRecorder: MediaRecorder
    private val mPlayer = MediaPlayer()
    private val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)
    private val timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT)
    private val dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
    private var currentPlayingImageButton: ImageButton? = null
    private var isRecordingRunning = false
    private var isRecorderConfigured = false
    private var currentRecording: AudioLocation? = null
    var onContinueButtonClicked: (() -> Unit)? = null
    var onCloseButtonClicked: (() -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentJournalEditorContentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = MainDatabase.getInstance(context)
        recordingTools = RecordingObjectTools(requireContext())

        mRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(requireContext())
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        // Set text indicator for current entry timestamp
        binding.btnDjDate.text = dateTimeFormat.format(Calendar.getInstance().timeInMillis)

        // Generate form content editor in case of form entry type
        if (entryType == DreamJournalEntry.EntryType.FORMS_TEXT) {
            binding.flxDjFormDream.visibility = View.VISIBLE
            binding.txtDjDescriptionDream.visibility = View.GONE
            generateFormContent()
        }

        // Set handler for back button
        binding.btnDjCloseEditor.setOnClickListener {
            promptDiscardChanges()
        }

        // Load available tags and setup tag editor
        lifecycleScope.launch(Dispatchers.IO) {
            allAvailableTags = db.journalEntryTagDao.getAllTagTexts()
            requireActivity().runOnUiThread {
                binding.btnDjAddTag.setOnClickListener {
                    showTagEditor()
                }
            }
        }

        // Set handler for journal entry time editor
        binding.btnDjDate.setOnClickListener {
            showTimestampEditor()
        }

        // Set handler for recordings manager
        binding.btnDjAddRecording.setOnClickListener {
            showRecordingManager()
        }

        // Set handler for continue button
        binding.btnDjContinueToRatings.setOnClickListener {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(requireView().windowToken, 0)
            onContinueButtonClicked?.invoke()
        }

        // Set title value and add listener to update object value TODO: See note in `generateEditText()`
        binding.txtDjTitleDream.singleLine()
        binding.txtDjTitleDream.setText(entry.journalEntry.title)
        binding.txtDjTitleDream.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) { }
            override fun afterTextChanged(s: Editable) { }
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                entry.journalEntry.title = s.toString()
            }
        })

        // Set description value and add listener to update object value TODO: See note in `generateEditText()`
        binding.txtDjDescriptionDream.setText(entry.journalEntry.description)
        binding.txtDjDescriptionDream.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) { }
            override fun afterTextChanged(s: Editable) { }
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                entry.journalEntry.description = s.toString()
            }
        })

        // If the title is empty, focus the title on the next frame TODO: Probably should be adjustable to allow for description focus or none in preferences
        if (TextUtils.isEmpty(entry.journalEntry.title)) {
            Handler(Looper.getMainLooper()).postDelayed({
                binding.txtDjTitleDream.requestFocus()
                val mgr = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                mgr.showSoftInput(binding.txtDjTitleDream, InputMethodManager.SHOW_IMPLICIT)
            }, 100)
        }
    }

    private fun showTimestampEditor() {
        val bsd = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogStyle)
        val sBinding = SheetJournalTimestampBinding.inflate(layoutInflater)
        bsd.setContentView(sBinding.root)

        // Default to current date time
        val date = Tools.calendarFromMillis(entry.journalEntry.timeStamp).time

        // Configure date part button
        sBinding.btnDjChangeDate.text = dateFormat.format(date)
        sBinding.btnDjChangeDate.setOnClickListener {
            val date = Tools.calendarFromMillis(entry.journalEntry.timeStamp)
            DatePickerDialog(requireContext(), { _, year, monthOfYear, dayOfMonth -> date.set(Calendar.YEAR, year)
                    date.set(Calendar.MONTH, monthOfYear)
                    date.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    entry.journalEntry.timeStamp = date.getTimeInMillis()
                    sBinding.btnDjChangeDate.text = dateFormat.format(date.getTime())
                    binding.btnDjDate.text = dateTimeFormat.format(date.getTime())
                },
                date.get(Calendar.YEAR),
                date.get(Calendar.MONTH),
                date.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Configure time part button
        sBinding.btnDjChangeTime.text = timeFormat.format(date)
        sBinding.btnDjChangeTime.setOnClickListener {
            val time = Tools.calendarFromMillis(entry.journalEntry.timeStamp)
            TimePickerDialog(context, { _, hourFrom, minuteFrom ->
                    time.set(Calendar.HOUR_OF_DAY, hourFrom)
                    time.set(Calendar.MINUTE, minuteFrom)
                    entry.journalEntry.timeStamp = time.getTimeInMillis()
                    sBinding.btnDjChangeTime.text = timeFormat.format(time.getTime())
                    binding.btnDjDate.text = dateTimeFormat.format(time.getTime())
                },
                time.get(Calendar.HOUR_OF_DAY),
                time.get(Calendar.MINUTE),
                true
            ).show()
        }

        bsd.show()
    }

    private fun generateFormContent() {
        // TODO: make this text editable for users e.g. in preferences
        val formsTemplate = "I was in <<EDIT>> and I saw <<EDIT>>. The daytime was <<EDIT>>. Characters in my dream were <<EDIT>>. I was <<EDIT>>. The characters in my dream behaved <<EDIT>>."
            .split("<<EDIT>>")
            .filter { it.isNotEmpty() && it.isNotBlank() }
            .toTypedArray()

        for (i in formsTemplate.indices) {
            val part = formsTemplate[i]

            // Split the current part when it starts with the end of a sentence (e.g. `? Something ...`
            // would become [`?`, `Something ...`] in order to try and keep the line ending symbol close
            // to the end of the edit text field)
            var sentences = arrayOf(part)
            if (startsWithSentenceEnd(part)) {
                sentences = separateAtSentenceEnd(part)
            }

            // Generate TextViews for all sentence parts
            for (sentence in sentences) {
                binding.flxDjFormDream.addView(generateTextView(sentence))
            }

            // Always append an additional EditText to the end for additional notes
            if (i < formsTemplate.size - 1) {
                binding.flxDjFormDream.addView(generateEditText())
            }
        }
    }

    private fun generateEditText(): EditText {
        val dpm5 = -5.dpToPx
        val editText = EditText(context).apply {
            setLayoutParams(LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                setMargins(0, dpm5, 0, dpm5)
            })
            setMinWidth(70.dpToPx)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        }

        // This will lead to a lot of events while typing the description
        // (after every character written), but it works very smoothly even for long
        // texts and therefore this implementation seems to be fine for now
        // TODO: Is it really necessary to load the description value after every text change?
        //       Wouldn't an approach of only loading whenever the preview has to be updated
        //       be better in any scenario?
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) { }
            override fun afterTextChanged(s: Editable) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                entry.journalEntry.description = getFormResult()
            }
        })

        return editText
    }

    private fun generateTextView(sentence: String?): TextView = TextView(context).apply {
        setLayoutParams(LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        text = sentence
    }

    private fun separateAtSentenceEnd(sentence: String): Array<String> {
        val splitPos = getFirstSentenceEnd(sentence)

        // In case the line ending character is somewhere within the sentence part,
        // split at the first occurrence and return the parts
        if (splitPos < sentence.length) {
            return arrayOf(
                sentence.substring(0, splitPos + 1),
                sentence.substring(splitPos + 1)
            )
        }

        return arrayOf(sentence)
    }

    private fun getFirstSentenceEnd(sentence: String): Int {
        for (i in 0..<sentence.length) {
            if (!isSentenceEndSymbol(sentence[i])) {
                return i
            }
        }
        return sentence.length
    }

    private fun startsWithSentenceEnd(sentence: String): Boolean = sentence.isNotEmpty() && isSentenceEndSymbol(sentence[0])

    private fun isSentenceEndSymbol(c: Char): Boolean = charArrayOf('.', '!', '?').contains(c)

    fun getFormResult(): String {
        val sb = StringBuilder()
        binding.flxDjFormDream.children.forEach {
            sb.append((it as TextView).text)
        }
        return sb.toString()
    }

    private fun promptDiscardChanges() {
        MaterialAlertDialogBuilder(requireContext(), R.style.Theme_LucidSourceKit_ThemedDialog)
            .setTitle("Discard changes")
            .setMessage("Do you really want to discard all changes")
            .setPositiveButton(resources.getString(R.string.yes)) { _, _ ->
                entry.removeAllAddedRecordings()
                onCloseButtonClicked?.invoke()
            }
            .setNegativeButton(resources.getString(R.string.no), null)
            .show()
    }

    private fun showRecordingManager() {
        val activity = requireActivity()

        // Request recording permission in case it is not yet granted
        if (!activity.applicationContext.isPermissionGranted(permission.RECORD_AUDIO)) {
//            sBinding.llDjRecsList.visibility = View.VISIBLE
//            sBinding.rlDjRecordingAudio.visibility = View.GONE
            ActivityCompat.requestPermissions(activity, arrayOf(permission.RECORD_AUDIO), REQUEST_AUDIO_PERMISSION_CODE)
            return
        }

        val bsd = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogStyle)
        val sBinding = SheetJournalRecordingBinding.inflate(layoutInflater)
        bsd.setContentView(sBinding.root)

        // List stored recordings or show there are no recordings yet
        sBinding.txtDjNoRecsFound.visibility = if (entry.audioLocations.isEmpty()) View.VISIBLE else View.GONE
        entry.audioLocations.forEach { recData ->
            sBinding.llDjRecsEntryList.addView(generateAudioEntry(recData, sBinding.txtDjNoRecsFound))
        }

        // Set handler to start recording
        sBinding.btnDjRecordAudio.setOnClickListener {
            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            sBinding.llDjRecsList.visibility = View.GONE
            sBinding.rlDjRecordingAudio.visibility = View.VISIBLE
            sBinding.btnDjPauseContinueRecording.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_pause_24, requireContext().theme))
            sBinding.txtDjRecording.text = resources.getString(R.string.recording)
            startRecordingAudio()
        }

        // Set handler to stop and store recording
        sBinding.btnDjStopRecording.setOnClickListener {
            storeRecording(sBinding)
            sBinding.txtDjNoRecsFound.visibility = View.GONE
            isRecordingRunning = false
            requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        // Set handler to pause or resume recording
        sBinding.btnDjPauseContinueRecording.setOnClickListener {
            if (isRecordingRunning) {
                mRecorder.pause()
                sBinding.btnDjPauseContinueRecording.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_play_arrow_24, requireContext().theme))
                sBinding.txtDjRecording.text = resources.getString(R.string.recording_paused)
            } else {
                mRecorder.resume()
                sBinding.btnDjPauseContinueRecording.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_pause_24, requireContext().theme))
                sBinding.txtDjRecording.text = resources.getString(R.string.recording)
            }
            isRecordingRunning = !isRecordingRunning
        }

        // Set handler for storing recording / stopping playback when closing the BottomSheet
        bsd.setOnDismissListener {
            if (isRecordingRunning) {
                isRecordingRunning = false
                storeRecording(sBinding)
            }
            stopCurrentPlayback()
        }

        bsd.show()
    }

    private fun showTagEditor() {
        val context = requireContext()
        val bsd = BottomSheetDialog(context, R.style.BottomSheetDialogStyle)
        val sBinding = SheetJournalTagsEditorBinding.inflate(layoutInflater)
        bsd.setContentView(sBinding.root)

        // Fix issue with EditText hidden behind soft-keyboard
        bsd.setOnShowListener { dialog ->
            Handler(Looper.getMainLooper()).postDelayed({
                val d = dialog as BottomSheetDialog
                val bottomSheetBehavior =
                    BottomSheetBehavior.from(d.findViewById(R.id.design_bottom_sheet))
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED)
            }, 0)
        }

        // Show all currently assigned tags
        for (tag in entry.stringTags) {
            insertTagChip(tag, sBinding.flxDjTagsToAdd)
        }

        // Set the autosuggest values to the available tags
        sBinding.txtDjTagsEnter.singleLine()
        sBinding.txtDjTagsEnter.setAdapter(
            ArrayAdapter(
                context,
                android.R.layout.simple_list_item_1,
                allAvailableTags
            )
        )

        // Set handler to assign entered new tag to entry
        sBinding.txtDjTagsEnter.onItemClickListener = OnItemClickListener { _, _, _, _ ->
            tryAddTag(sBinding, sBinding.txtDjTagsEnter.text.toString())
        }

        // Set handler to assign selected existing tag to entry
        sBinding.txtDjTagsEnter.setOnEditorActionListener { _, i, _ ->
            val enteredTag = sBinding.txtDjTagsEnter.getText().toString()
            if (i == EditorInfo.IME_ACTION_DONE && !enteredTag.isEmpty()) {
                tryAddTag(sBinding, enteredTag)
            }
            true
        }

        // Set handler to assign new tag
        sBinding.btnAddTag.setOnClickListener {
            val enteredTag = sBinding.txtDjTagsEnter.getText().toString()
            if (!enteredTag.isEmpty()) {
                tryAddTag(sBinding, enteredTag)
            }
        }

        bsd.show()
    }

    private fun tryAddTag(sBinding: SheetJournalTagsEditorBinding, enteredTag: String) {
        val isUnassignedTag = entry.journalEntryTags.none {
            it.description.equals(enteredTag, ignoreCase = true)
        }

        if (isUnassignedTag) {
            entry.journalEntryTags.add(JournalEntryTag(enteredTag))
            insertTagChip(enteredTag, sBinding.flxDjTagsToAdd, 0)
            sBinding.txtDjTagsEnter.setText("")
        }
    }

    private fun insertTagChip(chipText: String, tagsContainer: ViewGroup, index: Int = -1) {
        val tagChip = Chip(context).apply {
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                leftMargin = 3.dpToPx
                rightMargin = 3.dpToPx
            }
            text = chipText
            chipStrokeWidth = 0f
            isCheckedIconVisible = false
            isCloseIconVisible = true
            chipBackgroundColor = context.attrColorStateList(R.attr.colorSurfaceContainerHigh)
            closeIconTint = ColorStateList.valueOf(resources.getColor(R.color.white, context.theme))
            setTextColor(context.attrColorStateList(R.attr.primaryTextColor))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        }

        // Add remove handler to remove tag from view and journal object
        tagChip.setOnCloseIconClickListener {
            removeTag(chipText)
            tagsContainer.removeView(tagChip)
        }

        tagsContainer.addView(tagChip, index)
    }

    private fun removeTag(tag: String) {
        entry.journalEntryTags.removeIf {
            it.description.equals(tag, ignoreCase = true)
        }
    }

    private fun stopCurrentPlayback() {
        mPlayer.stop()
        mPlayer.reset()
        currentPlayingImageButton?.setImageResource(R.drawable.ic_baseline_play_arrow_24)
        currentPlayingImageButton = null
    }

    private fun startRecordingAudio() {
        val activity = requireActivity()
        val audioPath = activity.filesDir.absolutePath + "/Recordings/recording_" + UUID.randomUUID() + ".aac"
        currentRecording = AudioLocation(audioPath, Calendar.getInstance().timeInMillis)
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mRecorder.setAudioEncodingBitRate(128000)
        mRecorder.setAudioSamplingRate(44100)
        mRecorder.setOutputFile(audioPath)
        mRecorder.prepare()
        try {
            isRecordingRunning = true
            mRecorder.start()
        } catch (e: IOException) {
            showToastLong(activity, "Error recording: ${e.message}")
        }
    }

    private fun storeRecording(sBinding: SheetJournalRecordingBinding) {
        mRecorder.stop()
        mRecorder.reset()
        val recording = currentRecording ?: throw IllegalStateException("Recording without a recording object should not be possible")
        entry.addAudioLocation(recording)
        sBinding.llDjRecsList.visibility = View.VISIBLE
        sBinding.rlDjRecordingAudio.visibility = View.GONE
        sBinding.llDjRecsEntryList.addView(generateAudioEntry(recording, sBinding.txtDjNoRecsFound))
        currentRecording = null
    }

    private fun generateAudioEntry(recording: AudioLocation, noRecordingsFound: TextView): LinearLayout {
        val entryContainer = recordingTools.generateContainerLayout()

        // Generate and add play button
        val playButton = recordingTools.generatePlayButton()
        playButton.setOnClickListener {
            handlePlayPauseMediaPlayer(recording, playButton)
        }
        entryContainer.addView(playButton)

        // Generate and add labels
        val labelsContainer = recordingTools.generateLabelsContainer()
        labelsContainer.addView(recordingTools.generateHeading())
        labelsContainer.addView(recordingTools.generateTimestamp(recording))
        entryContainer.addView(labelsContainer)

        // Generate and add duration
        entryContainer.addView(recordingTools.generateDuration(recording, false))

        // Generate and add delete button
        val deleteButton = recordingTools.generateDeleteButton()
        deleteButton.setOnClickListener {
            setupRecordingsDeleteDialog(recording, entryContainer, playButton, noRecordingsFound)
        }
        entryContainer.addView(deleteButton)

        return entryContainer
    }

    private fun setupRecordingsDeleteDialog(recording: AudioLocation, entryContainer: LinearLayout, playButton: ImageButton, noRecordingsFound: TextView) {
        MaterialAlertDialogBuilder(requireContext(), R.style.Theme_LucidSourceKit_ThemedDialog)
            .setTitle(resources.getString(R.string.recording_delete_header))
            .setMessage(resources.getString(R.string.recording_delete_message))
            .setPositiveButton(resources.getString(R.string.yes)) { _, _ ->
                if (currentPlayingImageButton === playButton) {
                    stopCurrentPlayback()
                }

                // Remove view and delete recording file
                (entryContainer.parent as LinearLayout).removeView(entryContainer)
                entry.deleteAudioLocation(recording.audioPath)

                // Show empty text in case all recordings have been deleted
                if (entry.audioLocations.isEmpty()) {
                    noRecordingsFound.visibility = View.VISIBLE
                }
            }
            .setNegativeButton(resources.getString(R.string.no), null)
            .show()
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
            mPlayer.setDataSource(audioFile)
            mPlayer.setOnCompletionListener { stopCurrentPlayback() }
            mPlayer.prepare()
            mPlayer.start()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onStop() {
        super.onStop()
        mPlayer.release()
        mRecorder.release()
    }
}