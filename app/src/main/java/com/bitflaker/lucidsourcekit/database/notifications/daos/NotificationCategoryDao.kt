package com.bitflaker.lucidsourcekit.database.notifications.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bitflaker.lucidsourcekit.database.notifications.entities.NotificationCategory

@Dao
interface NotificationCategoryDao {
    @Query("SELECT * FROM NotificationCategory ORDER BY id")
    suspend fun getAll(): MutableList<NotificationCategory>

    @Query("SELECT * FROM NotificationCategory WHERE id = :notificationCategoryId")
    suspend fun getById(notificationCategoryId: String): NotificationCategory

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(notificationCategories: Array<NotificationCategory>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(notificationCategory: NotificationCategory)

    @Delete
    suspend fun delete(notificationCategory: NotificationCategory)

    @Delete
    suspend fun deleteAll(notificationCategories: List<NotificationCategory>)

    @Update
    suspend fun update(notificationCategory: NotificationCategory)
}
