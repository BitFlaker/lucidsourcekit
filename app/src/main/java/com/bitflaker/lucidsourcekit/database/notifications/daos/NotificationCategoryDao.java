package com.bitflaker.lucidsourcekit.database.notifications.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.bitflaker.lucidsourcekit.database.notifications.entities.NotificationCategory;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface NotificationCategoryDao {
    @Query("SELECT * FROM NotificationCategory ORDER BY id")
    Single<List<NotificationCategory>> getAll();

    @Query("SELECT * FROM NotificationCategory WHERE id = :notificationCategoryId")
    Single<NotificationCategory> getById(String notificationCategoryId);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    Completable insertAll(NotificationCategory... NotificationCategories);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    Completable insert(NotificationCategory NotificationCategory);

    @Delete
    Completable delete(NotificationCategory NotificationCategory);

    @Delete
    void deleteAll(List<NotificationCategory> NotificationCategories);

    @Update
    Completable update(NotificationCategory NotificationCategory);
}
