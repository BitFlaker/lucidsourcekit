package com.bitflaker.lucidsourcekit.database.questionnaire.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.bitflaker.lucidsourcekit.database.questionnaire.entities.QuestionOptions

@Dao
interface QuestionOptionsDao {
    @Query("SELECT * FROM QuestionOptions ORDER BY questionId, id")
    suspend fun getAll(): List<QuestionOptions>

    @Query("SELECT * FROM QuestionOptions WHERE questionId = :questionId AND isHidden = 0 ORDER BY orderNr, id")
    suspend fun getAllForQuestion(questionId: Int): List<QuestionOptions>

    @Query("SELECT MAX(id) + 1 FROM QuestionOptions WHERE questionId = :questionId")
    suspend fun getNextId(questionId: Int): Int

    @Query("SELECT * FROM QuestionOptions WHERE questionId = :questionId AND id = :optionId")
    suspend fun getById(questionId: Int, optionId: Int): QuestionOptions

    @Query("SELECT COUNT(*) > 0 FROM SelectedOptions WHERE questionId = :questionId AND optionId = :optionId")
    suspend fun isReferenced(questionId: Int, optionId: Int): Boolean

    @Update
    suspend fun update(entry: QuestionOptions)

    @Insert
    suspend fun insert(entry: QuestionOptions)

    @Insert
    suspend fun insertAll(entry: List<QuestionOptions>)

    @Delete
    suspend fun delete(entry: QuestionOptions)

    @Delete
    suspend fun deleteAll(entries: List<QuestionOptions>)
}