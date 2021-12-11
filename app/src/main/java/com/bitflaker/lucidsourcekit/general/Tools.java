package com.bitflaker.lucidsourcekit.general;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.util.TypedValue;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import java.util.Locale;

public class Tools {

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

    public static ColorStateList getAttrColor(int colorAttr, Resources.Theme theme) {
        TypedValue typedValue = getAttrValue(colorAttr, theme);
        @ColorInt int color = typedValue.data;
        return ColorStateList.valueOf(color);
    }

    @NonNull
    private static TypedValue getAttrValue(int attr, Resources.Theme theme) {
        TypedValue typedValue = new TypedValue();
        theme.resolveAttribute(attr, typedValue, true);
        return typedValue;
    }
}
