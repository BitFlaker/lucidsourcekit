package com.bitflaker.lucidsourcekit.database;

import androidx.room.TypeConverter;

public class Converters {
    @TypeConverter
    public static boolean[] booleanArrayFromString(String value) {
        if(value == null) {
            return null;
        }
        boolean[] array = new boolean[value.length()];
        for (int i = 0; i < value.length(); i++) {
            array[i] = value.charAt(i) == '1';
        }
        return array;
    }

    @TypeConverter
    public static String booleanArrayToString(boolean[] value) {
        if(value == null){
            return null;
        }
        StringBuilder stringVal = new StringBuilder();
        for (boolean b : value) {
            stringVal.append(b ? '1' : '0');
        }
        return stringVal.toString();
    }
}
