package com.bitflaker.lucidsourcekit.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.bitflaker.lucidsourcekit.database.entities.JournalEntryTag;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface JournalEntryTagDao {
    @Query("SELECT * FROM JournalEntryTag")
    Single<List<JournalEntryTag>> getAll();

    @Insert
    Single<List<Long>> insertAll(JournalEntryTag... tags);

    @Insert
    Single<List<Long>> insertAll(List<JournalEntryTag> journalEntryTags);

    @Delete
    Completable delete(JournalEntryTag tag);
}
