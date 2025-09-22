package com.bitflaker.lucidsourcekit.database.dreamjournal.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.DreamType

@Dao
interface DreamTypeDao {
    @Query("SELECT * FROM DreamType")
    suspend fun getAll(): MutableList<DreamType>

    @Insert(onConflict = OnConflictStrategy.Companion.IGNORE)
    suspend fun insertAll(types: Array<DreamType>)

    @Delete
    suspend fun delete(type: DreamType)
}
