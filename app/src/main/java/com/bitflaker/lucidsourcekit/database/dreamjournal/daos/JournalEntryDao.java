package com.bitflaker.lucidsourcekit.database.dreamjournal.daos;

import static androidx.room.OnConflictStrategy.REPLACE;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntry;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.resulttables.AverageEntryValues;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.resulttables.DreamJournalEntry;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface JournalEntryDao {
    @Transaction
    @Query("SELECT * FROM JournalEntry ORDER BY timeStamp DESC")
    Single<List<DreamJournalEntry>> getAll();

    @Transaction
    @Query("SELECT * FROM JournalEntry ORDER BY RANDOM() LIMIT 1")
    Single<List<DreamJournalEntry>> getRandomEntry();

    @Transaction
    @Query("SELECT * FROM JournalEntry WHERE entryId = :id")
    Single<DreamJournalEntry> getEntryDataById(int id);

    @Transaction
    @Query("SELECT * FROM JournalEntry WHERE timeStamp >= :startTimestamp AND timeStamp < :endTimestamp")
    Single<List<DreamJournalEntry>> getEntriesInTimestampRange(long startTimestamp, long endTimestamp);

    @Query("SELECT * FROM JournalEntry WHERE entryId = :id")
    Single<JournalEntry> getEntryById(int id);

    @Query("SELECT COUNT(*) as dreamCount, AVG(CASE moodId WHEN 'TRB' then 0 WHEN 'POR' then 1 WHEN 'OKY' then 2 WHEN 'GRT' then 3 WHEN 'OSD' then 4 ELSE 0 END) as avgMoods, AVG(CASE clarityId WHEN 'VCL' then 0 WHEN 'CLD' then 1 WHEN 'CLR' then 2 WHEN 'CCL' then 3 ELSE 0 END) as avgClarities, AVG(CASE qualityId WHEN 'TRB' then 0 WHEN 'POR' then 1 WHEN 'GRT' then 2 WHEN 'OSD' then 3 ELSE 0 END) as avgQualities FROM JournalEntry WHERE timeStamp BETWEEN :startTimestamp AND :endTimestamp")
    Single<AverageEntryValues> getAverageEntryInTimeSpan(long startTimestamp, long endTimestamp);

    @Query("SELECT COUNT(*) FROM JournalEntry LEFT JOIN JournalEntryHasType ON JournalEntry.entryId = JournalEntryHasType.entryId WHERE JournalEntryHasType.typeId = 'LCD' AND timeStamp BETWEEN :startTimestamp AND :endTimestamp")
    Single<Integer> getLucidEntriesCount(long startTimestamp, long endTimestamp);

    @Query("SELECT COUNT(*) FROM JournalEntry WHERE timeStamp BETWEEN :startTimestamp AND :endTimestamp")
    Single<Integer> getEntriesCount(long startTimestamp, long endTimestamp);

    @Query("SELECT timeStamp FROM JournalEntry WHERE timeStamp >= :timeFrom")
    Single<List<Long>> getEntriesFrom(long timeFrom);

    @Query("SELECT COUNT(*) FROM JournalEntry")
    Single<Integer> getTotalEntriesCount();

    @Query("SELECT COALESCE(MIN(timestamp), -1) FROM JournalEntry")
    Single<Long> getOldestTime();

    @Query("SELECT timeStamp FROM JournalEntry WHERE timeStamp >= :from AND timeStamp < :to ORDER BY timeStamp DESC")
    Single<List<Long>> getTimestampsBetween(long from, long to);

    @Insert(onConflict = REPLACE)
    Single<List<Long>> insertAll(List<JournalEntry> entries);

    @Insert(onConflict = REPLACE)
    Single<Long> insert(JournalEntry entry);

    @Delete
    Completable delete(JournalEntry journalEntry);
}
