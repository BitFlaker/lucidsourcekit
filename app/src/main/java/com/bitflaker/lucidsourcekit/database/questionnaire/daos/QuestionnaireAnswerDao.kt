package com.bitflaker.lucidsourcekit.database.questionnaire.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.bitflaker.lucidsourcekit.database.questionnaire.entities.QuestionnaireAnswer

@Dao
interface QuestionnaireAnswerDao {
    @Query("SELECT * FROM QuestionnaireAnswer ORDER BY completedQuestionnaireId, questionId")
    suspend fun getAll(): List<QuestionnaireAnswer>

    @Query("SELECT * FROM QuestionnaireAnswer WHERE completedQuestionnaireId = :completedQuestionnaireId AND questionId = :questionId")
    suspend fun getById(completedQuestionnaireId: Int, questionId: Int): QuestionnaireAnswer

    @Query("SELECT * FROM QuestionnaireAnswer WHERE completedQuestionnaireId = :completedQuestionnaireId")
    suspend fun getAll(completedQuestionnaireId: Int): List<QuestionnaireAnswer>

    @Update
    suspend fun update(entry: QuestionnaireAnswer)

    @Insert
    suspend fun insert(entry: QuestionnaireAnswer)

    @Delete
    suspend fun delete(entry: QuestionnaireAnswer)

    @Delete
    suspend fun deleteAll(entries: List<QuestionnaireAnswer>)

    @Query("DELETE FROM QuestionnaireAnswer")
    suspend fun deleteAll()
}