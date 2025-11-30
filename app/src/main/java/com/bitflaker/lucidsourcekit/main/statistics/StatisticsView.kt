package com.bitflaker.lucidsourcekit.main.statistics

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.data.datastore.DataStoreKeys
import com.bitflaker.lucidsourcekit.data.datastore.getSetting
import com.bitflaker.lucidsourcekit.data.records.DataValue
import com.bitflaker.lucidsourcekit.data.usage.AppUsage
import com.bitflaker.lucidsourcekit.data.usage.AppUsage.AppOpenStats
import com.bitflaker.lucidsourcekit.database.MainDatabase
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.resulttables.TagCount
import com.bitflaker.lucidsourcekit.databinding.FragmentMainStatisticsBinding
import com.bitflaker.lucidsourcekit.utils.Tools
import com.bitflaker.lucidsourcekit.utils.attrColor
import com.bitflaker.lucidsourcekit.utils.dpToPx
import com.bitflaker.lucidsourcekit.views.ProportionLineChart
import com.bitflaker.lucidsourcekit.views.RangeProgress
import com.bitflaker.lucidsourcekit.views.RodGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.InvalidParameterException
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Collections
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors
import kotlin.math.roundToInt

class StatisticsView : Fragment() {
    private lateinit var db: MainDatabase
    private val avgClarities: MutableList<Double> = ArrayList()
    private val avgMoods: MutableList<Double> = ArrayList()
    private val avgQualities: MutableList<Double> = ArrayList()
    private val dreamCounts: MutableList<Double> = ArrayList()
    private lateinit var moodIcons: Array<Drawable>
    private lateinit var clarityIcons: Array<Drawable>
    private lateinit var qualityIcons: Array<Drawable>
    private var selectedDaysCount = 7
    private val df = DecimalFormat("0.0")
    private lateinit var binding: FragmentMainStatisticsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMainStatisticsBinding.inflate(getLayoutInflater())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = MainDatabase.getInstance(context)
        moodIcons = Tools.getIconsDreamMood(requireContext())
        clarityIcons = Tools.getIconsDreamClarity(requireContext())
        qualityIcons = Tools.getIconsSleepQuality(requireContext())

        binding.btnDreamFrequencyFilter.setOnClickListener {
            val arrowUp = ResourcesCompat.getDrawable(resources, R.drawable.rounded_keyboard_arrow_up_24, requireContext().theme)
            val arrowDown = ResourcesCompat.getDrawable(resources, R.drawable.rounded_keyboard_arrow_down_24, requireContext().theme)

            binding.btnDreamFrequencyFilter.setCompoundDrawablesWithIntrinsicBounds(null, null, arrowUp, null)
            val popup = PopupMenu(ContextThemeWrapper(context, R.style.Theme_LucidSourceKit_PopupMenu_Icon), binding.btnDreamFrequencyFilter)
            popup.setForceShowIcon(true)
            popup.menuInflater.inflate(R.menu.dream_frequency_types, popup.menu)
            popup.setOnMenuItemClickListener {
                binding.btnDreamFrequencyFilter.text = String.format(Locale.getDefault(), "%s Frequency", it.title)
                true
            }
            popup.setOnDismissListener {
                binding.btnDreamFrequencyFilter.setCompoundDrawablesWithIntrinsicBounds(null, null, arrowDown, null)
            }
            popup.show()
        }

        // Set stats for app usage (time spent, session count, session length, ...)
        lifecycleScope.launch(Dispatchers.IO) {
            val appUsage = AppUsage.getUsageStats(requireActivity(), (1000 * 60 * 60 * 24 * 7).toLong())
            val appOpenTimes = appUsage.appOpenTimeStamps
            val totalTime = appUsage.totalTime
            val journalTime = appUsage.journalTime / 1000.0f
            val otherTime = totalTime / 1000.0f - journalTime

            requireActivity().runOnUiThread {
                setAppUsageStats(totalTime, journalTime, otherTime)
                if (!appOpenTimes.isEmpty()) {
                    setAppSessionStats(appOpenTimes)
                }
            }
        }

        // Set listener for filter of heatmap of dream journal entry types
        binding.htmDreamCountHeatmap.onWeekCountCalculatedListener = { weekCount: Int ->
            val calendar = Calendar.getInstance()
            calendar.firstDayOfWeek = Calendar.MONDAY
            var dayOfWeekIndex = calendar.get(Calendar.DAY_OF_WEEK) - 2
            dayOfWeekIndex = if (dayOfWeekIndex == -1) 6 else dayOfWeekIndex

            calendar.add(Calendar.HOUR, dayOfWeekIndex * -24)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            calendar.add(Calendar.HOUR, -24 * 7 * weekCount)

            val timeFrom = calendar.timeInMillis
            lifecycleScope.launch(Dispatchers.IO) {
                val timestamps = db.journalEntryDao.getEntriesFrom(timeFrom)
                requireActivity().runOnUiThread {
                    binding.htmDreamCountHeatmap.setTimestamps(timestamps)
                }
            }
        }

        // Load app open streak
        lifecycleScope.launch(Dispatchers.IO) {
            val currentStreakValue = requireContext().getSetting(DataStoreKeys.APP_OPEN_STREAK) ?: 0
            val bestStreakValue = requireContext().getSetting(DataStoreKeys.APP_OPEN_STREAK_LONGEST) ?: 0
            binding.iooStreakCheckIn.setValue(currentStreakValue.toInt())
            binding.iooStreakCheckIn.setMaxValue(bestStreakValue.toInt())
        }

        // TODO: refresh stats after entry modified/added/deleted
        // Recursively get average dream rating values over the last N days
        lifecycleScope.launch(Dispatchers.IO) {
            getAveragesForLastNDays(selectedDaysCount, 0)
            updateStats()
        }

        // Set listener for change of selection in timeframe
        binding.chpGrpTimeSpan.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedDaysCount = when (checkedIds[0]) {
                R.id.chp_last_7_days -> 7
                R.id.chp_last_30_days -> 30
                R.id.chp_all_time -> 60 // TODO: make it actually all time (but fix performance issues)
                else -> throw InvalidParameterException("Provided unknown timespan selection")
            }
            lifecycleScope.launch(Dispatchers.IO) {
                getAveragesForLastNDays(selectedDaysCount, 0)
                updateStats()
            }
        }
    }

    private fun setAppUsageStats(totalTime: Long, journalTime: Float, otherTime: Float) {
        val context = requireContext()
        val hours = TimeUnit.MILLISECONDS.toHours(totalTime)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(totalTime) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(totalTime))
        binding.pcTimeSpentProportions.setValues(
            arrayOf(
                ProportionLineChart.DataPoint(context.attrColor(R.attr.colorPrimary), journalTime, "Dream journal"),
                ProportionLineChart.DataPoint(context.attrColor(R.attr.colorTertiary), 0f, "Binaural beats"),
                ProportionLineChart.DataPoint(context.attrColor(R.attr.colorSecondary), otherTime, "Other")
            )
        )
        binding.txtTimeSpentValue.text = if (hours == 0L) String.format(Locale.ENGLISH, "%d min", minutes)
                                                     else String.format(Locale.ENGLISH, "%d hr %d min", hours, minutes)
    }

    private fun groupSessionDurationsByDay(appOpenTimes: List<AppOpenStats>): HashMap<Long, MutableList<Long>> {
        val daySessionDurations = HashMap<Long, MutableList<Long>>()
        val currentMidnight = Tools.getMidnightTime()
        for (i in 0..6) {
            daySessionDurations.put(currentMidnight - 1000 * 60 * 60 * 24 * i, ArrayList())
        }
        for (i in appOpenTimes.indices) {
            val appOpenStats = appOpenTimes[i]
            val midnightTime = Tools.getMidnightTime(appOpenStats.openedAt)
            daySessionDurations[midnightTime]?.add(appOpenStats.openFor)
        }
        return daySessionDurations
    }

    private fun setAppSessionStats(appOpenTimes: List<AppOpenStats>) {
        val daySessionDurations = groupSessionDurationsByDay(appOpenTimes)
        val averageDailyAppOpens = daySessionDurations.values
            .stream()
            .mapToInt { it?.size ?: 0 }
            .average()
            .orElse(0.0)
        val averageSessionLengthMs = daySessionDurations.values
            .stream()
            .filter { !it.isNullOrEmpty() }
            .mapToDouble { it!!.stream().mapToLong { l -> l!! }.average().orElse(0.0) }
            .average()
            .orElse(0.0)
            .toInt()
        val averageSessionLengthMin = TimeUnit.MILLISECONDS.toMinutes(averageSessionLengthMs.toLong())
        binding.txtAverageSessionCount.text = String.format(Locale.getDefault(), "%s x", df.format(averageDailyAppOpens))
        binding.txtAverageSessionLength.text = String.format(Locale.getDefault(), "%d min", averageSessionLengthMin)
        binding.ichSessionHeatmap.setTimestamps(appOpenTimes.stream().map {
            it.openedAt - Tools.getMidnightTime(it.openedAt)
        }.collect(Collectors.toList()))
    }

    private suspend fun updateStats() {
        val timeSpan = Tools.getTimeSpanFrom(selectedDaysCount - 1, true)
        generateStaticStats()
        generateDreamJournalStats(timeSpan)
        generateDailyGoalsStats(timeSpan)
    }

    private suspend fun generateStaticStats() {
        val journalEntriesCount: Int = db.journalEntryDao.getTotalEntriesCount()
        val tagCount = db.journalEntryTagDao.getTotalTagCount()
        val goalCount = db.goalDao.getGoalCount()

        requireActivity().runOnUiThread {
            binding.txtTotalJournalEntries.text = String.format(Locale.getDefault(), "%d", journalEntriesCount)
            binding.txtTotalTagCount.text = String.format(Locale.getDefault(), "%d", tagCount)
            binding.txtTotalGoalCount.text = String.format(Locale.getDefault(), "%d", goalCount)
        }
    }

    private suspend fun generateDailyGoalsStats(timeSpan: Pair<Long, Long>) {
        val goalShuffleData = db.shuffleHasGoalDao.getShufflesFromBetween(timeSpan.first, timeSpan.second)
        val hasGoalShuffleData = goalShuffleData.goalCount > 0

        requireActivity().runOnUiThread {
            if (!hasGoalShuffleData) {
                binding.crdNoDataGoals.visibility = View.VISIBLE
                binding.llGoalsReached.visibility = View.GONE
                return@runOnUiThread
            }

            binding.rpGoalsReached.setBackgroundAttrColor(R.attr.colorSurfaceContainer)
            binding.rpAvgGoalDiff.setBackgroundAttrColor(R.attr.colorSurfaceContainer)
            binding.rpGoalsReached.setData(
                goalShuffleData.goalCount.toFloat(),
                goalShuffleData.achievedCount.toFloat(),
                "ACHIEVED",
                null,
                String.format(Locale.ENGLISH, "%d/%d", goalShuffleData.achievedCount, goalShuffleData.goalCount)
            )
            binding.rpAvgGoalDiff.setData(
                3f,
                goalShuffleData.avgDifficulty.toFloat(),
                "AVERAGE DIFFICULTY LEVEL",
                null,
                String.format(Locale.ENGLISH, "%.2f", goalShuffleData.avgDifficulty)
            )
            binding.llGoalsReached.visibility = View.VISIBLE
        }
    }

    private suspend fun generateDreamJournalStats(timeSpan: Pair<Long, Long>) {
        val lucidEntriesCount = db.journalEntryDao.getLucidEntriesCount(timeSpan.first, timeSpan.second)
        val totalEntriesCount = db.journalEntryDao.getEntriesCount(timeSpan.first, timeSpan.second)
        val tagCounts = db.journalEntryHasTagDao.getMostUsedTagsList(timeSpan.first!!, timeSpan.second!!, 10)

        val hasJournalEntries = totalEntriesCount != 0
        val hasAvgMoodsData = hasData(avgMoods)
        val hasAvgDreamClarityData = hasData(avgClarities)
        val hasAvgSleepQualityData = hasData(avgQualities)
        val hasDreamCounts = hasData(dreamCounts)
        val hasAvgJournalRatings = hasAvgMoodsData && hasAvgDreamClarityData && hasAvgSleepQualityData && hasDreamCounts
        val hasTagData = !tagCounts.isEmpty()

        requireActivity().runOnUiThread {
            if (!hasJournalEntries) {
                binding.crdNoDataJournal.visibility = View.VISIBLE

                // Hide all stats on dream journal as there is no data available
                binding.crdLucidDreamRatio.visibility = View.GONE
                binding.crdAvgDreamMood.visibility = View.GONE
                binding.crdAvgDreamClarity.visibility = View.GONE
                binding.crdAvgSleepQuality.visibility = View.GONE
                binding.crdOverallJournalRatings.visibility = View.GONE
                binding.crdMostUsedTags.visibility = View.GONE
                return@runOnUiThread
            }

            // Set lucidity chart
            binding.crdNoDataJournal.visibility = View.GONE
            binding.ccgLucidPercentage.setData(
                lucidEntriesCount,
                totalEntriesCount - lucidEntriesCount,
                15.dpToPx.toFloat(),
                1.25.dpToPx.toFloat()
            )

            // Set average mood data
            if (hasAvgMoodsData) {
                generateRodChart(
                    binding.rgAvgDreamMoods,
                    3.dpToPx.toFloat(),
                    moodIcons,
                    avgMoods
                )
            }

            // Set average dream clarity data
            if (hasAvgDreamClarityData) {
                generateRodChart(
                    binding.rgAvgClarities,
                    3.dpToPx.toFloat(),
                    clarityIcons,
                    avgClarities
                )
            }

            // Set average sleep quality data
            if (hasAvgSleepQualityData) {
                generateRodChart(
                    binding.rgAvgSleepQualities,
                    3.dpToPx.toFloat(),
                    qualityIcons,
                    avgQualities
                )
            }

            // Set average journal ratings
            if (hasAvgJournalRatings) {
                generateAverageJournalRatingsStats()
            }

            // Set most used tags
            if (hasTagData) {
                binding.llMostUsedTags.removeAllViews()
                generateMostUsedTagsStats(tagCounts)
            }

            // Show / hide stats based on data availability
            binding.crdLucidDreamRatio.visibility = View.VISIBLE
            binding.crdAvgDreamMood.visibility = if (hasAvgMoodsData) View.VISIBLE else View.GONE
            binding.crdAvgDreamClarity.visibility = if (hasAvgDreamClarityData) View.VISIBLE else View.GONE
            binding.crdAvgSleepQuality.visibility = if (hasAvgSleepQualityData) View.VISIBLE else View.GONE
            binding.crdOverallJournalRatings.visibility = if (hasAvgJournalRatings) View.VISIBLE else View.GONE
            binding.crdMostUsedTags.visibility = if (hasTagData) View.VISIBLE else View.GONE
        }
    }

    fun hasData(data: List<Double>): Boolean = data.any { it != -1.0 }

    private fun generateMostUsedTagsStats(tagCounts: List<TagCount>) {
        val maxCount = tagCounts[0].count
        for (p in tagCounts) {
            val rngProg = RangeProgress(context)
            rngProg.setBackgroundAttrColor(R.attr.colorSurfaceContainer)
            val llParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                25.dpToPx
            )
            val margin = 5.dpToPx
            llParams.setMargins(0, margin, 0, margin)
            rngProg.layoutParams = llParams
            binding.llMostUsedTags.addView(rngProg)
            rngProg.setData(maxCount.toFloat(), p.count.toFloat(), p.tag, null, p.count.toString())
        }
    }

    private fun generateAverageJournalRatingsStats() {
        val averageMood = calcAverage(avgMoods, true)
        val averageClarity = calcAverage(avgClarities, true)
        val averageQuality = calcAverage(avgQualities, true)
        val averageDreamCount = calcAverage(dreamCounts, false)

        binding.rpDreamMood.setData(
            4f,
            averageMood,
            "DREAM MOOD",
            moodIcons[averageMood.roundToInt()],
            null
        )
        binding.rpDreamClarity.setData(
            3f,
            averageClarity,
            "DREAM CLARITY",
            clarityIcons[averageClarity.roundToInt()],
            null
        )
        binding.rpSleepQuality.setData(
            3f,
            averageQuality,
            "SLEEP QUALITY",
            qualityIcons[averageQuality.roundToInt()],
            null
        )
        binding.rpDreamsPerNight.setData(
            Collections.max(dreamCounts).toFloat(),
            averageDreamCount,
            "DREAMS PER NIGHT",
            null,
            String.format(Locale.ENGLISH, "%.2f", averageDreamCount)
        )
    }

    private fun calcAverage(vals: MutableList<Double>, ignoreMissedDays: Boolean): Float {
        if (vals.isEmpty()) {
            return 0f
        }
        var sum = 0.0
        var i = 0
        for (d in vals) {
            if (ignoreMissedDays && d == -1.0) {
                continue
            }
            sum += if (d == -1.0) 0.0 else d
            i++
        }
        return (sum / i).toFloat()
    }

    private fun generateRodChart(rg: RodGraph, lineWidth: Float, icons: Array<Drawable>, averageValues: MutableList<Double>) {
        val data: MutableList<DataValue> = arrayListOf()
        val calendar = Calendar.getInstance()
        val df = SimpleDateFormat("d\nMMM", Locale.getDefault())
        for (j in averageValues.indices) {
            val time = calendar.time
            val label = when {
                averageValues.size <= 7 -> df.format(time)
                averageValues.size <= 31 && j % 6 == 0 -> df.format(time)
                j % 10 == 0 -> df.format(time)   // TODO: use 1/7 of total entry count and guarantee that last day is written and probably do not use rod graph or use averages of days (=> loss of accuracy)
                else -> null
            }
            data.add(DataValue(averageValues[j], label))
            calendar.add(Calendar.DAY_OF_MONTH, -1)
        }
        rg.setData(data, lineWidth, 24.dpToPx, icons)
        rg.minimumHeight = rg.minHeight
    }

    private suspend fun getAveragesForLastNDays(amount: Int, daysBeforeToday: Int) {
        val isLastDayToCheck = daysBeforeToday == amount - 1
        val timeSpan = Tools.getTimeSpanFrom(daysBeforeToday, false)
        val averageEntryValues = db.journalEntryDao.getAverageEntryInTimeSpan(timeSpan.first, timeSpan.second)

        if (daysBeforeToday == 0) {
            avgQualities.clear()
            avgMoods.clear()
            avgClarities.clear()
            dreamCounts.clear()
        }

        val hasJournalEntries = averageEntryValues.dreamCount > 0
        avgQualities.add(if (hasJournalEntries) averageEntryValues.avgQualities else -1.0)
        avgMoods.add(if (hasJournalEntries) averageEntryValues.avgMoods else -1.0)
        avgClarities.add(if (hasJournalEntries) averageEntryValues.avgClarities else -1.0)
        dreamCounts.add(if (hasJournalEntries) averageEntryValues.dreamCount else -1.0)

        if (!isLastDayToCheck) {
            getAveragesForLastNDays(amount, daysBeforeToday + 1)
        }
    }
}