package com.bitflaker.lucidsourcekit.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.bitflaker.lucidsourcekit.database.entities.JournalEntryHasType;

import java.util.List;

@Dao
public interface JournalEntryIsTypeDao {
    @Query("SELECT * FROM JournalEntryHasType WHERE entryId = :entryId")
    List<JournalEntryHasType> getAllFromEntryId(int entryId);

    @Insert
    void insertAll(JournalEntryHasType... journalEntryHasTypes);

    @Delete
    void delete(JournalEntryHasType journalEntryHasType);
}
