package com.bitflaker.lucidsourcekit.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.bitflaker.lucidsourcekit.database.entities.AssignedTags;
import com.bitflaker.lucidsourcekit.database.entities.JournalEntryHasTag;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface JournalEntryHasTagDao {
    @Query("SELECT JournalEntryTag.description FROM JournalEntryHasTag LEFT JOIN JournalEntryTag ON JournalEntryTag.tagId = JournalEntryHasTag.tagId WHERE entryId = :entryId")
    Single<List<AssignedTags>> getAllFromEntryId(int entryId);

    @Insert
    Completable insertAll(JournalEntryHasTag... journalEntryHasTags);

    @Delete
    Completable delete(JournalEntryHasTag journalEntryHasTag);

    @Query("DELETE FROM JournalEntryHasTag WHERE entryId = :entryId")
    Completable deleteAll(int entryId);
}
