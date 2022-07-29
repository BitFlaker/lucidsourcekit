package com.bitflaker.lucidsourcekit.database.alarms.daos;

import static androidx.room.OnConflictStrategy.REPLACE;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.bitflaker.lucidsourcekit.database.alarms.entities.Alarm;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface AlarmDao {
    @Query("SELECT * FROM Alarm")
    Single<List<Alarm>> getAll();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    Completable insertAll(Alarm... alarm);

    @Insert(onConflict = REPLACE)
    Single<Long> insert(Alarm alarm);

    @Query("DELETE FROM Alarm WHERE Alarm.alarmId IN (:alarmsToDelete)")
    Completable deleteAllById(List<Integer> alarmsToDelete);

    @Delete
    Completable delete(Alarm alarm);

    @Query("DELETE FROM Alarm")
    Completable deleteAll();

    @Query("UPDATE Alarm SET isActive = :checked WHERE alarmId = :alarmId")
    Completable setActiveState(int alarmId, boolean checked);
}
