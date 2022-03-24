package com.bitflaker.lucidsourcekit.database.dreamjournal.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntryHasType;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface JournalEntryIsTypeDao {
    @Query("SELECT * FROM JournalEntryHasType WHERE entryId = :entryId")
    Single<List<JournalEntryHasType>> getAllFromEntryId(int entryId);

    @Insert
    Completable insertAll(JournalEntryHasType... journalEntryHasTypes);

    @Insert
    Completable insertAll(List<JournalEntryHasType> journalEntryHasTypes);

    @Delete
    Completable delete(JournalEntryHasType journalEntryHasType);

    @Query("DELETE FROM JournalEntryHasType WHERE entryId = :entryId")
    Completable deleteAll(int entryId);
}
