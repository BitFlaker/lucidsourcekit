package com.bitflaker.lucidsourcekit.database.daos;

import static androidx.room.OnConflictStrategy.REPLACE;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.bitflaker.lucidsourcekit.database.entities.JournalEntry;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface JournalEntryDao {
    @Query("SELECT * FROM JournalEntry")
    Flowable<List<JournalEntry>> getAll();

    @Query("SELECT * FROM JournalEntry WHERE entryId = :id")
    Single<JournalEntry> getEntryById(int id);

    @Insert(onConflict = REPLACE)
    Single<List<Long>> insertAll(List<JournalEntry> entries);

    @Insert(onConflict = REPLACE)
    Single<Long> insert(JournalEntry entry);

    @Delete
    Completable delete(JournalEntry journalEntry);
}
