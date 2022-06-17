package com.bitflaker.lucidsourcekit.general;

import android.graphics.Typeface;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FontHandler {
    private static FontHandler instance;
    private final Map<String, Typeface> sSystemFontMap;

    private FontHandler() {
        sSystemFontMap = getsSystemFontMap();
    }

    public static FontHandler getInstance(){
        if(instance == null){
            instance = new FontHandler();
        }
        return instance;
    }

    private Map<String, Typeface> getsSystemFontMap() {
        Map<String, Typeface> sSystemFontMap = null;
        try {
            Typeface typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL);
            Field field = Typeface.class.getDeclaredField("sSystemFontMap");
            field.setAccessible(true);
            sSystemFontMap = (Map<String, Typeface>) field.get(typeface);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sSystemFontMap;
    }

    public List<String> getNameByFont(Typeface typeface) {
        Set<Map.Entry<String, Typeface>> set = sSystemFontMap.entrySet();
        List<String> arr = new ArrayList<>();
        for (Object obj : set) {
            Map.Entry entry = (Map.Entry) obj;
            if (entry.getValue().equals(typeface)) {
                String str = (String) entry.getKey();
                arr.add(str);
            }
        }
        return arr;
    }

    public Typeface getFontByName(String fontName) {
        Set<Map.Entry<String, Typeface>> set = sSystemFontMap.entrySet();
        for (Map.Entry<String, Typeface> entry : set) {
            if(entry.getKey().equalsIgnoreCase(fontName)){
                return entry.getValue();
            }
        }
        return null;
    }
}
