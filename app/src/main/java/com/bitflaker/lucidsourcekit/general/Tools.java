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
import android.view.ViewGroup;
import android.widget.RelativeLayout;

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
}
