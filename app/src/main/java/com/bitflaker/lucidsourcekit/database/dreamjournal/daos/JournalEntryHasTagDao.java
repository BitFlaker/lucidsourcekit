package com.bitflaker.lucidsourcekit.database.dreamjournal.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntryHasTag;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.resulttables.TagCount;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface JournalEntryHasTagDao {
    @Query("SELECT JournalEntryTag.description as tag, COUNT(JournalEntryHasTag.tagId) as count FROM JournalEntryHasTag LEFT JOIN JournalEntryTag ON JournalEntryHasTag.tagId = JournalEntryTag.tagId LEFT JOIN JournalEntry ON JournalEntryHasTag.entryId = JournalEntry.entryId WHERE JournalEntry.timeStamp BETWEEN :startTimestamp AND :endTimestamp GROUP BY JournalEntryHasTag.tagId ORDER BY count DESC LIMIT :limit")
    Single<List<TagCount>> getMostUsedTagsList(long startTimestamp, long endTimestamp, int limit);

    @Insert
    Single<List<Long>> insertAll(List<JournalEntryHasTag> journalEntryHasTags);

    @Delete
    Completable delete(JournalEntryHasTag journalEntryHasTag);

    @Query("DELETE FROM JournalEntryHasTag WHERE entryId = :entryId")
    Completable deleteAll(int entryId);
}
