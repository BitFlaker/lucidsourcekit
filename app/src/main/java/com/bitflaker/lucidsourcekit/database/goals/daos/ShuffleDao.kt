package com.bitflaker.lucidsourcekit.database.goals.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bitflaker.lucidsourcekit.database.goals.entities.Shuffle

@Dao
interface ShuffleDao {
    @Query("SELECT * FROM Shuffle")
    suspend fun getAll(): List<Shuffle>

    @Query("SELECT * FROM Shuffle WHERE dayStartTimestamp = :dayStartTimestamp and dayEndTimestamp = :dayEndTimestamp ORDER BY shuffleId DESC LIMIT 1")
    suspend fun getLastShuffleInDay(dayStartTimestamp: Long, dayEndTimestamp: Long): Shuffle?

    @Insert(onConflict = OnConflictStrategy.Companion.IGNORE)
    suspend fun insertAll(vararg shuffles: Shuffle)

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insert(shuffles: Shuffle): Long

    @Delete
    suspend fun delete(shuffle: Shuffle)

    @Query("DELETE FROM Shuffle")
    suspend fun deleteAll()
}
