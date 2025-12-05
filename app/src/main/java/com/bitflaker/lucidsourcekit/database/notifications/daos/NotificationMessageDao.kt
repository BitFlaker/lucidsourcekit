package com.bitflaker.lucidsourcekit.database.notifications.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bitflaker.lucidsourcekit.database.notifications.entities.NotificationMessage

@Dao
interface NotificationMessageDao {
    @Query("SELECT * FROM NotificationMessage ORDER BY notificationCategoryId, id")
    suspend fun getAll(): List<NotificationMessage>

    @Query("SELECT * FROM NotificationMessage WHERE notificationCategoryId = :notificationCategoryId ORDER BY notificationCategoryId, id")
    suspend fun getAllOfCategory(notificationCategoryId: String): MutableList<NotificationMessage>

    @Query("SELECT COUNT(*) FROM NotificationMessage WHERE notificationCategoryId = :notificationCategoryId")
    suspend fun getCountOfMessagesForCategory(notificationCategoryId: String): Int

    @Query("SELECT COUNT(*) FROM NotificationMessage WHERE notificationCategoryId = :notificationCategoryId")
    suspend fun getCountOfMessagesForCategoryAndObfuscationType(notificationCategoryId: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(notificationMessages: List<NotificationMessage>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(notificationMessage: NotificationMessage): Long

    @Delete
    suspend fun delete(notificationMessage: NotificationMessage)

    @Delete
    suspend fun deleteAll(notificationMessages: List<NotificationMessage>)

    @Update
    suspend fun update(notificationMessage: NotificationMessage)
}
