package com.bitflaker.lucidsourcekit.main.statistics;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.views.CircleGraph;
import com.bitflaker.lucidsourcekit.data.records.DataValue;
import com.bitflaker.lucidsourcekit.views.HeatmapChart;
import com.bitflaker.lucidsourcekit.views.IconCircleHeatmap;
import com.bitflaker.lucidsourcekit.views.IconOutOf;
import com.bitflaker.lucidsourcekit.views.ProportionLineChart;
import com.bitflaker.lucidsourcekit.views.RangeProgress;
import com.bitflaker.lucidsourcekit.views.RodGraph;
import com.bitflaker.lucidsourcekit.data.usage.AppUsage;
import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.resulttables.AverageEntryValues;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.resulttables.TagCount;
import com.bitflaker.lucidsourcekit.database.goals.entities.resulttables.ShuffleHasGoalStats;
import com.bitflaker.lucidsourcekit.utils.Tools;
import com.bitflaker.lucidsourcekit.data.datastore.DataStoreKeys;
import com.bitflaker.lucidsourcekit.data.datastore.DataStoreManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class StatisticsView extends Fragment {
    private RodGraph rgAvgDreamMoods, rgAvgDreamClarities, rgAvgSleepQualities;
    private MaterialCardView crdLucidDreamRatio, crdOverallJournalRatings, crdAvgDreamMoods, crdAvgDreamClarity, crdAvgSleepQuality, crdMostUsedTags, crdNoDataJournal, crdNoDataGoals;
    private LinearLayout mostUsedTagsContainer;
    private LinearLayout goalsReachedContainer;
    private ChipGroup chartTimeSpan;
    private CircleGraph lucidPercentage;
    private TextView totalJournalEntries, totalTagCount, totalGoalCount, timeSpent, averageSessionCount, averageSessionLength;
    private RangeProgress rpDreamMood, rpDreamClarity, rpSleepQuality, rpDreamsPerNight, rpGoalsReached, rpAvgDiff;
    private MainDatabase db;
    private final List<Double> avgClarities = new ArrayList<>();
    private final List<Double> avgMoods = new ArrayList<>();
    private final List<Double> avgQualities = new ArrayList<>();
    private final List<Double> dreamCounts = new ArrayList<>();
    private Drawable[] moodIcons;
    private Drawable[] clarityIcons;
    private Drawable[] qualityIcons;
    private int selectedDaysCount = 7;
    private CompositeDisposable compositeDisposable;
    private HeatmapChart heatmapChart;
    private IconOutOf streakCheckIns;
    private ProportionLineChart timeSpentProportions;
    private IconCircleHeatmap sessionHeatmap;
    private MaterialButton dreamFrequencyTypeFilter;
    private final DecimalFormat df = new DecimalFormat("0.0");

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main_statistics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getView().findViewById(R.id.txt_stats_heading).setLayoutParams(Tools.getRelativeLayoutParamsTopStatusbar(getContext()));
        compositeDisposable = new CompositeDisposable();

        mostUsedTagsContainer = getView().findViewById(R.id.ll_most_used_tags);
        rgAvgDreamMoods = getView().findViewById(R.id.rg_avg_dream_moods);
        rgAvgDreamClarities = getView().findViewById(R.id.rg_avg_clarities);
        rgAvgSleepQualities = getView().findViewById(R.id.rg_avg_sleep_qualities);
        chartTimeSpan = getView().findViewById(R.id.chp_grp_time_span);
        lucidPercentage = getView().findViewById(R.id.ccg_lucid_percentage);
        streakCheckIns = getView().findViewById(R.id.ioo_streak_check_in);
        rpDreamMood = getView().findViewById(R.id.rp_dream_mood);
        rpDreamClarity = getView().findViewById(R.id.rp_dream_clarity);
        rpSleepQuality = getView().findViewById(R.id.rp_sleep_quality);
        rpDreamsPerNight = getView().findViewById(R.id.rp_dreams_per_night);
        goalsReachedContainer = getView().findViewById(R.id.ll_goals_reached);
        rpGoalsReached = getView().findViewById(R.id.rp_goals_reached);
        rpAvgDiff = getView().findViewById(R.id.rp_avg_goal_diff);
        totalJournalEntries = getView().findViewById(R.id.txt_total_journal_entries);
        totalTagCount = getView().findViewById(R.id.txt_total_tag_count);
        totalGoalCount = getView().findViewById(R.id.txt_total_goal_count);
        dreamFrequencyTypeFilter = getView().findViewById(R.id.btn_dream_frequency_filter);

        crdLucidDreamRatio = getView().findViewById(R.id.crd_lucid_dream_ratio);
        crdOverallJournalRatings = getView().findViewById(R.id.crd_overall_journal_ratings);
        crdAvgDreamMoods = getView().findViewById(R.id.crd_avg_dream_mood);
        crdAvgDreamClarity = getView().findViewById(R.id.crd_avg_dream_clarity);
        crdAvgSleepQuality = getView().findViewById(R.id.crd_avg_sleep_quality);
        crdMostUsedTags = getView().findViewById(R.id.crd_most_used_tags);
        crdNoDataJournal = getView().findViewById(R.id.crd_no_data_journal);

        crdNoDataGoals = getView().findViewById(R.id.crd_no_data_goals);
        heatmapChart = getView().findViewById(R.id.htm_dream_count_heatmap);
        timeSpentProportions = getView().findViewById(R.id.pc_time_spent_proportions);
        sessionHeatmap = getView().findViewById(R.id.ich_session_heatmap);
        timeSpent = getView().findViewById(R.id.txt_time_spent_value);

        averageSessionCount = getView().findViewById(R.id.txt_averageSessionCount);
        averageSessionLength = getView().findViewById(R.id.txt_AverageSessionLength);

        db = MainDatabase.getInstance(getContext());

        dreamFrequencyTypeFilter.setOnClickListener(e -> {
            Drawable arrowUp = ResourcesCompat.getDrawable(getResources(), R.drawable.rounded_keyboard_arrow_up_24, getContext().getTheme());
            Drawable arrowDown = ResourcesCompat.getDrawable(getResources(), R.drawable.rounded_keyboard_arrow_down_24, getContext().getTheme());

            dreamFrequencyTypeFilter.setCompoundDrawablesWithIntrinsicBounds(null, null, arrowUp, null);
            PopupMenu popup = new PopupMenu(new ContextThemeWrapper(getContext(), R.style.Theme_LucidSourceKit_PopupMenu_Icon), dreamFrequencyTypeFilter);
            popup.setForceShowIcon(true);
            popup.getMenuInflater().inflate(R.menu.dream_frequency_types, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                dreamFrequencyTypeFilter.setText(item.getTitle());
                return true;
            });
            popup.setOnDismissListener(menu -> {
                dreamFrequencyTypeFilter.setCompoundDrawablesWithIntrinsicBounds(null, null, arrowDown, null);
            });
            popup.show();
        });

        // Set stats for app usage (time spent, session count, session length, ...)
        AppUsage.AppUsageEvents appUsage = AppUsage.Companion.getUsageStats(getContext(), 1000 * 60 * 60 * 24 * 7);
        List<AppUsage.AppOpenStats> appOpenTimes = appUsage.getAppOpenTimeStamps();
        long totalTime = appUsage.getTotalTime();
        float journalTime = appUsage.getJournalTime() / 1000.0f;
        float otherTime = totalTime / 1000.0f - journalTime;
        setAppUsageStats(totalTime, journalTime, otherTime);
        if (!appOpenTimes.isEmpty()) {
            setAppSessionStats(appOpenTimes);
        }

        heatmapChart.setOnWeekCountCalculatedListener(weekCount -> {
            Calendar calendar = Calendar.getInstance();
            calendar.setFirstDayOfWeek(Calendar.MONDAY);
            int dayOfWeekIndex = calendar.get(Calendar.DAY_OF_WEEK) - 2;
            dayOfWeekIndex = dayOfWeekIndex == -1 ? 6 : dayOfWeekIndex;

            calendar.add(Calendar.HOUR, dayOfWeekIndex * -24);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            calendar.add(Calendar.HOUR, -24 * 7 * weekCount);

            long timeFrom = calendar.getTimeInMillis();
            List<Long> timestamps = db.getJournalEntryDao().getEntriesFrom(timeFrom).blockingGet();
            heatmapChart.setTimestamps(timestamps);
        });

        moodIcons = Tools.getIconsDreamMood(getContext());
        clarityIcons = Tools.getIconsDreamClarity(getContext());
        qualityIcons = Tools.getIconsSleepQuality(getContext());

        long currentStreakValue = DataStoreManager.getInstance().getSetting(DataStoreKeys.APP_OPEN_STREAK).blockingFirst();
        long bestStreakValue = DataStoreManager.getInstance().getSetting(DataStoreKeys.APP_OPEN_STREAK_LONGEST).blockingFirst();
        streakCheckIns.setValue((int)currentStreakValue);
        streakCheckIns.setMaxValue((int)bestStreakValue);

        // TODO: add loading indicators while gathering data
        // TODO: refresh stats after entry modified/added/deleted

        compositeDisposable.add(getAveragesForLastNDays(selectedDaysCount, 0)
                .subscribeOn(Schedulers.io())
                .subscribe(this::updateStats));

        chartTimeSpan.setOnCheckedStateChangeListener((chipGroup, checkedIds) -> {
            if(checkedIds.size() != 1) {
                Log.e("Statistics", "Chart Timespan chip group has more than one selected item");
                return;
            }

            int selectedId = checkedIds.get(0);
            if(selectedId == R.id.chp_last_7_days) {
                selectedDaysCount = 7;
            }
            else if(selectedId == R.id.chp_last_30_days) {
                selectedDaysCount = 30;
            }
            else if(selectedId == R.id.chp_all_time) {
                selectedDaysCount = 60;    // TODO: make it actually all time (but fix performance issues)
            }
            compositeDisposable.add(getAveragesForLastNDays(selectedDaysCount, 0)
                    .subscribeOn(Schedulers.io())
                    .subscribe(this::updateStats));
        });
    }

    private void setAppUsageStats(long totalTime, float journalTime, float otherTime) {
        long hours = TimeUnit.MILLISECONDS.toHours(totalTime);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(totalTime) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(totalTime));
        timeSpentProportions.setValues(new ProportionLineChart.DataPoint[] {
                new ProportionLineChart.DataPoint(Tools.getAttrColor(R.attr.colorPrimary, getContext().getTheme()), journalTime, "Dream journal"),
                new ProportionLineChart.DataPoint(Tools.getAttrColor(R.attr.colorTertiary, getContext().getTheme()), 0, "Binaural beats"),
                new ProportionLineChart.DataPoint(Tools.getAttrColor(R.attr.colorSecondary, getContext().getTheme()), otherTime, "Other")
        });
        timeSpent.setText(hours == 0 ? String.format(Locale.ENGLISH, "%d min", minutes) : String.format(Locale.ENGLISH, "%d hr %d min", hours, minutes));
    }

    private void setAppSessionStats(List<AppUsage.AppOpenStats> appOpenTimes) {
        HashMap<Long, List<Long>> daySessionDurations = groupSessionDurationsByDay(appOpenTimes);
        double averageDailyAppOpens = daySessionDurations.values()
                .stream()
                .mapToDouble(List::size)
                .average()
                .orElse(0);
        int averageSessionLengthMs = (int) daySessionDurations.values()
                .stream()
                .filter(x -> !x.isEmpty())
                .mapToDouble(x -> x.stream().mapToDouble(d -> d).average().orElse(0))
                .average()
                .orElse(0);
        long averageSessionLengthMin = TimeUnit.MILLISECONDS.toMinutes(averageSessionLengthMs);
        averageSessionCount.setText(df.format(averageDailyAppOpens));
        averageSessionLength.setText(String.format(Locale.getDefault(), "%d min", averageSessionLengthMin));
        sessionHeatmap.setTimestamps(appOpenTimes.stream()
                .map(x -> x.getOpenedAt() - Tools.getMidnightTime(x.getOpenedAt()))
                .collect(Collectors.toList())
        );
    }

    private static HashMap<Long, List<Long>> groupSessionDurationsByDay(List<AppUsage.AppOpenStats> appOpenTimes) {
        HashMap<Long, List<Long>> daySessionDurations = new HashMap<>();
        long currentMidnight = Tools.getMidnightTime();
        for (int i = 0; i < 7; i++) {
            daySessionDurations.put(currentMidnight - 1000 * 60 * 60 * 24 * i, new ArrayList<>());
        }
        for (int i = 0; i < appOpenTimes.size(); i++) {
            AppUsage.AppOpenStats appOpenStats = appOpenTimes.get(i);
            Long midnightTime = Tools.getMidnightTime(appOpenStats.getOpenedAt());
            if (daySessionDurations.containsKey(midnightTime)) {
                Objects.requireNonNull(daySessionDurations.get(midnightTime)).add(appOpenStats.getOpenFor());
            }
        }
        return daySessionDurations;
    }

    @Override
    public void onDestroyView() {
        compositeDisposable.clear();
        super.onDestroyView();
    }

    private void updateStats() {
        Pair<Long, Long> timeSpan = Tools.getTimeSpanFrom(selectedDaysCount - 1, true);

        generateStaticStats().blockingSubscribe();
        generateDreamJournalStats(timeSpan).blockingSubscribe();
        generateDailyGoalsStats(timeSpan).blockingSubscribe();
    }

    private Completable generateStaticStats() {
        return Completable.fromAction(() -> {
            int journalEntriesCount = db.getJournalEntryDao().getTotalEntriesCount().blockingGet();
            int tagCount = db.getJournalEntryTagDao().getTotalTagCount().blockingGet();
            int goalCount = db.getGoalDao().getGoalCount().blockingGet();

            getActivity().runOnUiThread(() -> {
                totalJournalEntries.setText(String.format(Locale.getDefault(), "%d", journalEntriesCount));
                totalTagCount.setText(String.format(Locale.getDefault(), "%d", tagCount));
                totalGoalCount.setText(String.format(Locale.getDefault(), "%d", goalCount));
            });
        });
    }

    private Completable generateDailyGoalsStats(Pair<Long, Long> timeSpan) {
        return Completable.fromAction(() -> {
            ShuffleHasGoalStats goalShuffleData = db.getShuffleHasGoalDao().getShufflesFromBetween(timeSpan.first, timeSpan.second).blockingGet();

            boolean hasGoalShuffleData = goalShuffleData.goalCount > 0;

            getActivity().runOnUiThread(() -> {
                if (hasGoalShuffleData) {
                    rpGoalsReached.setData(goalShuffleData.goalCount, goalShuffleData.achievedCount, "ACHIEVED", null, String.format(Locale.ENGLISH, "%d/%d", goalShuffleData.achievedCount, goalShuffleData.goalCount));
                    rpAvgDiff.setData(3, (float) goalShuffleData.avgDifficulty, "AVERAGE DIFFICULTY LEVEL", null, String.format(Locale.ENGLISH, "%.2f", goalShuffleData.avgDifficulty));

                    goalsReachedContainer.setVisibility(View.VISIBLE);
                }
                else {
                    crdNoDataGoals.setVisibility(View.VISIBLE);

                    // Hide all stats on daily goals as there is no data available
                    goalsReachedContainer.setVisibility(View.GONE);
                }
            });
        });
    }

    private Completable generateDreamJournalStats(Pair<Long, Long> timeSpan) {
        return Completable.fromAction(() -> {
            int lucidEntriesCount = db.getJournalEntryDao().getLucidEntriesCount(timeSpan.first, timeSpan.second).blockingGet();
            int totalEntriesCount = db.getJournalEntryDao().getEntriesCount(timeSpan.first, timeSpan.second).blockingGet();
            List<TagCount> tagCounts = db.getJournalEntryHasTagDao().getMostUsedTagsList(timeSpan.first, timeSpan.second, 10).blockingGet();

            boolean hasJournalEntries = totalEntriesCount != 0;
            boolean hasAvgMoodsData = !Tools.hasNoData(avgMoods);
            boolean hasAvgDreamClarityData = !Tools.hasNoData(avgClarities);
            boolean hasAvgSleepQualityData = !Tools.hasNoData(avgQualities);
            boolean hasAvgJournalRatings = !Tools.hasNoData(avgMoods) && !Tools.hasNoData(avgClarities) && !Tools.hasNoData(avgQualities) && !Tools.hasNoData(dreamCounts);
            boolean hasTagData = tagCounts.size() > 0;

            getActivity().runOnUiThread(() -> {
                if (hasJournalEntries) {
                    crdNoDataJournal.setVisibility(View.GONE);
                    lucidPercentage.setData(lucidEntriesCount, totalEntriesCount - lucidEntriesCount, Tools.dpToPx(getContext(), 15), Tools.dpToPx(getContext(), 1.25));
                    if (hasAvgMoodsData) {
                        generateRodChart(rgAvgDreamMoods, Tools.dpToPx(getContext(), 3f), moodIcons, avgMoods);
                    }
                    if (hasAvgDreamClarityData) {
                        generateRodChart(rgAvgDreamClarities, Tools.dpToPx(getContext(), 3f), clarityIcons, avgClarities);
                    }
                    if (hasAvgSleepQualityData) {
                        generateRodChart(rgAvgSleepQualities, Tools.dpToPx(getContext(), 3f), qualityIcons, avgQualities);
                    }
                    if (hasAvgJournalRatings) {
                        generateAverageJournalRatingsStats();
                    }
                    if (hasTagData) {
                        mostUsedTagsContainer.removeAllViews();
                        generateMostUsedTagsStats(tagCounts);
                    }

                    crdLucidDreamRatio.setVisibility(View.VISIBLE);
                    crdAvgDreamMoods.setVisibility(hasAvgMoodsData ? View.VISIBLE : View.GONE);
                    crdAvgDreamClarity.setVisibility(hasAvgDreamClarityData ? View.VISIBLE : View.GONE);
                    crdAvgSleepQuality.setVisibility(hasAvgSleepQualityData ? View.VISIBLE : View.GONE);
                    crdOverallJournalRatings.setVisibility(hasAvgJournalRatings ? View.VISIBLE : View.GONE);
                    crdMostUsedTags.setVisibility(hasTagData ? View.VISIBLE : View.GONE);
                }
                else {
                    crdNoDataJournal.setVisibility(View.VISIBLE);

                    // Hide all stats on dream journal as there is no data available
                    crdAvgDreamMoods.setVisibility(View.GONE);
                    crdAvgDreamClarity.setVisibility(View.GONE);
                    crdAvgSleepQuality.setVisibility(View.GONE);
                    crdOverallJournalRatings.setVisibility(View.GONE);
                    crdLucidDreamRatio.setVisibility(View.GONE);
                    crdMostUsedTags.setVisibility(View.GONE);
                }
            });
        });
    }

    private void generateMostUsedTagsStats(List<TagCount> tagCounts) {
        int maxCount = tagCounts.get(0).count();
        for (TagCount p : tagCounts) {
            RangeProgress rngProg = new RangeProgress(getContext());
            LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Tools.dpToPx(getContext(), 25));
            int margin = Tools.dpToPx(getContext(), 5);
            llParams.setMargins(0, margin, 0, margin);
            rngProg.setLayoutParams(llParams);
            mostUsedTagsContainer.addView(rngProg);
            rngProg.setData(maxCount, p.count(), p.tag(), null, Integer.toString(p.count()));
        }
    }

    private void generateAverageJournalRatingsStats() {
        float averageMood = calcAverage(avgMoods, true);
        float averageClarity = calcAverage(avgClarities, true);
        float averageQuality = calcAverage(avgQualities, true);
        float averageDreamCount = calcAverage(dreamCounts, false);

        rpDreamMood.setData(4, averageMood, "DREAM MOOD", moodIcons[Math.round(averageMood)], null);
        rpDreamClarity.setData(3, averageClarity, "DREAM CLARITY", clarityIcons[Math.round(averageClarity)], null);
        rpSleepQuality.setData(3, averageQuality, "SLEEP QUALITY", qualityIcons[Math.round(averageQuality)], null);
        rpDreamsPerNight.setData(Collections.max(dreamCounts).floatValue(), averageDreamCount, "DREAMS PER NIGHT", null, String.format(Locale.ENGLISH, "%.2f", averageDreamCount));
    }

    private float calcAverage(List<Double> vals, boolean ignoreMissedDays) {
        if(vals.size() == 0){ return 0; }
        double sum = 0;
        int i = 0;
        for (Double d : vals) {
            if(ignoreMissedDays && d == -1.0) {
                continue;
            }
            sum += d == -1 ? 0 : d;
            i++;
        }
        return (float)(sum / (double)i);
    }

    private void generateRodChart(RodGraph rg, float lineWidth, Drawable[] icons, List<Double> averageValues) {
        List<DataValue> data = new ArrayList<>();
        Calendar cldr = new GregorianCalendar(TimeZone.getDefault());
        cldr.setTime(Calendar.getInstance().getTime());
        SimpleDateFormat df = new SimpleDateFormat("d\nMMM");
        for (int j = 0; j < averageValues.size(); j++) {
            String label = null;
            if(averageValues.size() <= 7){
                label = df.format(cldr.getTime());
            }
            else if (averageValues.size() <= 31) {
                if(j % 6 == 0) {
                    label = df.format(cldr.getTime());
                }
            }
            else {
                // TODO: use 1/7 of total entry count and guarantee that last day is written and probably do not use rod graph or use averages of days (=> loss of accuracy)
                if(j % 10 == 0) {
                    label = df.format(cldr.getTime());
                }
            }
            data.add(new DataValue(averageValues.get(j), label));
            cldr.add(Calendar.DAY_OF_MONTH, -1);
        }
        rg.setData(data, lineWidth, Tools.dpToPx(getContext(), 24), icons);
        rg.setMinimumHeight(rg.getMinHeight());
    }

    private Completable getAveragesForLastNDays(int amount, int daysBeforeToday) {
        return Completable.fromAction(() -> {
            boolean isLastDayToCheck = daysBeforeToday == amount - 1;
            Pair<Long, Long> timeSpan = Tools.getTimeSpanFrom(daysBeforeToday, false);
            AverageEntryValues averageEntryValues = db.getJournalEntryDao().getAverageEntryInTimeSpan(timeSpan.first, timeSpan.second).blockingGet();

            if(daysBeforeToday == 0) {
                avgQualities.clear();
                avgMoods.clear();
                avgClarities.clear();
                dreamCounts.clear();
            }

            boolean hasJournalEntries = averageEntryValues.dreamCount() > 0;
            avgQualities.add(hasJournalEntries ? averageEntryValues.avgQualities() : -1.0);
            avgMoods.add(hasJournalEntries ? averageEntryValues.avgMoods() : -1.0);
            avgClarities.add(hasJournalEntries ? averageEntryValues.avgClarities() : -1.0);
            dreamCounts.add(hasJournalEntries ? averageEntryValues.dreamCount() : -1.0);

            if(!isLastDayToCheck) {
                getAveragesForLastNDays(amount, daysBeforeToday + 1).blockingSubscribe();
            }
        });
    }
}