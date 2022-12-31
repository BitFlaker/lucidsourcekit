package com.bitflaker.lucidsourcekit.general;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.util.Pair;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.database.goals.entities.Goal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class Tools {
    private static int THEME_DIALOG;
    private static int THEME_POPUP;

    public static void setThemeColors(int theme){
        if(theme == R.style.Theme_LucidSourceKit_Light) {
            THEME_DIALOG = R.style.ThemedDialog_Light;
            THEME_POPUP = R.style.PopupMenu_Light;
        }
        else if(theme == R.style.Theme_LucidSourceKit_LCDark) { // Todo should be removed
            THEME_DIALOG = R.style.ThemedDialog_LCDark;
            THEME_POPUP = R.style.PopupMenu_LCDark;
        }
        else if(theme == R.style.Theme_LucidSourceKit_Amoled_Dark) {
            THEME_DIALOG = R.style.ThemedDialog_Amoled_Dark;
            THEME_POPUP = R.style.PopupMenu_LCDark;
        }
        else if(theme == R.style.Theme_LucidSourceKit_Dark) {
            THEME_DIALOG = R.style.ThemedDialog_Dark;
            THEME_POPUP = R.style.PopupMenu_Dark;
        }
    }

    public static int getThemeDialog(){
        return THEME_DIALOG;
    }

    public static int getPopupTheme() {
        return THEME_POPUP;
    }

    public static void loadLanguage(Activity activity){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        String lang = preferences.getString("lang", null);
        if (lang == null) {
            lang = "en";
            preferences.edit().putString("lang", lang).commit();
        }
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        activity.getBaseContext().getResources().updateConfiguration(config, activity.getBaseContext().getResources().getDisplayMetrics());
    }

    public static ColorStateList getAttrColorStateList(int colorAttr, Resources.Theme theme) {
        TypedValue typedValue = getAttrValue(colorAttr, theme);
        @ColorInt int color = typedValue.data;
        return ColorStateList.valueOf(color);
    }

    public static int getAttrColor(int colorAttr, Resources.Theme theme) {
        TypedValue typedValue = getAttrValue(colorAttr, theme);
        @ColorInt int color = typedValue.data;
        return color;
    }

    @NonNull
    private static TypedValue getAttrValue(int attr, Resources.Theme theme) {
        TypedValue typedValue = new TypedValue();
        theme.resolveAttribute(attr, typedValue, true);
        return typedValue;
    }

    public static void makeStatusBarTransparent(Activity activity) {
        activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
    }

    public static void colorStatusBar(Activity activity, int color, Resources.Theme theme) {
        activity.getWindow().setStatusBarColor(getAttrColor(color, theme));
    }

    public static int dpToPx(Context context, double dp) {
        return (int)(dp * context.getResources().getDisplayMetrics().density);
    }

    public static int spToPx(Context context, float sp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.getResources().getDisplayMetrics());
    }

    public static int pxToDp(Context context, double px) {
        return (int)(px / context.getResources().getDisplayMetrics().density);
    }

    public static <T> T[] addFirst(T[] elements, T element)
    {
        T[] finishedArray = Arrays.copyOf(elements, elements.length + 1);
        finishedArray[0] = element;
        System.arraycopy(elements, 0, finishedArray, 1, elements.length);

        return finishedArray;
    }

    public static <T> T[] removeAt(T[] array, int index)
    {
        T[] finishedArray = Arrays.copyOf(array, array.length - 1);
        System.arraycopy(array, index + 1, finishedArray, index, array.length - 1 - index);
        return finishedArray;
    }

    public static String[] getUniqueOnly(List<String[]> items) {
        List<String> uniques = new ArrayList<>();
        for (String[] arr : items) {
            for (String item : arr) {
                if(!uniques.contains(item)){
                    uniques.add(item);
                }
            }
        }
        return uniques.toArray(new String[0]);
    }

    public static RelativeLayout.LayoutParams getRelativeLayoutParamsTopStatusbar(Context context) {
        RelativeLayout.LayoutParams lParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lParams.setMargins(dpToPx(context, 15), getStatusBarHeight(context), dpToPx(context, 10), 0);
        return lParams;
    }

    public static LinearLayout.LayoutParams getLinearLayoutParamsTopStatusbar(Context context) {
        LinearLayout.LayoutParams lParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lParams.setMargins(dpToPx(context, 15), getStatusBarHeight(context) + dpToPx(context, 10), 0, 0);
        return lParams;
    }

    public static RelativeLayout.LayoutParams addRelativeLayoutParamsTopStatusbarSpacing(Context context, RelativeLayout.LayoutParams lParams) {
        lParams.topMargin = lParams.topMargin + getStatusBarHeight(context);
        return lParams;
    }

    public static FrameLayout.LayoutParams addFrameLayoutLayoutParamsTopStatusbarSpacing(Context context, FrameLayout.LayoutParams lParams) {
        lParams.topMargin = lParams.topMargin + getStatusBarHeight(context);
        return lParams;
    }

    public static LinearLayout.LayoutParams addLinearLayoutParamsTopStatusbarSpacing(Context context, LinearLayout.LayoutParams lParams) {
        lParams.topMargin = lParams.topMargin + getStatusBarHeight(context);
        return lParams;
    }

    public static int getStatusBarHeight(Context context) {
        // TODO: maybe find a better way of getting this data
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    @ColorInt
    public static int getColorAtGradientPosition(float x, float minX, float maxX, @ColorInt int... colors) {
        if (colors.length == 0) {
            return -1;
        }
        else if(colors.length == 1){
            return colors[0];
        }
        else {
            float step = (maxX - minX) / (colors.length - 1);
            for (int i = 1; i < colors.length; i++) {
                if(x <= minX + step * i) {
                    return getColorAtGradientPosition(x, minX + step * (i - 1), minX + step * i, false, colors[i-1], colors[i]);
                }
            }
            return -1;
        }
    }

    @ColorInt
    public static int getColorAtGradientPosition(float x, float minX, float maxX, boolean reduceAccuracy, @ColorInt int fromColor, @ColorInt int toColor) {
        float range = maxX - minX;
        float pos = (x - minX) / range;
        if(reduceAccuracy){
            pos = Math.round(pos * 10) / 10.0f;
            pos = Math.round(pos * 2) / 2.0f;
        }
        float invPos = 1 - pos;

        int fromAlpha = Color.alpha(fromColor);
        int fromRed = Color.red(fromColor);
        int fromGreen = Color.green(fromColor);
        int fromBlue = Color.blue(fromColor);

        int toAlpha = Color.alpha(toColor);
        int toRed = Color.red(toColor);
        int toGreen = Color.green(toColor);
        int toBlue = Color.blue(toColor);

        int resAlpha = Math.round(fromAlpha * invPos + toAlpha * pos);
        int resRed = Math.round(fromRed * invPos + toRed * pos);
        int resGreen = Math.round(fromGreen * invPos + toGreen * pos);
        int resBlue = Math.round(fromBlue * invPos + toBlue * pos);

        return Color.argb(resAlpha, resRed, resGreen, resBlue);
    }

    public static Pair<Long, Long> getTimeSpanFrom(int toDaysInPast, boolean getTimeSpanUntilNow) {
        Calendar cldr = new GregorianCalendar(TimeZone.getDefault());
        cldr.setTime(Calendar.getInstance().getTime());
        cldr.add(Calendar.DAY_OF_MONTH, -toDaysInPast);
        cldr.set(Calendar.HOUR_OF_DAY, 0);
        cldr.set(Calendar.MINUTE, 0);
        cldr.set(Calendar.SECOND, 0);
        cldr.set(Calendar.MILLISECOND, 0);
        long startTime = cldr.getTimeInMillis();
        if(getTimeSpanUntilNow) {
            cldr.add(Calendar.DAY_OF_MONTH, toDaysInPast);
        }
        cldr.set(Calendar.HOUR_OF_DAY, 23);
        cldr.set(Calendar.MINUTE, 59);
        cldr.set(Calendar.SECOND, 59);
        cldr.set(Calendar.MILLISECOND, 999);
        long endTime = cldr.getTimeInMillis();
        return new Pair<>(startTime, endTime);
    }

    /**
     * finds the required amount of goals approximately suiting within a specified difficulty constraint
     * @param context current context (used for acquiring preferences)
     * @param providedGoals goals to be selected from
     * @param difficultyConstraint average difficulty that should be reached by selection (should be a number from 1.0f to 3.0f)
     * @param difficultyVariance the amount the difficultyConstraint should randomly spread within (difficultyVariance of 0.3f means difficultyConstraint of 2.0f will be spreading between 1.7f and 2.3f)
     * @param accuracy should be a multiple of ten and is used to specify the accuracy of the difficulties to calculate with
     * @param variance should be a small number for getting a little bit of randomization into selections (the higher it is, the more the result spreads around the difficulty constraint)
     * @param count the amount of goals to choose
     * @return the chosen goals suiting the specified constraints
     */
    public static List<Goal> getSuitableGoals(Context context, List<Goal> providedGoals, float difficultyConstraint, float difficultyVariance, int accuracy, float variance, int count) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        List<Goal> fitGoals = new ArrayList<>();
        float randomizedDiffConstraint = Math.min(3.0f, Math.max(1.0f, difficultyConstraint + (difficultyVariance > 0.0f ? (float)(ThreadLocalRandom.current().nextDouble(-difficultyVariance, difficultyVariance)) : 0.0f)));
        float difficultyLimit = randomizedDiffConstraint * count;
        List<Goal> goals = new ArrayList<>();
        for (Goal goal : providedGoals) {
            goals.add(goal.clone());
        }
        Integer[] difficulties = new Integer[goals.size()];
        Integer[] goalVal = new Integer[goals.size()];

        for (int i = 0; i < goals.size(); i++) {
            difficulties[i] = (int)(goals.get(i).difficulty * accuracy);
            double valFunction = preferences.getFloat("goal_function_value_a", -1.0f) * Math.pow(i, 2) + preferences.getFloat("goal_function_value_b", -1.0f) * i + preferences.getFloat("goal_function_value_c", -1.0f);
            double val = (Math.max(0, valFunction * ((1.0f/(goals.size()/6.0f)) * Math.pow(2.0f - Math.abs((difficulties[i]/(float)accuracy) - randomizedDiffConstraint), 2)) + ThreadLocalRandom.current().nextDouble(-variance, variance)));
            goalVal[i] = (int)(val);
        }

        int[][] m = new int[goals.size() + 1][(int)(difficultyLimit * accuracy) + 1];
        int res = findBestSuiting(difficulties, goalVal, goals.size(), (int)(difficultyLimit * accuracy), m);
        int w = (int)(difficultyLimit * accuracy);

        List<Integer> indexesToRemove = new ArrayList<>();
        for (int i = goals.size(); i > 0 && res > 0; i--) {
            if (res != m[i-1][w]) {
                fitGoals.add(goals.get(i-1));
                indexesToRemove.add(i-1);
                res -= goalVal[i-1]; // value
                w -= difficulties[i-1]; // weight
            }
        }

        // remove all goals selected by the algorithm to not choose them in the fill ups
        Collections.reverse(indexesToRemove);
        for (int i = 0; i < indexesToRemove.size(); i++) {
            goals.remove(indexesToRemove.get(i) - i);
            removeAt(difficulties, indexesToRemove.get(i) - i);
            removeAt(goalVal, indexesToRemove.get(i) - i);
        }
        indexesToRemove.clear();

        // fill the amount of goals to the desired count TODO check whether there are even enough goals inside
        while (fitGoals.size() < count) {
            m = new int[goals.size() + 1][(int)(difficultyLimit * accuracy) + 1];
            res = findBestSuiting(difficulties, goalVal, goals.size(), (int)(difficultyLimit * accuracy), m);
            w = (int)(difficultyLimit * accuracy);

            for (int i = goals.size(); i > 0 && res > 0; i--) {
                if (res != m[i-1][w]) {
                    fitGoals.add(goals.get(i-1));
                    indexesToRemove.add(i-1);
                    res -= goalVal[i-1]; // value
                    w -= difficulties[i-1]; // weight
                }
            }

            Collections.reverse(indexesToRemove);
            for (int i = 0; i < indexesToRemove.size(); i++) {
                goals.remove(indexesToRemove.get(i) - i);
                removeAt(difficulties, indexesToRemove.get(i) - i);
                removeAt(goalVal, indexesToRemove.get(i) - i);
            }
            indexesToRemove.clear();
        }

        return fitGoals.stream().limit(count).collect(Collectors.toList());
    }

    public static List<Goal> getSuitableGoals(Context context, List<Goal> providedGoals, int goalsCount, float goalsDiff1Weight, float goalsDiff2Weight, float goalsDiff3Weight) {
        float a = (-goalsDiff1Weight + 2 * goalsDiff2Weight - goalsDiff3Weight) / -2;
        float b = goalsDiff2Weight - goalsDiff1Weight - 3 * a;
        float c = goalsDiff1Weight - a - b;

        List<Double> weightedGoals = new ArrayList<>();

        if(goalsCount > providedGoals.size()){
            throw new IllegalArgumentException("The goals count must be smaller or equal to the size of the list of proved goals");
        }

        double weightSum = 0;
        for (Goal goal : providedGoals) {
            // ignoring the fact that multiple goals could have the value 0, as they are ignored anyways
            double weight = Math.max(a * Math.pow(goal.difficulty, 2) + b * goal.difficulty + c, 0);
            weightSum += weight;
            weightedGoals.add(weightSum);
        }

        Random r = new Random();
        List<Goal> chosenGoals = new ArrayList<>();
        for (int i = 0; i < goalsCount; i++) {
            double chosenWeight = weightedGoals.get(weightedGoals.size() - 1) * r.nextDouble();
            int closestValueIndex = getClosestHigherNumber(weightedGoals, chosenWeight);
            chosenGoals.add(providedGoals.get(closestValueIndex));
            providedGoals.remove(closestValueIndex);
            weightedGoals.remove(closestValueIndex);
        }
        return chosenGoals;
    }

    public static int getClosestHigherNumber(List<Double> numbers, double target) {
        int low = 0;
        int high = numbers.size() - 1;

        while (low < high) {
            int mid = (low + high) / 2;
            double midVal = numbers.get(mid);
            if (midVal < target) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }

        if (low >= numbers.size()) {
            return -1;
        }

        return low;
    }

    public static double[] calculateQuadraticFunction(PointF weight1, PointF weight2, PointF weight3) {
        double[] points = new double[3];

        points[0] = (weight2.x * weight1.y - weight2.x * weight3.y - weight3.x * weight1.y + weight3.x * weight2.y) / (Math.pow(weight2.x, 2) * weight3.x - weight2.x * Math.pow(weight3.x, 2));
        points[1] = (-Math.pow(weight2.x, 2) * weight1.y + Math.pow(weight2.x, 2) * weight3.y + Math.pow(weight3.x, 2) * weight1.y - Math.pow(weight3.x, 2) * weight2.y) / (Math.pow(weight2.x, 2) * weight3.x - weight2.x * Math.pow(weight3.x, 2));
        points[2] = weight1.y;

        return points;
    }

    public static int findBestSuiting(Integer[] w, Integer[] v, int n, int W, int[][] m) {
        if (n <= 0 || W <= 0) {
            return 0;
        }

        for (int j = 0; j <= W; j++) {
            m[0][j] = 0;
        }

        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= W; j++) {
                if (w[i - 1] > j) {
                    m[i][j] = m[i - 1][j];
                } else {
                    m[i][j] = Math.max(m[i - 1][j], m[i - 1][j - w[i - 1]] + v[i - 1]);
                }
            }
        }
        return m[n][W];
    }

    public static boolean hasNoData(List<Double> data) {
        for (Double dataPoint : data) {
            if (dataPoint.doubleValue() != -1.0) {
                return false;
            }
        }
        return true;
    }

    public static Drawable[] getIconsDreamMood(Context context) {
        Drawable iconMood1 = context.getResources().getDrawable(R.drawable.ic_baseline_sentiment_very_dissatisfied_24, context.getTheme());
        Drawable iconMood2 = context.getResources().getDrawable(R.drawable.ic_baseline_sentiment_dissatisfied_24, context.getTheme());
        Drawable iconMood3 = context.getResources().getDrawable(R.drawable.ic_baseline_sentiment_neutral_24, context.getTheme());
        Drawable iconMood4 = context.getResources().getDrawable(R.drawable.ic_baseline_sentiment_satisfied_24, context.getTheme());
        Drawable iconMood5 = context.getResources().getDrawable(R.drawable.ic_baseline_sentiment_very_satisfied_24, context.getTheme());
        return new Drawable[] { iconMood1, iconMood2, iconMood3, iconMood4, iconMood5 };
    }

    public static Drawable[] getIconsDreamClarity(Context context) {
        Drawable iconClarity1 = context.getResources().getDrawable(R.drawable.ic_baseline_brightness_4_24, context.getTheme());
        Drawable iconClarity2 = context.getResources().getDrawable(R.drawable.ic_baseline_brightness_5_24, context.getTheme());
        Drawable iconClarity3 = context.getResources().getDrawable(R.drawable.ic_baseline_brightness_6_24, context.getTheme());
        Drawable iconClarity4 = context.getResources().getDrawable(R.drawable.ic_baseline_brightness_7_24, context.getTheme());
        return new Drawable[] { iconClarity1, iconClarity2, iconClarity3, iconClarity4 };
    }

    public static Drawable[] getIconsSleepQuality(Context context) {
        Drawable iconQuality1 = context.getResources().getDrawable(R.drawable.ic_baseline_star_border_24, context.getTheme());
        Drawable iconQuality2 = context.getResources().getDrawable(R.drawable.ic_baseline_star_half_24, context.getTheme());
        Drawable iconQuality3 = context.getResources().getDrawable(R.drawable.ic_baseline_star_24, context.getTheme());
        Drawable iconQuality4 = context.getResources().getDrawable(R.drawable.ic_baseline_stars_24, context.getTheme());
        return new Drawable[] { iconQuality1, iconQuality2, iconQuality3, iconQuality4 };
    }

    public static void runResourceStatsPrinter() {
        new Thread(() -> {
            while(true) {
                final Runtime runtime = Runtime.getRuntime();
                final long usedMemInMB=(runtime.totalMemory() - runtime.freeMemory()) / 1048576L;
                final long maxHeapSizeInMB=runtime.maxMemory() / 1048576L;
                final long availHeapSizeInMB = maxHeapSizeInMB - usedMemInMB;
                System.out.printf(Locale.ENGLISH, "Resource-Usage: [%d] [%d] [%d]%n", usedMemInMB, maxHeapSizeInMB, availHeapSizeInMB);
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static Bitmap drawableToBitmap (Drawable drawable, int size) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, size, size);
        drawable.draw(canvas);

        return bitmap;
    }

    public static Bitmap drawableToBitmap (Drawable drawable, int tint, int size) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, size, size);
        drawable.setTint(tint);
        drawable.draw(canvas);

        return bitmap;
    }

    public static float roundToDigits(float value, int digits) {
        return (float)(Math.round(value * Math.pow(10, digits)) / Math.pow(10, digits));
    }

    @ColorInt
    public static int manipulateAlpha(@ColorInt int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    /**
     * Generates alarm specific unique id
     * @param alarmId the id of the alarm
     * @param weekdayId the id of the weekday of the alarm (Repeat only once = -1, Sunday = 0, Monday = 1, ...)
     * @return the unique id for the alarm at the specific weekday
     */
    public static int getBroadcastReqCodeFromID(int alarmId, int weekdayId) {
        return (alarmId*10) + 50000 + (weekdayId+1);
    }

    public static int getBroadcastReqCodeSnoozeFromID(int alarmId) {
        return alarmId + 100000;
    }
}
