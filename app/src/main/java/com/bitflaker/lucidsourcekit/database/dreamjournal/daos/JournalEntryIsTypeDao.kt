package com.bitflaker.lucidsourcekit.database.dreamjournal.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntryHasType

@Dao
interface JournalEntryIsTypeDao {
    @Query("SELECT * FROM JournalEntryHasType WHERE entryId = :entryId")
    suspend fun getAllFromEntryId(entryId: Int): List<JournalEntryHasType>

    @Insert
    suspend fun insertAll(vararg journalEntryHasTypes: JournalEntryHasType)

    @Insert
    suspend fun insertAll(journalEntryHasTypes: List<JournalEntryHasType>)

    @Delete
    suspend fun delete(journalEntryHasType: JournalEntryHasType)

    @Query("DELETE FROM JournalEntryHasType WHERE entryId = :entryId")
    suspend fun deleteAll(entryId: Int)
}
