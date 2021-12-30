package com.bitflaker.lucidsourcekit.general;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import com.bitflaker.lucidsourcekit.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class Tools {
    private static int THEME_DIALOG;
    private static int THEME_POPUP;

    public static void setThemeColors(int theme){
        switch(theme){
            case R.style.Theme_LucidSourceKit_Light:
                THEME_DIALOG = R.style.ThemedDialog_Light;
                THEME_POPUP = R.style.PopupMenu_Light;
                break;
            case R.style.Theme_LucidSourceKit_LCDark:
                THEME_DIALOG = R.style.ThemedDialog_LCDark;
                THEME_POPUP = R.style.PopupMenu_LCDark;
                break;
            case R.style.Theme_LucidSourceKit_Amoled_Dark:
                THEME_DIALOG = R.style.ThemedDialog_Amoled_Dark;
                THEME_POPUP = R.style.PopupMenu_LCDark;
                break;
            case R.style.Theme_LucidSourceKit_Dark:
                THEME_DIALOG = R.style.ThemedDialog_Dark;
                THEME_POPUP = R.style.PopupMenu_Dark;
                break;
        }
        THEME_DIALOG = R.style.ThemedDialog_Dark;
        THEME_POPUP = R.style.PopupMenu_Dark;
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

    public static int dpToPx(Context context, double dp) {
        return (int)(dp * context.getResources().getDisplayMetrics().density);
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
}
