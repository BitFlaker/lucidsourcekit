package com.bitflaker.lucidsourcekit.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.bitflaker.lucidsourcekit.database.entities.DreamType;

import java.util.List;

@Dao
public interface DreamTypeDao {
    @Query("SELECT * FROM DreamType")
    List<DreamType> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(DreamType... types);

    @Delete
    void delete(DreamType type);
}
