package com.bitflaker.lucidsourcekit.database.questionnaire.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.bitflaker.lucidsourcekit.database.questionnaire.entities.SelectedOptions

@Dao
interface SelectedOptionsDao {
    @Query("SELECT * FROM SelectedOptions ORDER BY completedQuestionnaireId, questionId, optionId")
    suspend fun getAll(): List<SelectedOptions>

    @Query("SELECT * FROM SelectedOptions WHERE completedQuestionnaireId = :completedQuestionnaireId AND questionId = :questionId ORDER BY completedQuestionnaireId, questionId, optionId")
    suspend fun getById(completedQuestionnaireId: Int, questionId: Int): List<SelectedOptions>

    @Insert
    suspend fun insert(entry: SelectedOptions)

    @Insert
    suspend fun insertAll(entry: List<SelectedOptions>)

    @Delete
    suspend fun delete(entry: SelectedOptions)

    @Delete
    suspend fun deleteAll(entries: List<SelectedOptions>)
}