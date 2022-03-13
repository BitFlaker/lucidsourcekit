package com.bitflaker.lucidsourcekit.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.bitflaker.lucidsourcekit.database.entities.JournalEntry;

import java.util.List;

@Dao
public interface JournalEntryDao {
    @Query("SELECT * FROM JournalEntry")
    List<JournalEntry> getAll();

    @Insert
    void insertAll(JournalEntry... entries);

    @Delete
    void delete(JournalEntry journalEntry);
}
