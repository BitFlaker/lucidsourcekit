package com.bitflaker.lucidsourcekit.database.dreamjournal.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntryHasTag
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.resulttables.TagCount

@Dao
interface JournalEntryHasTagDao {
    @Query("SELECT JournalEntryTag.description as tag, COUNT(JournalEntryHasTag.tagId) as count FROM JournalEntryHasTag LEFT JOIN JournalEntryTag ON JournalEntryHasTag.tagId = JournalEntryTag.tagId LEFT JOIN JournalEntry ON JournalEntryHasTag.entryId = JournalEntry.entryId WHERE JournalEntry.timeStamp BETWEEN :startTimestamp AND :endTimestamp GROUP BY JournalEntryHasTag.tagId ORDER BY count DESC LIMIT :limit")
    suspend fun getMostUsedTagsList(startTimestamp: Long, endTimestamp: Long, limit: Int): List<TagCount>

    @Insert
    suspend fun insertAll(journalEntryHasTags: List<JournalEntryHasTag>): List<Long>

    @Delete
    suspend fun delete(journalEntryHasTag: JournalEntryHasTag)

    @Query("DELETE FROM JournalEntryHasTag WHERE entryId = :entryId")
    suspend fun deleteAll(entryId: Int)
}
