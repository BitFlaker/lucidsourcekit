package com.bitflaker.lucidsourcekit.database.dreamjournal.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntryTag

@Dao
interface JournalEntryTagDao {
    @Query("SELECT * FROM JournalEntryTag")
    suspend fun getAll(): List<JournalEntryTag>

    @Query("SELECT description FROM JournalEntryTag")
    suspend fun getAllTagTexts(): List<String>

    @Query("SELECT COUNT(*) FROM JournalEntryTag")
    suspend fun getTotalTagCount(): Int

    @Query("SELECT * FROM JournalEntryTag WHERE description IN (:descriptions)")
    suspend fun getByDescription(descriptions: List<String>): List<JournalEntryTag>

    @Insert
    suspend fun insertAll(vararg tags: JournalEntryTag): List<Long>

    @Insert(onConflict = OnConflictStrategy.Companion.IGNORE)
    suspend fun insertAll(journalEntryTags: MutableList<JournalEntryTag>): List<Long>

    @Delete
    suspend fun delete(tag: JournalEntryTag)
}
