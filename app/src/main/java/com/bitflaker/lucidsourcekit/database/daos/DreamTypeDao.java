package com.bitflaker.lucidsourcekit.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.bitflaker.lucidsourcekit.database.entities.DreamType;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface DreamTypeDao {
    @Query("SELECT * FROM DreamType")
    Single<List<DreamType>> getAll();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    Completable insertAll(DreamType... types);

    @Delete
    Completable delete(DreamType type);
}
