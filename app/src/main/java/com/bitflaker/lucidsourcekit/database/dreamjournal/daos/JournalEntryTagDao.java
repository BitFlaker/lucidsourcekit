package com.bitflaker.lucidsourcekit.database.dreamjournal.daos;

import static androidx.room.OnConflictStrategy.IGNORE;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntryTag;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface JournalEntryTagDao {
    @Query("SELECT * FROM JournalEntryTag")
    Single<List<JournalEntryTag>> getAll();

    @Query("SELECT description FROM JournalEntryTag")
    Single<List<String>> getAllTagTexts();

    @Query("SELECT COUNT(*) FROM JournalEntryTag")
    Single<Integer> getTotalTagCount();

    @Query("SELECT * FROM JournalEntryTag WHERE description IN (:descriptions)")
    Single<List<JournalEntryTag>> getByDescription(List<String> descriptions);

    @Insert
    Single<List<Long>> insertAll(JournalEntryTag... tags);

    @Insert(onConflict = IGNORE)
    Single<List<Long>> insertAll(List<JournalEntryTag> journalEntryTags);

    @Delete
    Completable delete(JournalEntryTag tag);
}
