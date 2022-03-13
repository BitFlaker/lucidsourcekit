package com.bitflaker.lucidsourcekit.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.bitflaker.lucidsourcekit.database.entities.DreamClarity;

import java.util.List;

@Dao
public interface DreamClarityDao {
    @Query("SELECT * FROM DreamClarity")
    List<DreamClarity> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(DreamClarity... dreamClarities);

    @Delete
    void delete(DreamClarity dreamClarity);
}
