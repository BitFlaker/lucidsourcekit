package com.bitflaker.lucidsourcekit.database.dreamjournal.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntry
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.results.AverageEntryValues
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.results.DreamJournalEntry

@Dao
interface JournalEntryDao {
    @Transaction
    @Query("SELECT * FROM JournalEntry ORDER BY timeStamp DESC")
    suspend fun getAll(): List<DreamJournalEntry>

    @Query("SELECT entryId FROM JournalEntry ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomEntryId(): Long?

    @Transaction
    @Query("SELECT * FROM JournalEntry ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomEntry(): List<DreamJournalEntry>

    @Transaction
    @Query("SELECT * FROM JournalEntry WHERE entryId = :id")
    suspend fun getEntryDataById(id: Int): DreamJournalEntry

    @Transaction
    @Query("SELECT * FROM JournalEntry WHERE timeStamp >= :startTimestamp AND timeStamp < :endTimestamp")
    suspend fun getEntriesInTimestampRange(startTimestamp: Long, endTimestamp: Long): MutableList<DreamJournalEntry>

    @Query("SELECT * FROM JournalEntry WHERE entryId = :id")
    suspend fun getEntryById(id: Int): JournalEntry

    @Query("SELECT COUNT(*) as dreamCount, AVG(CASE moodId WHEN 'TRB' then 0 WHEN 'POR' then 1 WHEN 'OKY' then 2 WHEN 'GRT' then 3 WHEN 'OSD' then 4 ELSE 0 END) as avgMoods, AVG(CASE clarityId WHEN 'VCL' then 0 WHEN 'CLD' then 1 WHEN 'CLR' then 2 WHEN 'CCL' then 3 ELSE 0 END) as avgClarities, AVG(CASE qualityId WHEN 'TRB' then 0 WHEN 'POR' then 1 WHEN 'GRT' then 2 WHEN 'OSD' then 3 ELSE 0 END) as avgQualities FROM JournalEntry WHERE timeStamp BETWEEN :startTimestamp AND :endTimestamp")
    suspend fun getAverageEntryInTimeSpan(startTimestamp: Long, endTimestamp: Long): AverageEntryValues

    @Query("SELECT COUNT(*) FROM JournalEntry LEFT JOIN JournalEntryHasType ON JournalEntry.entryId = JournalEntryHasType.entryId WHERE JournalEntryHasType.typeId = 'LCD' AND timeStamp BETWEEN :startTimestamp AND :endTimestamp")
    suspend fun getLucidEntriesCount(startTimestamp: Long, endTimestamp: Long): Int

    @Query("SELECT COUNT(*) FROM JournalEntry WHERE timeStamp BETWEEN :startTimestamp AND :endTimestamp")
    suspend fun getEntriesCount(startTimestamp: Long, endTimestamp: Long): Int

    @Query("SELECT timeStamp FROM JournalEntry WHERE timeStamp >= :timeFrom")
    suspend fun getEntriesFrom(timeFrom: Long): List<Long>

    @Query("SELECT COUNT(*) FROM JournalEntry")
    suspend fun getTotalEntriesCount(): Int

    @Query("SELECT COALESCE(MIN(timestamp), -1) FROM JournalEntry")
    suspend fun getOldestTime(): Long

    @Query("SELECT timeStamp FROM JournalEntry WHERE timeStamp >= :from AND timeStamp < :to ORDER BY timeStamp DESC")
    suspend fun getTimestampsBetween(from: Long, to: Long): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<JournalEntry>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: JournalEntry): Long

    @Delete
    suspend fun delete(journalEntry: JournalEntry)
}
