package com.bitflaker.lucidsourcekit.utils;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.util.Pair;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.res.ResourcesCompat;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.DreamClarity;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.DreamMood;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.SleepQuality;
import com.bitflaker.lucidsourcekit.database.goals.entities.Goal;
import com.bitflaker.lucidsourcekit.data.datastore.DataStoreKeys;
import com.bitflaker.lucidsourcekit.data.datastore.DataStoreManager;
import com.bitflaker.lucidsourcekit.main.goals.RandomGoalPicker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;

import io.reactivex.rxjava3.core.Maybe;

public class Tools {
    private static final int NOTIFICATION_ID_START = 500000;
//    private static int THEME_DIALOG;
//    private static int THEME_POPUP;
//    private static int THEME;
    private static final HashMap<String, Integer> notificationIdMap = new HashMap<>() {{
        put("DJR", NOTIFICATION_ID_START + 11);
        put("RCR", NOTIFICATION_ID_START + 12);
        put("DGR", NOTIFICATION_ID_START + 13);
        put("CR", NOTIFICATION_ID_START + 14);
    }};
    private static HashMap<String, Drawable> iconsDreamMood = null;
    private static HashMap<String, Drawable> iconsDreamClarity = null;
    private static HashMap<String, Drawable> iconsSleepQuality = null;

//    public static void setThemeColors(int theme){
//        THEME_DIALOG = R.style.ThemedDialog_Dark;
//        THEME_POPUP = R.style.PopupMenu_Dark;
//        if(theme == R.style.Theme_LucidSourceKit_Light) {
//            THEME_DIALOG = R.style.ThemedDialog_Light;
//            THEME_POPUP = R.style.PopupMenu_Light;
//        }
//        else if(theme == R.style.Theme_LucidSourceKit_LCDark) { // Todo should be removed
//            THEME_DIALOG = R.style.ThemedDialog_LCDark;
//            THEME_POPUP = R.style.PopupMenu_LCDark;
//        }
//        else if(theme == R.style.Theme_LucidSourceKit_Amoled_Dark) {
//            THEME_DIALOG = R.style.ThemedDialog_Amoled_Dark;
//            THEME_POPUP = R.style.PopupMenu_LCDark;
//        }
//        else if(theme == R.style.Theme_LucidSourceKit_Dark) {
//            THEME_DIALOG = R.style.ThemedDialog_Dark;
//            THEME_POPUP = R.style.PopupMenu_Dark;
//        }
//        THEME = theme;
//    }

//    public static int getThemeDialog(){
//        return THEME_DIALOG;
//    }

//    public static int getPopupTheme() {
//        return THEME_POPUP;
//    }

//    public static int getTheme() {
//        return THEME;
//    }

    public static void loadLanguage(Activity activity){
        String lang = DataStoreManager.getInstance().getSetting(DataStoreKeys.LANGUAGE).blockingFirst();
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

    public static int dpToPx(Context context, double dp) {
        return (int)(dp * context.getResources().getDisplayMetrics().density);
    }

    public static int spToPx(Context context, float sp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.getResources().getDisplayMetrics());
    }

    public static int pxToDp(Context context, double px) {
        return (int)(px / context.getResources().getDisplayMetrics().density);
    }

    public static RelativeLayout.LayoutParams getRelativeLayoutParamsTopStatusbar(Context context) {
        RelativeLayout.LayoutParams lParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lParams.setMargins(dpToPx(context, 15), getStatusBarHeight(context), dpToPx(context, 10), 0);
        return lParams;
    }

    public static ConstraintLayout.LayoutParams getConstraintLayoutParamsTopStatusbar(ConstraintLayout.LayoutParams layoutParams, Context context) {
        layoutParams.setMargins(dpToPx(context, 15), getStatusBarHeight(context), 0, 0);
        return layoutParams;
    }

    public static RelativeLayout.LayoutParams addRelativeLayoutParamsTopStatusbarSpacing(Context context, RelativeLayout.LayoutParams lParams) {
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

    public static List<Goal> getNewShuffleGoals(MainDatabase db) {
        DataStoreManager dsManager = DataStoreManager.getInstance();
        float weightCommon = dsManager.getSetting(DataStoreKeys.GOAL_DIFFICULTY_VALUE_COMMON).blockingFirst();
        float weightUncommon = dsManager.getSetting(DataStoreKeys.GOAL_DIFFICULTY_VALUE_UNCOMMON).blockingFirst();
        float weightRare = dsManager.getSetting(DataStoreKeys.GOAL_DIFFICULTY_VALUE_RARE).blockingFirst();
        int goalCount = dsManager.getSetting(DataStoreKeys.GOAL_DIFFICULTY_COUNT).blockingFirst();
        float[] weights = new float[] { weightCommon, weightUncommon, weightRare };

        Maybe<List<Goal>> goals = db.getGoalDao().getAllMaybe();
        if(goals.isEmpty().blockingGet()) {
            return new ArrayList<>();
        }

        RandomGoalPicker randomGoalPicker = new RandomGoalPicker();
        for (Goal goal : goals.blockingGet()) {
            int occurrenceLevel = (int) goal.difficulty;
            float higherPercentage = goal.difficulty - occurrenceLevel;
            float lowerPercentage = 1 - higherPercentage;

            float weight = weights[weights.length - 1];
            if(occurrenceLevel < weights.length) {
                float lowerWeight = weights[occurrenceLevel - 1];
                float higherWeight = weights[occurrenceLevel];
                weight = lowerPercentage * lowerWeight + higherPercentage * higherWeight;
            }

            randomGoalPicker.add(weight, goal);
        }

        List<Goal> shuffledGoals = new ArrayList<>();
        for (int i = 0; i < goalCount; i++) {
            shuffledGoals.add(randomGoalPicker.getRandomGoal());
        }

        return shuffledGoals;
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
        initIconsDreamMood(context);
        return Arrays.stream(DreamMood.defaultData)
                .filter(x -> x != DreamMood.DEFAULT)
                .sorted(Comparator.comparing(x -> x.value))
                .map(x -> iconsDreamMood.getOrDefault(x.moodId, null))
                .toArray(Drawable[]::new);
    }

    public static Drawable[] getIconsDreamClarity(Context context) {
        initDreamClarities(context);
        return Arrays.stream(DreamClarity.defaultData)
                .filter(x -> x != DreamClarity.DEFAULT)
                .sorted(Comparator.comparing(x -> x.value))
                .map(x -> iconsDreamClarity.getOrDefault(x.clarityId, null))
                .toArray(Drawable[]::new);
    }

    public static Drawable[] getIconsSleepQuality(Context context) {
        initSleepQualities(context);
        return Arrays.stream(SleepQuality.defaultData)
                .filter(x -> x != SleepQuality.DEFAULT)
                .sorted(Comparator.comparing(x -> x.value))
                .map(x -> iconsSleepQuality.getOrDefault(x.qualityId, null))
                .toArray(Drawable[]::new);
    }

    public static Drawable resolveIconDreamMood(Context context, String dreamMoodId) {
        initIconsDreamMood(context);
        return iconsDreamMood.getOrDefault(dreamMoodId, null);
    }

    public static Drawable resolveIconDreamClarity(Context context, String dreamClarityId) {
        initDreamClarities(context);
        return iconsDreamClarity.getOrDefault(dreamClarityId, null);
    }

    public static Drawable resolveIconSleepQuality(Context context, String sleepQualityId) {
        initSleepQualities(context);
        return iconsSleepQuality.getOrDefault(sleepQualityId, null);
    }

    private static void initIconsDreamMood(Context context) {
        if (iconsDreamMood == null) {
            iconsDreamMood = new HashMap<>() {{
                put("TRB", resolveDrawable(context, R.drawable.ic_baseline_sentiment_very_dissatisfied_24));
                put("POR", resolveDrawable(context, R.drawable.ic_baseline_sentiment_dissatisfied_24));
                put("OKY", resolveDrawable(context, R.drawable.ic_baseline_sentiment_neutral_24));
                put("GRT", resolveDrawable(context, R.drawable.ic_baseline_sentiment_satisfied_24));
                put("OSD", resolveDrawable(context, R.drawable.ic_baseline_sentiment_very_satisfied_24));
            }};
        }
    }

    private static void initDreamClarities(Context context) {
        if (iconsDreamClarity == null) {
            iconsDreamClarity = new HashMap<>() {{
                put("VCL", resolveDrawable(context, R.drawable.ic_baseline_brightness_4_24));
                put("CLD", resolveDrawable(context, R.drawable.ic_baseline_brightness_5_24));
                put("CLR", resolveDrawable(context, R.drawable.ic_baseline_brightness_6_24));
                put("CCL", resolveDrawable(context, R.drawable.ic_baseline_brightness_7_24));
            }};
        }
    }

    private static void initSleepQualities(Context context) {
        if (iconsSleepQuality == null) {
            iconsSleepQuality = new HashMap<>() {{
                put("TRB", resolveDrawable(context, R.drawable.ic_baseline_star_border_24));
                put("POR", resolveDrawable(context, R.drawable.ic_baseline_star_half_24));
                put("GRT", resolveDrawable(context, R.drawable.ic_baseline_star_24));
                put("OSD", resolveDrawable(context, R.drawable.ic_baseline_stars_24));
            }};
        }
    }

    public static Drawable resolveDrawable(Context context, @DrawableRes int drawable) {
        return ResourcesCompat.getDrawable(context.getResources(), drawable, context.getTheme());
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

    public static long getTimeOfDayMillis(Calendar calendar) {
        long timeOfDayMillis = calendar.get(Calendar.HOUR_OF_DAY) * 60 * 60 * 1000;
        timeOfDayMillis += calendar.get(Calendar.MINUTE) * 60 * 1000;
        timeOfDayMillis += calendar.get(Calendar.SECOND) * 1000;
        timeOfDayMillis += calendar.get(Calendar.MILLISECOND);
        return timeOfDayMillis;
    }

    public static long getTimeFromMidnight(long timeInMillis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(getMidnightTime() + timeInMillis);
        return cal.getTimeInMillis();
    }

    public static long getMidnightTime(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    public static long getMidnightTime() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
        for (Map.Entry<T, E> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static int getUniqueNotificationId(String notificationCategoryId) {
        Integer val = notificationIdMap.get(notificationCategoryId);
        if(val != null) {
            return val;
        }
        return -1;
    }

    public static boolean deleteFile(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if(files != null) {
                for (File child : files) {
                    deleteFile(child);
                }
            }
        }
        return file.delete();
    }

    public static void copyFile(File src, File dest) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(src)) {
            try (FileOutputStream outputStream = new FileOutputStream(dest)) {
                byte[] buffer = new byte[2048];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
        }
    }

    public static void copyDir(File src, File dest) throws IOException {
        dest.mkdirs();
        File[] content = src.listFiles();
        if(content != null) {
            for (File file : content) {
                if(file.isFile()) {
                    copyFile(file, new File(dest, file.getName()));
                }
                else {
                    copyDir(file, new File(dest, file.getName()));
                }
            }
        }
    }

    public static Drawable resizeDrawable(Resources resources, Drawable drawable, int width, int height) {
        Bitmap bitmap;
        if(drawable.getClass().equals(VectorDrawable.class)) {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
        }
        else {
            bitmap = ((BitmapDrawable) drawable).getBitmap();
        }
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);
        return new BitmapDrawable(resources, scaledBitmap);
    }

    public static boolean isTimeInPast(long timestampDayEnd) {
        Calendar calendar = Calendar.getInstance();
        long current = calendar.getTimeInMillis();
        return timestampDayEnd <= current;
    }

    public static void animateBackgroundTint(View view, @ColorInt int colorFrom, @ColorInt int colorTo, int duration) {
        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
        colorAnimation.setDuration(duration);
        colorAnimation.addUpdateListener(animator -> view.setBackgroundTintList(ColorStateList.valueOf((int) animator.getAnimatedValue())));
        colorAnimation.start();
    }

    public static void animateImageTint(ImageView view, @ColorInt int colorFrom, @ColorInt int colorTo, int duration) {
        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
        colorAnimation.setDuration(duration);
        colorAnimation.addUpdateListener(animator -> view.setImageTintList(ColorStateList.valueOf((int) animator.getAnimatedValue())));
        colorAnimation.start();
    }

    public static Drawable cloneDrawable(Drawable drawable) {
        Drawable icon = drawable.getConstantState().newDrawable();
        icon.setBounds(drawable.copyBounds());
        return icon;
    }

    public static Calendar calendarFromMillis(long milliseconds) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(milliseconds);
        return cal;
    }
}
