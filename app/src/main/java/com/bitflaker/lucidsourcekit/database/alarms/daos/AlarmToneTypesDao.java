package com.bitflaker.lucidsourcekit.database.alarms.daos;

import static androidx.room.OnConflictStrategy.REPLACE;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.bitflaker.lucidsourcekit.database.alarms.entities.AlarmToneTypes;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface AlarmToneTypesDao {
    @Query("SELECT * FROM AlarmToneTypes")
    Single<List<AlarmToneTypes>> getAll();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    Completable insertAll(AlarmToneTypes... alarmToneTypes);

    @Insert(onConflict = REPLACE)
    Single<Long> insert(AlarmToneTypes alarmToneTypes);

    @Delete
    Completable delete(AlarmToneTypes alarmToneTypes);

    @Query("DELETE FROM AlarmToneTypes")
    Completable deleteAll();
}
