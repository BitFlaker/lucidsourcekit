package com.bitflaker.lucidsourcekit.database.notifications.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.bitflaker.lucidsourcekit.database.notifications.entities.NotificationMessage;
import com.bitflaker.lucidsourcekit.general.NotificationObfuscationLookup;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface NotificationMessageDao {
    @Query("SELECT * FROM NotificationMessage ORDER BY notificationCategoryId, obfuscationTypeId, id")
    Single<List<NotificationMessage>> getAll();

    @Query("SELECT * FROM NotificationMessage WHERE notificationCategoryId = :notificationCategoryId ORDER BY obfuscationTypeId, id")
    Single<List<NotificationMessage>> getAllOfCategory(String notificationCategoryId);

    @Query("SELECT * FROM NotificationMessage WHERE notificationCategoryId = :notificationCategoryId AND obfuscationTypeId = :obfuscationTypeId ORDER BY notificationCategoryId, id")
    Single<List<NotificationMessage>> getAllOfCategoryAndObfuscationType(String notificationCategoryId, int obfuscationTypeId);

    @Query("SELECT COUNT(*) FROM NotificationMessage WHERE notificationCategoryId = :notificationCategoryId")
    Single<Integer> getCountOfMessagesForCategory(String notificationCategoryId);

    @Query("SELECT COUNT(*) FROM NotificationMessage WHERE notificationCategoryId = :notificationCategoryId AND obfuscationTypeId = :obfuscationTypeId")
    Single<Integer> getCountOfMessagesForCategoryAndObfuscationType(String notificationCategoryId, int obfuscationTypeId);

    @Query("SELECT notificationCategoryId, obfuscationTypeId, COUNT(*) AS messageCount FROM NotificationMessage GROUP BY notificationCategoryId, obfuscationTypeId")
    Single<List<NotificationObfuscationLookup.NotificationObfuscationCount>> getMessageCountsForObfuscationType();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    Completable insertAll(List<NotificationMessage> notificationMessages);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(NotificationMessage notificationMessage);

    @Delete
    Completable delete(NotificationMessage notificationMessage);

    @Delete
    void deleteAll(List<NotificationMessage> notificationMessages);

    @Update
    Completable update(NotificationMessage notificationMessage);
}
