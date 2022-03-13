package com.bitflaker.lucidsourcekit.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.bitflaker.lucidsourcekit.database.entities.JournalEntryHasTag;

import java.util.List;

@Dao
public interface JournalEntryHasTagDao {
    @Query("SELECT * FROM JournalEntryHasTag WHERE entryId = :entryId")
    List<JournalEntryHasTag> getAllFromEntryId(int entryId);

    @Insert
    void insertAll(JournalEntryHasTag... journalEntryHasTags);

    @Delete
    void delete(JournalEntryHasTag journalEntryHasTag);
}
