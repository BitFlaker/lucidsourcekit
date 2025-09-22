package com.bitflaker.lucidsourcekit.database

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun booleanArrayFromString(value: String?): BooleanArray? {
        return value?.map { v -> v == '1' }?.toBooleanArray()
    }

    @TypeConverter
    fun booleanArrayToString(value: BooleanArray?): String? {

        if (value == null) {
            return null
        }
        val stringVal = StringBuilder()
        for (b in value) {
            stringVal.append(if (b) '1' else '0')
        }
        return stringVal.toString()
    }
}
