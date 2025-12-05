package com.bitflaker.lucidsourcekit.database.alarms.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bitflaker.lucidsourcekit.database.alarms.entities.ActiveAlarm
import com.bitflaker.lucidsourcekit.database.alarms.entities.ActiveAlarmDetails
import com.bitflaker.lucidsourcekit.database.alarms.entities.StoredAlarm
import com.bitflaker.lucidsourcekit.database.alarms.entities.results.AlarmTimestamps

@Dao
interface ActiveAlarmDao {
    @Query("SELECT * FROM activealarm")
    suspend fun getAll(): List<ActiveAlarm>

    @Query("SELECT ActiveAlarm.*, StoredAlarm.pattern, StoredAlarm.alarmId AS storedAlarmId FROM StoredAlarm LEFT JOIN ActiveAlarm ON ActiveAlarm.requestCode = StoredAlarm.requestCodeActiveAlarm WHERE ActiveAlarm.requestCode != -1")
    suspend fun getAllDetails(): List<ActiveAlarmDetails>

    @Query("SELECT * FROM (SELECT t1.requestCode+1 AS Id FROM ActiveAlarm t1 WHERE NOT EXISTS(SELECT * FROM ActiveAlarm t2 WHERE t2.requestCode = t1.requestCode + 1 ) UNION SELECT 1 AS Id WHERE NOT EXISTS (SELECT * FROM ActiveAlarm t3 WHERE t3.requestCode = 1)) ot ORDER BY 1 LIMIT 1")
    suspend fun getFirstFreeRequestCode(): Int

    @Query("SELECT initialTime as alarmTimestamp, StoredAlarm.bedtimeTimestamp FROM ActiveAlarm LEFT JOIN StoredAlarm ON StoredAlarm.requestCodeActiveAlarm = requestCode WHERE requestCode != -1 ORDER BY initialTime ASC LIMIT 1")
    suspend fun getNextUpcomingAlarmTimestamp(): List<AlarmTimestamps>

    @Update
    suspend fun update(alarm: ActiveAlarm)

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insert(alarm: ActiveAlarm)

    @Delete
    suspend fun delete(alarm: ActiveAlarm)

    @Query("DELETE FROM ActiveAlarm")
    suspend fun deleteAll()

    @Query("DELETE FROM ActiveAlarm WHERE requestCode != -1")
    suspend fun deleteAllButUnreferenced()

    @Query("SELECT * FROM ActiveAlarm WHERE requestCode = :requestCode")
    suspend fun getById(requestCode: Int): ActiveAlarm?

    @Query("DELETE FROM ActiveAlarm WHERE requestCode = :requestCodeActiveAlarm")
    suspend fun deleteById(requestCodeActiveAlarm: Int)

    @Query("SELECT StoredAlarm.* FROM ActiveAlarm LEFT JOIN StoredAlarm ON ActiveAlarm.requestCode = StoredAlarm.requestCodeActiveAlarm WHERE initialTime = :alarmTime")
    suspend fun getStoredAlarmByAlarmTime(alarmTime: Long): List<StoredAlarm>

    @Query("DELETE FROM ActiveAlarm WHERE requestCode != -1")
    suspend fun clear()
}
