package com.bitflaker.lucidsourcekit.database.alarms.daos;

import static androidx.room.OnConflictStrategy.REPLACE;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.bitflaker.lucidsourcekit.database.alarms.entities.AlarmIsOnWeekday;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface AlarmIsOnWeekdayDao {
    @Query("SELECT * FROM AlarmIsOnWeekday")
    Single<List<AlarmIsOnWeekday>> getAll();

    @Query("SELECT * FROM AlarmIsOnWeekday WHERE alarmId = :alarmId")
    Single<List<AlarmIsOnWeekday>> getAllForAlarm(int alarmId);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    Completable insertAll(AlarmIsOnWeekday... alarmIsOnWeekday);

    @Insert(onConflict = REPLACE)
    Completable insert(AlarmIsOnWeekday alarmIsOnWeekday);

    @Delete
    Completable delete(AlarmIsOnWeekday alarmIsOnWeekday);

    @Query("DELETE FROM AlarmIsOnWeekday")
    Completable deleteAll();
}
