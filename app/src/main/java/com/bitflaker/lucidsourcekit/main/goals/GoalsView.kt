package com.bitflaker.lucidsourcekit.main.goals

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.annotation.DrawableRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager

import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.datastore.DataStoreKeys
import com.bitflaker.lucidsourcekit.datastore.getSetting
import com.bitflaker.lucidsourcekit.datastore.updateSetting
import com.bitflaker.lucidsourcekit.database.MainDatabase
import com.bitflaker.lucidsourcekit.database.goals.entities.Shuffle
import com.bitflaker.lucidsourcekit.database.goals.entities.ShuffleHasGoal
import com.bitflaker.lucidsourcekit.databinding.FragmentMainGoalsBinding
import com.bitflaker.lucidsourcekit.databinding.SheetGoalsAlgorithmEditorBinding
import com.bitflaker.lucidsourcekit.utils.Tools
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.slider.Slider

import java.util.Arrays
import java.util.Locale

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.InvalidParameterException
import kotlin.math.max
import kotlin.math.min

class GoalsView : Fragment() {
    private lateinit var db: MainDatabase
    private lateinit var binding: FragmentMainGoalsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentMainGoalsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = MainDatabase.getInstance(context)

        // Set speedometer chart text
        binding.somDifficulty.setDescription("Today's goals average\noccurrence rating")

//        // Start the graph from the time the app was first opened today
//        long firstOpenToday = dsManager.getSetting(DataStoreKeys.FIRST_OPEN_TIME_TODAY_DAY).blockingFirst();
//        binding.gtlAchieved.setShuffleInitTime(firstOpenToday);

        // Set click listener for algorithm adjustments
        binding.btnAdjustAlgorithm.setOnClickListener { setupAdjustAlgorithmSheet() }

        updateStats()

        // Set popup handler for more options button
        binding.btnMoreOptions.setOnClickListener {
            val popup = PopupMenu(ContextThemeWrapper(context, R.style.Theme_LucidSourceKit_PopupMenu), binding.btnMoreOptions)
            popup.menuInflater.inflate(R.menu.more_goals_options, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.itm_shuffle -> Thread(this::storeNewShuffle).start()
                    R.id.itm_edit_goals -> startActivity(Intent(context, GoalsEditorView::class.java))
                    R.id.itm_about_goals -> {
                        // TODO: show page with details about what goals are and what they are supposed to achieve
                    }
                    else -> throw InvalidParameterException("Unknown option selected")
                }
                true
            }
            popup.show()
        }
    }

    private fun setupAdjustAlgorithmSheet() {
        val sBinding = SheetGoalsAlgorithmEditorBinding.inflate(layoutInflater)
        val bsdAdjustAlgorithm = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogStyle)
        bsdAdjustAlgorithm.setContentView(sBinding.root)

        val transition = ChangeBounds()
        transition.excludeTarget(sBinding.txtTitleGoalAlgorithm, true)
        transition.setDuration(300L)

        sBinding.btnShuffle.setOnClickListener {
            Thread(this::storeNewShuffle).start()
        }
        sBinding.crdEditGoals.setOnClickListener {
            startActivity(Intent(context, GoalsEditorView::class.java))
        }

        // Set transition handler to edit weights views
        sBinding.crdEditWeights.setOnClickListener {
            TransitionManager.beginDelayedTransition(bsdAdjustAlgorithm.findViewById(R.id.design_bottom_sheet), transition)
            sBinding.btnBack.visibility = View.VISIBLE
            sBinding.btnShuffle.visibility = View.GONE
            sBinding.clMainSettings.visibility = View.GONE
            sBinding.clWeightEditor.visibility = View.VISIBLE
            sBinding.txtTitleGoalAlgorithm.text = "Edit weights"
        }

        // Set transition handler to edit goal count views
        sBinding.crdEditGoalCount.setOnClickListener {
            TransitionManager.beginDelayedTransition(bsdAdjustAlgorithm.findViewById(R.id.design_bottom_sheet), transition)
            sBinding.btnBack.visibility = View.VISIBLE
            sBinding.clMainSettings.visibility = View.GONE
            sBinding.btnShuffle.visibility = View.GONE
            sBinding.clGoalCountEditor.visibility = View.VISIBLE
            sBinding.txtTitleGoalAlgorithm.text = "Edit goal count"
        }

        // Set transition handler to personalize goals views
        sBinding.btnBack.setOnClickListener {
            TransitionManager.beginDelayedTransition(bsdAdjustAlgorithm.findViewById(R.id.design_bottom_sheet), transition)
            sBinding.btnBack.visibility = View.GONE
            sBinding.clMainSettings.visibility = View.VISIBLE
            sBinding.clWeightEditor.visibility = View.GONE
            sBinding.clGoalCountEditor.visibility = View.GONE
            sBinding.btnShuffle.visibility = View.VISIBLE
            sBinding.txtTitleGoalAlgorithm.text = "Personalize goals"
        }

        // Set checkbox entry click handlers
        sBinding.crdDynamicRating.setOnClickListener {
            sBinding.swtDynamicRating.setChecked(!sBinding.swtDynamicRating.isChecked)
        }
        sBinding.crdDynamicShuffleCount.setOnClickListener {
            sBinding.swtDynamicShuffleCount.setChecked(!sBinding.swtDynamicShuffleCount.isChecked)
        }

        val weightSliders = arrayOf(
            sBinding.sldAlgoDiffCommon,
            sBinding.sldAlgoDiffUncommon,
            sBinding.sldAlgoDiffRare
        )

        // Set value change listener of category `common`
        sBinding.sldAlgoDiffCommon.addOnChangeListener { slider, value, fromUser ->
            sBinding.txtSldValueCommon.text = String.format(Locale.getDefault(), "%.0f%%", value * 100)
            if (fromUser) {
                setSliderProportions(weightSliders, slider)
            }
        }

        // Set value change listener of category `uncommon`
        sBinding.sldAlgoDiffUncommon.addOnChangeListener { slider, value, fromUser ->
            sBinding.txtSldValueUncommon.text = String.format(Locale.getDefault(), "%.0f%%", value * 100)
            if (fromUser) {
                setSliderProportions(weightSliders, slider)
            }
        }

        // Set value change listener of category `rare`
        sBinding.sldAlgoDiffRare.addOnChangeListener { slider, value, fromUser ->
            sBinding.txtSldValueRare.text = String.format(Locale.getDefault(), "%.0f%%", value * 100)
            if (fromUser) {
                setSliderProportions(weightSliders, slider)
            }
        }

        // Set value change listener for goals count
        sBinding.sldGoalCount.addOnChangeListener { slider, value, fromUser ->
            sBinding.txtSldValueGoalCount.text = String.format(Locale.getDefault(), "%d", value.toInt())
        }

        // Load stored values and set fields accordingly
        lifecycleScope.launch(Dispatchers.IO) {
            val activity = requireActivity()
            val diffCommon = activity.getSetting(DataStoreKeys.GOAL_DIFFICULTY_VALUE_COMMON) ?: 0f
            val diffUncommon = activity.getSetting(DataStoreKeys.GOAL_DIFFICULTY_VALUE_UNCOMMON) ?: 0f
            val diffRare = activity.getSetting(DataStoreKeys.GOAL_DIFFICULTY_VALUE_RARE) ?: 0f
            val goalCount = activity.getSetting(DataStoreKeys.GOAL_DIFFICULTY_COUNT) ?: 3
            val isDynamicRating = activity.getSetting(DataStoreKeys.GOAL_DIFFICULTY_AUTO_ADJUST) ?: false

            activity.runOnUiThread {
                sBinding.sldAlgoDiffCommon.value = diffCommon
                sBinding.sldAlgoDiffUncommon.value = diffUncommon
                sBinding.sldAlgoDiffRare.value = diffRare
                sBinding.sldGoalCount.value = goalCount.toFloat()
                sBinding.swtDynamicRating.setChecked(isDynamicRating)
            }
        }

        // Set dismiss handler
        bsdAdjustAlgorithm.setOnDismissListener {
            saveGoalAlgorithm(
                sBinding.sldAlgoDiffCommon.value,
                sBinding.sldAlgoDiffUncommon.value,
                sBinding.sldAlgoDiffRare.value,
                sBinding.sldGoalCount.value.toInt(),
                sBinding.swtDynamicRating.isChecked
            )
        }

        bsdAdjustAlgorithm.show()
    }

    private fun setSliderProportions(sliders: Array<Slider>, slider: Slider) {
        if (sliders.isEmpty()) {
            return
        }

        val otherSliders: Array<Slider> = sliders.filter { it !== slider }.toTypedArray()
        val total = otherSliders.sumOf { it.value.toDouble() }.toFloat()

        val parts = FloatArray(otherSliders.size)
        if (total == 0f) {
            Arrays.fill(parts, 1.0f / otherSliders.size)
        } else {
            for (i in otherSliders.indices) {
                parts[i] = otherSliders[i].value / total
            }
        }

        val diff = 1 - (total + slider.value)

        for (i in otherSliders.indices) {
            val current: Slider = otherSliders[i]
            current.value = min(max(current.value + parts[i] * diff, 0f), 1f)
        }
    }

    private fun updateStats() {
        // Get timestamps for today and yesterday
        val tsToday = Tools.getTimeSpanFrom(0, false)
        val tsYesterday = Tools.getTimeSpanFrom(1, false)

        // Build goal stats calculators
        val statsCalcToday = GoalStatisticsCalculator(db, tsToday.first, tsToday.second)
        val statsCalcYesterday = GoalStatisticsCalculator(db, tsYesterday.first, tsYesterday.second)

        // Calculate goal stats and update stats
        lifecycleScope.launch(Dispatchers.IO) {
            val today = statsCalcToday.calculate()
            val yesterday = statsCalcYesterday.calculate()
            requireActivity().runOnUiThread {
//                updateGoalStatsYesterdayUI(yesterday);
                updateGoalStatsTodayUI(today, yesterday)
            }
        }
    }

    //    private void updateGoalStatsYesterdayUI(GoalStatisticsCalculator statsCalculator) {
    //        if(setPastGoalsVisibility(statsCalculator.hasGoals())) {
    //            binding.txtYgoalsDifficulty.setText(String.format(Locale.getDefault(), "%.1f", statsCalculator.getDifficulty()));
    //
    //            String[] numParts = getDecimalNumParts(100 * statsCalculator.getRatioAchieved(), 2);
    //            binding.txtYgoalsAchieved.setText(numParts[0]);
    //            binding.txtYgoalsAchievedPart.setText(String.format(Locale.getDefault(), "%s%%", numParts[1]));
    //
    //            numParts = getDecimalNumParts(100 * statsCalculator.getShuffleOccurrenceRating(), 1);
    //            binding.txtYgoalsSelDifficulty.setText(numParts[0]);
    //            binding.txtYgoalsSelDifficultyPart.setText(String.format(Locale.getDefault(), "%s%%", numParts[1]));
    //
    //            numParts = getDecimalNumParts(statsCalculator.getRecurrenceFrequency(), 2);
    //            binding.txtYgoalsOccFreq.setText(numParts[0]);
    //            binding.txtYgoalsOccFreqPart.setText(String.format(Locale.getDefault(), "%s%%", numParts[1]));
    //        }
    //    }
    //
    //    private boolean setPastGoalsVisibility(boolean hasGoals) {
    //        binding.llPastGoalsRatings.setVisibility(hasGoals ? View.VISIBLE : View.GONE);
    //        binding.crdPastGoalsAchieved.setVisibility(hasGoals ? View.VISIBLE : View.GONE);
    //        binding.crdPastGoalsOccurrenceRating.setVisibility(hasGoals ? View.VISIBLE : View.GONE);
    //        binding.crdNoDataPastGoals.setVisibility(hasGoals ? View.GONE : View.VISIBLE);
    //        return hasGoals;
    //    }

    private fun updateGoalStatsTodayUI(current: GoalStatisticsCalculator, comparedTo: GoalStatisticsCalculator) {
        binding.somDifficulty.setData(25f, current.difficulty, 3f)

        // Initialize recycler view
        val adapterCurrentGoals = RecyclerViewAdapterCurrentGoals(this, current.getGoals(), current.shuffleId)
        binding.rcvCurrentGoals.setAdapter(adapterCurrentGoals)
        binding.rcvCurrentGoals.setLayoutManager(LinearLayoutManager(context))

        // Highlight all achieved timestamps on graph
        lifecycleScope.launch(Dispatchers.IO) {
            val achievedTimes = db.shuffleTransactionDao.getAchievedTimes(current.shuffleId)
            val showTimeline = achievedTimes.isEmpty()

            requireActivity().runOnUiThread {
                binding.gtlAchieved.achieved = achievedTimes
                binding.gtlAchieved.visibility = if (showTimeline) View.GONE else View.VISIBLE
                binding.crdNoGoalTimeline.visibility = if (showTimeline) View.VISIBLE else View.GONE
            }
        }

        // Set shuffle occurrence rating indicator and value
        binding.txtCurrentSelectionDiffFull.text = getDecimalNumParts(100 * current.shuffleOccurrenceRating, 1)
        binding.imgSelectionDiffComparison.setImageDrawable(
            getDiffIndicator(
                current.difficulty,
                comparedTo.difficulty,
                comparedTo.hasGoals()
            )
        )

        // Set shuffle recurrence rating indicator and value
        binding.txtOccurrenceFreqFull.text = getDecimalNumParts(current.recurrenceFrequency, 1)
        binding.imgOccFreqComparison.setImageDrawable(
            getDiffIndicator(
                current.recurrenceFrequency,
                comparedTo.recurrenceFrequency,
                comparedTo.hasGoals()
            )
        )
    }

    private fun getDiffIndicator(current: Float, comparedTo: Float, hasPastGoals: Boolean): Drawable? {
        @DrawableRes val iconResId = when {
            !hasPastGoals -> R.drawable.round_mode_standby_24
            comparedTo < current -> R.drawable.ic_round_arrow_upward_24
            else -> R.drawable.ic_round_arrow_downward_24
        }
        return ResourcesCompat.getDrawable(resources, iconResId, requireContext().theme)
    }

    @Suppress("SameParameterValue")
    private fun getDecimalNumParts(value: Float, decimals: Int): String {
        val decimalsFormat = "%." + decimals + "f"
        return if (value.isNaN()) "0" else String.format(Locale.getDefault(), decimalsFormat, value)
    }

    private fun storeNewShuffle() {
        lifecycleScope.launch(Dispatchers.IO) {
            val todayTimeSpan = Tools.getTimeSpanFrom(0, false)
            val alreadyPresentShuffle = db.shuffleDao.getLastShuffleInDay(todayTimeSpan.first, todayTimeSpan.second)
            val id = getShuffleIdWithNoGoals(todayTimeSpan, alreadyPresentShuffle)
            val shuffleHasGoals = Tools.getNewShuffleGoals(requireContext()).map {
                ShuffleHasGoal(id, it.goalId)
            }
            db.shuffleHasGoalDao.insertAll(shuffleHasGoals)
            updateStats()
        }
    }

    private suspend fun getShuffleIdWithNoGoals(timeSpan: Pair<Long, Long>, shuffle: Shuffle?): Int {
        if (shuffle != null) {
            db.shuffleHasGoalDao.deleteAllWithShuffleId(shuffle.shuffleId)
            return shuffle.shuffleId
        }
        val newShuffle = Shuffle(timeSpan.first, timeSpan.second)
        return db.shuffleDao.insert(newShuffle).toInt()
    }

    private fun saveGoalAlgorithm(valueCommon: Float, valueUncommon: Float, valueRare: Float, goalCount: Int, autoAdjustGoalDifficulty: Boolean) {
        val context = requireContext()
        lifecycleScope.launch(Dispatchers.IO) {
            context.updateSetting(DataStoreKeys.GOAL_DIFFICULTY_AUTO_ADJUST, autoAdjustGoalDifficulty)
            context.updateSetting(DataStoreKeys.GOAL_DIFFICULTY_VALUE_COMMON, valueCommon)
            context.updateSetting(DataStoreKeys.GOAL_DIFFICULTY_VALUE_UNCOMMON, valueUncommon)
            context.updateSetting(DataStoreKeys.GOAL_DIFFICULTY_VALUE_RARE, valueRare)
            context.updateSetting(DataStoreKeys.GOAL_DIFFICULTY_COUNT, goalCount)
        }
    }
}