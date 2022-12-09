package com.bitflaker.lucidsourcekit.database.alarms.updated.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.bitflaker.lucidsourcekit.database.alarms.updated.entities.ActiveAlarm;
import com.bitflaker.lucidsourcekit.database.alarms.updated.entities.ActiveAlarmDetails;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface ActiveAlarmDao {
    @Query("SELECT * FROM activealarm")
    Single<List<ActiveAlarm>> getAll();

    @Query("SELECT ActiveAlarm.*, StoredAlarm.pattern, StoredAlarm.alarmId AS storedAlarmId FROM ActiveAlarm LEFT JOIN StoredAlarm ON ActiveAlarm.requestCode = StoredAlarm.requestCodeActiveAlarm WHERE ActiveAlarm.requestCode != -1")
    Single<List<ActiveAlarmDetails>> getAllDetails();

    @Query("SELECT * FROM (SELECT t1.requestCode+1 AS Id FROM ActiveAlarm t1 WHERE NOT EXISTS(SELECT * FROM ActiveAlarm t2 WHERE t2.requestCode = t1.requestCode + 1 ) UNION SELECT 1 AS Id WHERE NOT EXISTS (SELECT * FROM ActiveAlarm t3 WHERE t3.requestCode = 1)) ot ORDER BY 1 LIMIT 1")
    Single<Integer> getFirstFreeRequestCode();

    @Query("SELECT IFNULL((SELECT initialTime FROM ActiveAlarm WHERE requestCode != -1 ORDER BY initialTime ASC LIMIT 1), -1)")
    Single<Long> getNextUpcomingAlarmTimestamp();

    @Update
    Completable update(ActiveAlarm alarm);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    Completable insert(ActiveAlarm alarm);

    @Delete
    Completable delete(ActiveAlarm alarm);

    @Query("DELETE FROM ActiveAlarm")
    Completable deleteAll();

    @Query("DELETE FROM ActiveAlarm WHERE requestCode != -1")
    Completable deleteAllButUnreferenced();

    @Query("SELECT * FROM ActiveAlarm WHERE requestCode = :requestCode")
    Single<ActiveAlarm> getById(int requestCode);
}
