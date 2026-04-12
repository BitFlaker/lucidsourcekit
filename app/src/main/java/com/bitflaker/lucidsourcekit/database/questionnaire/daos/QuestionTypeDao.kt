package com.bitflaker.lucidsourcekit.database.questionnaire.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bitflaker.lucidsourcekit.database.questionnaire.entities.QuestionType

@Dao
interface QuestionTypeDao {
    @Query("SELECT * FROM QuestionType ORDER BY id")
    suspend fun getAll(): List<QuestionType>

    @Query("SELECT * FROM QuestionType WHERE id = :id")
    suspend fun getById(id: Int): QuestionType

    @Update
    suspend fun update(entry: QuestionType)

    @Insert
    suspend fun insert(entry: QuestionType)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entry: List<QuestionType>)

    @Delete
    suspend fun delete(entry: QuestionType)

    @Delete
    suspend fun deleteAll(entries: List<QuestionType>)
}