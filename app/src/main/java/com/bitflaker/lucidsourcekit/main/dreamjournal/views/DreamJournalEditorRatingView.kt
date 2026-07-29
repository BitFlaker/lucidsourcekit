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
    private lateinit var binding: FragmentJournalEditorRatingBinding
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

        binding.isDreamMood.setItems(
            R.drawable.ic_baseline_sentiment_very_dissatisfied_24 to "Terrible",
            R.drawable.ic_baseline_sentiment_dissatisfied_24 to "Poor",
            R.drawable.ic_baseline_sentiment_neutral_24 to "Okay",
            R.drawable.ic_baseline_sentiment_satisfied_24 to "Great",
            R.drawable.ic_baseline_sentiment_very_satisfied_24 to "Outstanding",
        )

        binding.isSleepQuality.setItems(
            R.drawable.ic_baseline_star_border_24 to "Terrible",
            R.drawable.ic_baseline_star_half_24 to "Poor",
            R.drawable.ic_baseline_star_24 to "Great",
            R.drawable.ic_baseline_stars_24 to "Outstanding",
        )

        binding.isDreamClarity.setItems(
            R.drawable.ic_baseline_brightness_4_24 to "Very Cloudy",
            R.drawable.ic_baseline_brightness_5_24 to "Cloudy",
            R.drawable.ic_baseline_brightness_6_24 to "Clear",
            R.drawable.ic_baseline_brightness_7_24 to "Crystal Clear",
        )

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

        // Set handlers for slider value changes
        binding.isDreamMood.onValueChange = { entry.journalEntry.moodId = DreamMoods.entries[it].id }
        binding.isSleepQuality.onValueChange = { entry.journalEntry.qualityId = SleepQuality.entries[it].id }
        binding.isDreamClarity.onValueChange = { entry.journalEntry.clarityId = DreamClarity.entries[it].id }

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

        restoreStoredEntry()
    }

    private fun toggleSpecialDreamType(button: ToggleButton, dreamType: String) {
        if (button.isChecked) {
            entry.addDreamType(dreamType)
        } else {
            entry.removeDreamType(dreamType)
        }
    }

    private fun restoreStoredEntry() {
        // Restore slider states and values
        binding.isDreamMood.value = DreamMoods.getEnum(entry.journalEntry.moodId)?.ordinal ?: 0
        binding.isDreamClarity.value = DreamClarity.getEnum(entry.journalEntry.clarityId)?.ordinal ?: 0
        binding.isSleepQuality.value = SleepQuality.getEnum(entry.journalEntry.qualityId)?.ordinal ?: 0

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