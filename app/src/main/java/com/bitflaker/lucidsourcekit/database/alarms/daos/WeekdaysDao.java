package com.bitflaker.lucidsourcekit.database.alarms.daos;

import static androidx.room.OnConflictStrategy.REPLACE;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.bitflaker.lucidsourcekit.database.alarms.entities.Weekdays;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface WeekdaysDao {
    @Query("SELECT * FROM Weekdays")
    Single<List<Weekdays>> getAll();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    Completable insertAll(Weekdays... weekdays);

    @Insert(onConflict = REPLACE)
    Single<Long> insert(Weekdays weekdays);

    @Delete
    Completable delete(Weekdays weekdays);

    @Query("DELETE FROM Weekdays")
    Completable deleteAll();
}
