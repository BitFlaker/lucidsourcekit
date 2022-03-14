package com.bitflaker.lucidsourcekit.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.bitflaker.lucidsourcekit.database.entities.AudioLocation;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface AudioLocationDao {
    @Query("SELECT * FROM AudioLocation WHERE entryId = :entryId")
    Single<List<AudioLocation>> getAllFromEntryId(int entryId);

    //@Insert
    //Single<Long> insertAll(AudioLocation... audioLocations);

    @Insert
    Single<List<Long>> insertAll(List<AudioLocation> audioLocations);

    @Delete
    Completable delete(AudioLocation audioLocation);
}
