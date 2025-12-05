package com.bitflaker.lucidsourcekit.database.dreamjournal.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.DreamMood

@Dao
interface DreamMoodDao {
    @Query("SELECT * FROM DreamMood")
    suspend fun getAll(): MutableList<DreamMood>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(dreamMoods: Array<DreamMood>)

    @Delete
    suspend fun delete(dreamMood: DreamMood)
}
