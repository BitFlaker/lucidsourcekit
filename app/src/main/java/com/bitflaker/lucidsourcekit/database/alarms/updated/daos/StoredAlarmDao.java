package com.bitflaker.lucidsourcekit.database.alarms.updated.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.bitflaker.lucidsourcekit.database.alarms.updated.entities.StoredAlarm;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface StoredAlarmDao {
    @Query("SELECT * FROM StoredAlarm")
    Single<List<StoredAlarm>> getAll();

    @Query("SELECT * FROM StoredAlarm WHERE alarmId IN (:alarmIds)")
    Single<List<StoredAlarm>> getAllById(List<Integer> alarmIds);

    @Query("SELECT * FROM StoredAlarm WHERE alarmId = :alarmId")
    Single<StoredAlarm> getById(int alarmId);

    @Query("UPDATE StoredAlarm SET requestCodeActiveAlarm = :requestCode WHERE alarmId = :storedAlarmId")
    Completable updateRequestCode(long storedAlarmId, int requestCode);

    @Update
    Completable update(StoredAlarm alarm);

    @Insert
    Single<Long> insert(StoredAlarm alarm);

    @Delete
    Completable delete(StoredAlarm alarm);

    @Query("DELETE FROM StoredAlarm")
    Completable deleteAll();

    @Query("UPDATE StoredAlarm SET isAlarmActive = :isAlarmActive WHERE alarmId = :alarmId")
    Completable setActiveState(int alarmId, boolean isAlarmActive);

    @Query("DELETE FROM StoredAlarm WHERE alarmId IN (:alarmsToDelete)")
    Completable deleteAllById(List<Integer> alarmsToDelete);
}
