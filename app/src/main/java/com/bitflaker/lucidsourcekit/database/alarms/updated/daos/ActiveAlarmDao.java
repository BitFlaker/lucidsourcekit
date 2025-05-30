package com.bitflaker.lucidsourcekit.database.alarms.updated.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.bitflaker.lucidsourcekit.database.alarms.updated.entities.ActiveAlarm;
import com.bitflaker.lucidsourcekit.database.alarms.updated.entities.ActiveAlarmDetails;
import com.bitflaker.lucidsourcekit.database.alarms.updated.entities.StoredAlarm;
import com.bitflaker.lucidsourcekit.database.alarms.updated.entities.resulttables.AlarmTimestamps;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface ActiveAlarmDao {
    @Query("SELECT * FROM activealarm")
    Single<List<ActiveAlarm>> getAll();

    // TODO: Fix ActiveAlarm entries existing without StoredAlarms
    @Query("SELECT ActiveAlarm.*, StoredAlarm.pattern, StoredAlarm.alarmId AS storedAlarmId FROM StoredAlarm LEFT JOIN ActiveAlarm ON ActiveAlarm.requestCode = StoredAlarm.requestCodeActiveAlarm WHERE ActiveAlarm.requestCode != -1")
    Single<List<ActiveAlarmDetails>> getAllDetails();

    @Query("SELECT * FROM (SELECT t1.requestCode+1 AS Id FROM ActiveAlarm t1 WHERE NOT EXISTS(SELECT * FROM ActiveAlarm t2 WHERE t2.requestCode = t1.requestCode + 1 ) UNION SELECT 1 AS Id WHERE NOT EXISTS (SELECT * FROM ActiveAlarm t3 WHERE t3.requestCode = 1)) ot ORDER BY 1 LIMIT 1")
    Single<Integer> getFirstFreeRequestCode();

    @Query("SELECT initialTime as alarmTimestamp, StoredAlarm.bedtimeTimestamp FROM ActiveAlarm LEFT JOIN StoredAlarm ON StoredAlarm.requestCodeActiveAlarm = requestCode WHERE requestCode != -1 ORDER BY initialTime ASC LIMIT 1")
    Single<List<AlarmTimestamps>> getNextUpcomingAlarmTimestamp();

    @Update
    Completable update(ActiveAlarm alarm);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insert(ActiveAlarm alarm);

    @Delete
    Completable delete(ActiveAlarm alarm);

    @Query("DELETE FROM ActiveAlarm")
    Completable deleteAll();

    @Query("DELETE FROM ActiveAlarm WHERE requestCode != -1")
    Completable deleteAllButUnreferenced();

    @Query("SELECT * FROM ActiveAlarm WHERE requestCode = :requestCode")
    Single<ActiveAlarm> getById(int requestCode);

    @Query("DELETE FROM ActiveAlarm WHERE requestCode = :requestCodeActiveAlarm")
    Completable deleteById(int requestCodeActiveAlarm);

    @Query("SELECT StoredAlarm.* FROM ActiveAlarm LEFT JOIN StoredAlarm ON ActiveAlarm.requestCode = StoredAlarm.requestCodeActiveAlarm WHERE initialTime = :alarmTime")
    Single<List<StoredAlarm>> getStoredAlarmByAlarmTime(long alarmTime);

    @Query("DELETE FROM ActiveAlarm WHERE requestCode != -1")
    Completable clear();
}
