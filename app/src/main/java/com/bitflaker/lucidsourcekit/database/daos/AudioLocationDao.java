package com.bitflaker.lucidsourcekit.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.bitflaker.lucidsourcekit.database.entities.AudioLocation;

import java.util.List;

@Dao
public interface AudioLocationDao {
    @Query("SELECT * FROM AudioLocation")
    List<AudioLocation> getAll();

    @Insert
    void insertAll(AudioLocation... audioLocations);

    @Delete
    void delete(AudioLocation audioLocation);
}
