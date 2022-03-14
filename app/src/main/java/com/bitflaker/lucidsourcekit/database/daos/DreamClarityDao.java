package com.bitflaker.lucidsourcekit.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.bitflaker.lucidsourcekit.database.entities.DreamClarity;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface DreamClarityDao {
    @Query("SELECT * FROM DreamClarity")
    Single<List<DreamClarity>> getAll();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    Completable insertAll(DreamClarity... dreamClarities);

    @Delete
    Completable delete(DreamClarity dreamClarity);
}
