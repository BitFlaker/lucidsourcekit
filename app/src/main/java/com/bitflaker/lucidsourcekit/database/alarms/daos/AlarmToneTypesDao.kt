package com.bitflaker.lucidsourcekit.database.alarms.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bitflaker.lucidsourcekit.database.alarms.entities.AlarmToneTypes

@Dao
interface AlarmToneTypesDao {
    @Query("SELECT * FROM AlarmToneTypes")
    suspend fun getAll(): MutableList<AlarmToneTypes>

    @Insert(onConflict = OnConflictStrategy.Companion.IGNORE)
    suspend fun insertAll(alarmToneTypes: Array<AlarmToneTypes>)

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insert(alarmToneTypes: AlarmToneTypes): Long

    @Delete
    suspend fun delete(alarmToneTypes: AlarmToneTypes)

    @Query("DELETE FROM AlarmToneTypes")
    suspend fun deleteAll()
}