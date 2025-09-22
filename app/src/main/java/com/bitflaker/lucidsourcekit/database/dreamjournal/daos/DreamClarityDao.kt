package com.bitflaker.lucidsourcekit.database.dreamjournal.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.DreamClarity

@Dao
interface DreamClarityDao {
    @Query("SELECT * FROM DreamClarity")
    suspend fun getAll(): MutableList<DreamClarity>

    @Insert(onConflict = OnConflictStrategy.Companion.IGNORE)
    suspend fun insertAll(dreamClarities: Array<DreamClarity>)

    @Delete
    suspend fun delete(dreamClarity: DreamClarity)
}
