package com.bitflaker.lucidsourcekit.main.statistics;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.data.datastore.DataStoreKeys;
import com.bitflaker.lucidsourcekit.data.datastore.DataStoreManager;
import com.bitflaker.lucidsourcekit.data.records.DataValue;
import com.bitflaker.lucidsourcekit.data.usage.AppUsage;
import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.resulttables.AverageEntryValues;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.resulttables.TagCount;
import com.bitflaker.lucidsourcekit.database.goals.entities.resulttables.ShuffleHasGoalStats;
import com.bitflaker.lucidsourcekit.databinding.FragmentMainStatisticsBinding;
import com.bitflaker.lucidsourcekit.utils.Tools;
import com.bitflaker.lucidsourcekit.views.ProportionLineChart;
import com.bitflaker.lucidsourcekit.views.RangeProgress;
import com.bitflaker.lucidsourcekit.views.RodGraph;

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
    private MainDatabase db;
    private final List<Double> avgClarities = new ArrayList<>();
    private final List<Double> avgMoods = new ArrayList<>();
    private final List<Double> avgQualities = new ArrayList<>();
    private final List<Double> dreamCounts = new ArrayList<>();
    private Drawable[] moodIcons;
    private Drawable[] clarityIcons;
    private Drawable[] qualityIcons;
    private int selectedDaysCount = 7;
    private final DecimalFormat df = new DecimalFormat("0.0");
    private CompositeDisposable compositeDisposable;
    private FragmentMainStatisticsBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMainStatisticsBinding.inflate(getLayoutInflater());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        compositeDisposable = new CompositeDisposable();
        db = MainDatabase.getInstance(getContext());

        binding.txtStatsHeading.setLayoutParams(Tools.getRelativeLayoutParamsTopStatusbar(getContext()));
        binding.btnDreamFrequencyFilter.setOnClickListener(e -> {
            Drawable arrowUp = ResourcesCompat.getDrawable(getResources(), R.drawable.rounded_keyboard_arrow_up_24, getContext().getTheme());
            Drawable arrowDown = ResourcesCompat.getDrawable(getResources(), R.drawable.rounded_keyboard_arrow_down_24, getContext().getTheme());

            binding.btnDreamFrequencyFilter.setCompoundDrawablesWithIntrinsicBounds(null, null, arrowUp, null);
            PopupMenu popup = new PopupMenu(new ContextThemeWrapper(getContext(), R.style.Theme_LucidSourceKit_PopupMenu_Icon), binding.btnDreamFrequencyFilter);
            popup.setForceShowIcon(true);
            popup.getMenuInflater().inflate(R.menu.dream_frequency_types, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                binding.btnDreamFrequencyFilter.setText(item.getTitle());
                return true;
            });
            popup.setOnDismissListener(menu -> {
                binding.btnDreamFrequencyFilter.setCompoundDrawablesWithIntrinsicBounds(null, null, arrowDown, null);
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

        binding.htmDreamCountHeatmap.setOnWeekCountCalculatedListener(weekCount -> {
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
            binding.htmDreamCountHeatmap.setTimestamps(timestamps);
        });

        moodIcons = Tools.getIconsDreamMood(getContext());
        clarityIcons = Tools.getIconsDreamClarity(getContext());
        qualityIcons = Tools.getIconsSleepQuality(getContext());

        long currentStreakValue = DataStoreManager.getInstance().getSetting(DataStoreKeys.APP_OPEN_STREAK).blockingFirst();
        long bestStreakValue = DataStoreManager.getInstance().getSetting(DataStoreKeys.APP_OPEN_STREAK_LONGEST).blockingFirst();
        binding.iooStreakCheckIn.setValue((int)currentStreakValue);
        binding.iooStreakCheckIn.setMaxValue((int)bestStreakValue);

        // TODO: add loading indicators while gathering data
        // TODO: refresh stats after entry modified/added/deleted

        compositeDisposable.add(getAveragesForLastNDays(selectedDaysCount, 0)
                .subscribeOn(Schedulers.io())
                .subscribe(this::updateStats));

        binding.chpGrpTimeSpan.setOnCheckedStateChangeListener((chipGroup, checkedIds) -> {
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
        binding.pcTimeSpentProportions.setValues(new ProportionLineChart.DataPoint[] {
                new ProportionLineChart.DataPoint(Tools.getAttrColor(R.attr.colorPrimary, getContext().getTheme()), journalTime, "Dream journal"),
                new ProportionLineChart.DataPoint(Tools.getAttrColor(R.attr.colorTertiary, getContext().getTheme()), 0, "Binaural beats"),
                new ProportionLineChart.DataPoint(Tools.getAttrColor(R.attr.colorSecondary, getContext().getTheme()), otherTime, "Other")
        });
        binding.txtTimeSpentValue.setText(hours == 0 ? String.format(Locale.ENGLISH, "%d min", minutes) : String.format(Locale.ENGLISH, "%d hr %d min", hours, minutes));
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
        binding.txtAverageSessionCount.setText(df.format(averageDailyAppOpens));
        binding.txtAverageSessionLength.setText(String.format(Locale.getDefault(), "%d min", averageSessionLengthMin));
        binding.ichSessionHeatmap.setTimestamps(appOpenTimes.stream()
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
                binding.txtTotalJournalEntries.setText(String.format(Locale.getDefault(), "%d", journalEntriesCount));
                binding.txtTotalTagCount.setText(String.format(Locale.getDefault(), "%d", tagCount));
                binding.txtTotalGoalCount.setText(String.format(Locale.getDefault(), "%d", goalCount));
            });
        });
    }

    private Completable generateDailyGoalsStats(Pair<Long, Long> timeSpan) {
        return Completable.fromAction(() -> {
            ShuffleHasGoalStats goalShuffleData = db.getShuffleHasGoalDao().getShufflesFromBetween(timeSpan.first, timeSpan.second).blockingGet();
            boolean hasGoalShuffleData = goalShuffleData.goalCount > 0;

            getActivity().runOnUiThread(() -> {
                if (hasGoalShuffleData) {
                    binding.rpGoalsReached.setData(goalShuffleData.goalCount, goalShuffleData.achievedCount, "ACHIEVED", null, String.format(Locale.ENGLISH, "%d/%d", goalShuffleData.achievedCount, goalShuffleData.goalCount));
                    binding.rpAvgGoalDiff.setData(3, (float) goalShuffleData.avgDifficulty, "AVERAGE DIFFICULTY LEVEL", null, String.format(Locale.ENGLISH, "%.2f", goalShuffleData.avgDifficulty));
                    binding.llGoalsReached.setVisibility(View.VISIBLE);
                }
                else {
                    binding.crdNoDataGoals.setVisibility(View.VISIBLE);
                    binding.llGoalsReached.setVisibility(View.GONE);
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
            boolean hasTagData = !tagCounts.isEmpty();

            getActivity().runOnUiThread(() -> {
                if (hasJournalEntries) {
                    binding.crdNoDataJournal.setVisibility(View.GONE);
                    binding.ccgLucidPercentage.setData(lucidEntriesCount, totalEntriesCount - lucidEntriesCount, Tools.dpToPx(getContext(), 15), Tools.dpToPx(getContext(), 1.25));
                    if (hasAvgMoodsData) {
                        generateRodChart(binding.rgAvgDreamMoods, Tools.dpToPx(getContext(), 3f), moodIcons, avgMoods);
                    }
                    if (hasAvgDreamClarityData) {
                        generateRodChart(binding.rgAvgClarities, Tools.dpToPx(getContext(), 3f), clarityIcons, avgClarities);
                    }
                    if (hasAvgSleepQualityData) {
                        generateRodChart(binding.rgAvgSleepQualities, Tools.dpToPx(getContext(), 3f), qualityIcons, avgQualities);
                    }
                    if (hasAvgJournalRatings) {
                        generateAverageJournalRatingsStats();
                    }
                    if (hasTagData) {
                        binding.llMostUsedTags.removeAllViews();
                        generateMostUsedTagsStats(tagCounts);
                    }

                    binding.crdLucidDreamRatio.setVisibility(View.VISIBLE);
                    binding.crdAvgDreamMood.setVisibility(hasAvgMoodsData ? View.VISIBLE : View.GONE);
                    binding.crdAvgDreamClarity.setVisibility(hasAvgDreamClarityData ? View.VISIBLE : View.GONE);
                    binding.crdAvgSleepQuality.setVisibility(hasAvgSleepQualityData ? View.VISIBLE : View.GONE);
                    binding.crdOverallJournalRatings.setVisibility(hasAvgJournalRatings ? View.VISIBLE : View.GONE);
                    binding.crdMostUsedTags.setVisibility(hasTagData ? View.VISIBLE : View.GONE);
                }
                else {
                    binding.crdNoDataJournal.setVisibility(View.VISIBLE);

                    // Hide all stats on dream journal as there is no data available
                    binding.crdLucidDreamRatio.setVisibility(View.GONE);
                    binding.crdAvgDreamMood.setVisibility(View.GONE);
                    binding.crdAvgDreamClarity.setVisibility(View.GONE);
                    binding.crdAvgSleepQuality.setVisibility(View.GONE);
                    binding.crdOverallJournalRatings.setVisibility(View.GONE);
                    binding.crdMostUsedTags.setVisibility(View.GONE);
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
            binding.llMostUsedTags.addView(rngProg);
            rngProg.setData(maxCount, p.count(), p.tag(), null, Integer.toString(p.count()));
        }
    }

    private void generateAverageJournalRatingsStats() {
        float averageMood = calcAverage(avgMoods, true);
        float averageClarity = calcAverage(avgClarities, true);
        float averageQuality = calcAverage(avgQualities, true);
        float averageDreamCount = calcAverage(dreamCounts, false);

        binding.rpDreamMood.setData(4, averageMood, "DREAM MOOD", moodIcons[Math.round(averageMood)], null);
        binding.rpDreamClarity.setData(3, averageClarity, "DREAM CLARITY", clarityIcons[Math.round(averageClarity)], null);
        binding.rpSleepQuality.setData(3, averageQuality, "SLEEP QUALITY", qualityIcons[Math.round(averageQuality)], null);
        binding.rpDreamsPerNight.setData(Collections.max(dreamCounts).floatValue(), averageDreamCount, "DREAMS PER NIGHT", null, String.format(Locale.ENGLISH, "%.2f", averageDreamCount));
    }

    private float calcAverage(List<Double> vals, boolean ignoreMissedDays) {
        if(vals.isEmpty()) { return 0; }
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