package com.bitflaker.lucidsourcekit.database.dreamjournal.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.AudioLocation

@Dao
interface AudioLocationDao {
    @Query("SELECT * FROM AudioLocation WHERE entryId = :entryId")
    suspend fun getAllFromEntryId(entryId: Int): List<AudioLocation>

    @Insert
    suspend fun insertAll(audioLocations: List<AudioLocation>): List<Long>

    @Delete
    suspend fun delete(audioLocation: AudioLocation)

    @Query("DELETE FROM AudioLocation WHERE entryId = :entryId")
    suspend fun deleteAll(entryId: Int)
}
