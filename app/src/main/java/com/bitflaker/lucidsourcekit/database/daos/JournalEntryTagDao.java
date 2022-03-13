package com.bitflaker.lucidsourcekit.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.bitflaker.lucidsourcekit.database.entities.JournalEntryTag;

import java.util.List;

@Dao
public interface JournalEntryTagDao {
    @Query("SELECT * FROM JournalEntryTag")
    List<JournalEntryTag> getAll();

    @Insert
    void insertAll(JournalEntryTag... tags);

    @Delete
    void delete(JournalEntryTag tag);
}
