package com.bitflaker.lucidsourcekit.database.questionnaire.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.bitflaker.lucidsourcekit.database.questionnaire.entities.Question

@Dao
interface QuestionDao {
    @Query("SELECT * FROM Question ORDER BY id")
    suspend fun getAll(): List<Question>

    @Query("SELECT * FROM Question WHERE questionnaireId = :id AND isHidden = 0 ORDER BY orderNr, id")
    suspend fun getAllForQuestionnaire(id: Int): List<Question>

    @Query("SELECT * FROM Question WHERE id = :id")
    suspend fun getById(id: Int): Question

    @Query("SELECT (SELECT COUNT(*) FROM QuestionnaireAnswer WHERE questionId = :id) + (SELECT COUNT(*) FROM SelectedOptions WHERE questionId = :id) > 0")
    suspend fun isQuestionReferenced(id: Int): Boolean

    @Update
    suspend fun update(entry: Question)

    @Insert
    suspend fun insert(entry: Question): Long

    @Insert
    suspend fun insertAll(entry: List<Question>)

    @Delete
    suspend fun delete(entry: Question)
    @Delete
    suspend fun deleteAll(entries: List<Question>)
}