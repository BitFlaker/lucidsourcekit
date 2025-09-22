package com.bitflaker.lucidsourcekit.database.alarms.updated.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.bitflaker.lucidsourcekit.database.alarms.updated.entities.StoredAlarm

@Dao
interface StoredAlarmDao {
    @Query("SELECT * FROM StoredAlarm")
    suspend fun getAll(): List<StoredAlarm>

    @Query("SELECT * FROM StoredAlarm WHERE requestCodeActiveAlarm != -1")
    suspend fun  getAllActive(): List<StoredAlarm>

    @Query("SELECT * FROM StoredAlarm WHERE alarmId IN (:alarmIds)")
    suspend fun getAllById(alarmIds: List<Long>): List<StoredAlarm>

    @Query("SELECT * FROM StoredAlarm WHERE alarmId = :alarmId")
    suspend fun getById(alarmId: Long): StoredAlarm

    @Query("UPDATE StoredAlarm SET requestCodeActiveAlarm = :requestCode WHERE alarmId = :storedAlarmId")
    suspend fun updateRequestCode(storedAlarmId: Long, requestCode: Int)

    @Update
    suspend fun update(alarm: StoredAlarm)

    @Insert
    suspend fun insert(alarm: StoredAlarm): Long

    @Delete
    suspend fun delete(alarm: StoredAlarm)

    @Query("DELETE FROM StoredAlarm")
    suspend fun deleteAll()

    @Query("UPDATE StoredAlarm SET isAlarmActive = :isAlarmActive WHERE alarmId = :alarmId")
    suspend fun setActiveState(alarmId: Long, isAlarmActive: Boolean)
}
