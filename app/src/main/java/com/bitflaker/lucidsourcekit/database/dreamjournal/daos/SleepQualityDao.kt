package com.bitflaker.lucidsourcekit.database.dreamjournal.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.SleepQuality

@Dao
interface SleepQualityDao {
    @Query("SELECT * FROM SleepQuality")
    suspend fun getAll(): List<SleepQuality>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(sleepQualities: Array<SleepQuality>)

    @Delete
    suspend fun delete(sleepQuality: SleepQuality)
}
