package com.bitflaker.lucidsourcekit.main.dreamjournal.views

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ToggleButton
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.main.dreamjournal.rating.DreamClarity
import com.bitflaker.lucidsourcekit.main.dreamjournal.rating.DreamMoods
import com.bitflaker.lucidsourcekit.main.dreamjournal.rating.DreamTypes
import com.bitflaker.lucidsourcekit.main.dreamjournal.rating.SleepQuality
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.results.DreamJournalEntry
import com.bitflaker.lucidsourcekit.databinding.FragmentJournalEditorRatingBinding
import com.bitflaker.lucidsourcekit.utils.attrColor
import com.bitflaker.lucidsourcekit.utils.attrColorStateList
import com.bitflaker.lucidsourcekit.utils.dpToPx
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.security.InvalidParameterException

class DreamJournalEditorRatingView(val entry: DreamJournalEntry) : Fragment() {
    private val dreamMoodLabels = arrayOf("Terrible", "Poor", "Okay", "Great", "Outstanding")
    private val sleepQualityLabels = arrayOf("Terrible", "Poor", "Great", "Outstanding")
    private val dreamClarityLabels = arrayOf("Very Cloudy", "Cloudy", "Clear", "Crystal Clear")
    private lateinit var binding: FragmentJournalEditorRatingBinding
    private lateinit var dreamMoods: Array<ImageView>
    private lateinit var sleepQualities: Array<ImageView>
    private lateinit var dreamClarities: Array<ImageView>
    private lateinit var unselectedIconColor: ColorStateList
    private lateinit var selectedIconColor: ColorStateList
    var onBackButtonListener: (() -> Unit)? = null
    var onDoneButtonListener: (() -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentJournalEditorRatingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()
        unselectedIconColor = context.attrColorStateList(R.attr.colorSurfaceContainerHigh)
        selectedIconColor = context.attrColorStateList(R.attr.colorOnSurface)

        // Initialize indicator image arrays
        dreamMoods = arrayOf(
            binding.imgVeryDissatisfied,
            binding.imgDissatisfied,
            binding.imgNeutralSatisfied,
            binding.imgSatisfied,
            binding.imgVerySatisfied
        )
        sleepQualities = arrayOf(
            binding.imgVeryBadQuality,
            binding.imgBadQuality,
            binding.imgGoodQuality,
            binding.imgVeryGoodQuality
        )
        dreamClarities = arrayOf(
            binding.imgVeryUnclear,
            binding.imgUnclear,
            binding.imgClear,
            binding.imgVeryClear
        )

        restoreStoredEntry()

        // Set handlers for toggling special dream types
        binding.tglDjNightmare.setOnClickListener { e ->
            toggleSpecialDreamType(e as ToggleButton, DreamTypes.Nightmare.id)
        }
        binding.tglDjParalysis.setOnClickListener { e ->
            toggleSpecialDreamType(e as ToggleButton, DreamTypes.SleepParalysis.id)
        }
        binding.tglDjLucid.setOnClickListener { e ->
            toggleSpecialDreamType(e as ToggleButton, DreamTypes.Lucid.id)
        }
        binding.tglDjRecurring.setOnClickListener { e ->
            toggleSpecialDreamType(e as ToggleButton, DreamTypes.Recurring.id)
        }
        binding.tglDjFalseAwakening.setOnClickListener { e ->
            toggleSpecialDreamType(e as ToggleButton, DreamTypes.FalseAwakening.id)
        }

        // Set label formatter values
        binding.sldDjDreamMood.setLabelFormatter { value -> dreamMoodLabels[value.toInt()] }
        binding.sldDjSleepQuality.setLabelFormatter { value -> sleepQualityLabels[value.toInt()] }
        binding.sldDjDreamClarity.setLabelFormatter { value -> dreamClarityLabels[value.toInt()] }

        // Set handlers for slider value changes
        binding.sldDjDreamMood.addOnChangeListener { _, value, fromUser ->
            handleIconSlider(value.toInt(), dreamMoods)
            entry.journalEntry.moodId = DreamMoods.entries[value.toInt()].id
        }
        binding.sldDjSleepQuality.addOnChangeListener { _, value, fromUser ->
            handleIconSlider(value.toInt(), sleepQualities)
            entry.journalEntry.qualityId = SleepQuality.entries[value.toInt()].id
        }
        binding.sldDjDreamClarity.addOnChangeListener { _, value, fromUser ->
            handleIconSlider(value.toInt(), dreamClarities)
            entry.journalEntry.clarityId = DreamClarity.entries[value.toInt()].id
        }

        // Set handler for closing editor
        binding.btnDjCloseEditor.setOnClickListener {
            onBackButtonListener?.invoke()
        }

        // Set handler for going back to the text editor
        binding.btnDjBackToText.setOnClickListener {
            onBackButtonListener?.invoke()
        }

        // Set handler for saving entry
        binding.btnDjDoneRating.setOnClickListener {
            val hasMissingTitle = TextUtils.isEmpty(entry.journalEntry.title)
            val hasMissingContent = (TextUtils.isEmpty(entry.journalEntry.description) && entry.audioLocations.isEmpty())
            if (hasMissingTitle || hasMissingContent) {
                MaterialAlertDialogBuilder(context, R.style.Theme_LucidSourceKit_ThemedDialog)
                    .setTitle("Missing fields")
                    .setMessage("You have to provide a title and a description or audio recording for your dream journal entry!")
                    .setPositiveButton(resources.getString(R.string.ok), null)
                    .show()
                return@setOnClickListener
            }
            onDoneButtonListener?.invoke()
        }
    }

    private fun toggleSpecialDreamType(button: ToggleButton, dreamType: String) {
        if (button.isChecked) {
            entry.addDreamType(dreamType)
        } else {
            entry.removeDreamType(dreamType)
        }
    }

    private fun handleIconSlider(value: Int, icons: Array<ImageView>) {
        if (value >= icons.size) {
            throw InvalidParameterException("Value exceeded icon count")
        }

        // Update the selected state of all icons
        for (i in icons.indices) {
            val isSelected = i == value
            icons[i].setImageTintList(if (isSelected) selectedIconColor else unselectedIconColor)
            icons[i].updateLayoutParams<LinearLayout.LayoutParams> {
                topMargin = if (isSelected) 4.dpToPx else 0
                height = if (isSelected) 20.dpToPx else 16.dpToPx
            }
            icons[i].invalidate()
        }
    }

    private fun restoreStoredEntry() {
        // Restore slider states and values
        binding.sldDjDreamMood.value = DreamMoods.getEnum(entry.journalEntry.moodId).ordinal.toFloat()
        handleIconSlider(binding.sldDjDreamMood.value.toInt(), dreamMoods)
        binding.sldDjDreamClarity.value = DreamClarity.getEnum(entry.journalEntry.clarityId).ordinal.toFloat()
        handleIconSlider(binding.sldDjDreamClarity.value.toInt(), dreamClarities)
        binding.sldDjSleepQuality.value = SleepQuality.getEnum(entry.journalEntry.qualityId).ordinal.toFloat()
        handleIconSlider(binding.sldDjSleepQuality.value.toInt(), sleepQualities)

        // Restore toggle button states
        binding.tglDjNightmare.setChecked(entry.hasSpecialType(DreamTypes.Nightmare.id))
        toggleSpecialDreamType(binding.tglDjNightmare, DreamTypes.Nightmare.id)
        binding.tglDjParalysis.setChecked(entry.hasSpecialType(DreamTypes.SleepParalysis.id))
        toggleSpecialDreamType(binding.tglDjParalysis, DreamTypes.SleepParalysis.id)
        binding.tglDjLucid.setChecked(entry.hasSpecialType(DreamTypes.Lucid.id))
        toggleSpecialDreamType(binding.tglDjLucid, DreamTypes.Lucid.id)
        binding.tglDjRecurring.setChecked(entry.hasSpecialType(DreamTypes.Recurring.id))
        toggleSpecialDreamType(binding.tglDjRecurring, DreamTypes.Recurring.id)
        binding.tglDjFalseAwakening.setChecked(entry.hasSpecialType(DreamTypes.FalseAwakening.id))
        toggleSpecialDreamType(binding.tglDjFalseAwakening, DreamTypes.FalseAwakening.id)
    }

    fun updatePreview() {
        val context = requireContext()
        val title = entry.journalEntry.title
        val description = entry.journalEntry.description
        val emptyTitle = title.isNullOrEmpty()
        val emptyDescription = description.isNullOrEmpty()

        // Set text colors for title and description
        binding.txtJournalTitle.setTextColor(context.attrColor(if (emptyTitle) R.attr.tertiaryTextColor else R.attr.primaryTextColor))
        binding.txtJournalDescription.setTextColor(context.attrColor(if (emptyDescription) R.attr.tertiaryTextColor else R.attr.secondaryTextColor))

        // Set text values for title and description
        binding.txtJournalTitle.text = if (emptyTitle) "No title provided" else title
        binding.txtJournalDescription.text = if (emptyDescription) "No description provided. Try to write the dream down to better memorize them." else description

        // Load tags container into preview
        binding.llTagsHolder.removeAllViews()

        // TODO: Rework the way this preview section works / put something different up there
//        RecyclerViewAdapterDreamJournal.MainViewHolder.setTagList(
//            binding.llTagsHolder,
//            0,
//            0,
//            entry.stringTags,
//            activity
//        )
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onResume() {
        super.onResume()
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
    }

    override fun onPause() {
        super.onPause()
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR)
    }
}