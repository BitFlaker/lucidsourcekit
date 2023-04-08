package com.bitflaker.lucidsourcekit.database.notifications.daos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.bitflaker.lucidsourcekit.database.notifications.entities.NotificationObfuscations;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface NotificationObfuscationDao {
    @Query("SELECT * FROM NotificationObfuscations ORDER BY obfuscationTypeId")
    Single<List<NotificationObfuscations>> getAll();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    Completable insertAll(NotificationObfuscations... NotificationObfuscations);
}
