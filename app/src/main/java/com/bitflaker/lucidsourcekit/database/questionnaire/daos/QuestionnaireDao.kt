package com.bitflaker.lucidsourcekit.database.questionnaire.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.bitflaker.lucidsourcekit.database.questionnaire.entities.Questionnaire
import com.bitflaker.lucidsourcekit.database.questionnaire.entities.results.QuestionnaireDetails

@Dao
interface QuestionnaireDao {
    @Query("SELECT * FROM Questionnaire ORDER BY id")
    suspend fun getAll(): List<Questionnaire>

    @Query("""SELECT fq.*, COUNT(cq.id) AS completedCount, AVG(cq.answerDuration) AS averageDuration 
            FROM (
                SELECT q.id, q.title, q.description, q.isCompact, q.colorCode, Count(*) AS questionCount 
                FROM Questionnaire q 
                LEFT JOIN Question ON q.id = Question.questionnaireId 
                GROUP BY q.id, q.title, q.description, q.isCompact, q.colorCode
                HAVING q.isHidden = 0 AND Question.isHidden = 0
                ORDER BY q.id
            ) fq
            LEFT JOIN CompletedQuestionnaire cq ON fq.id = cq.questionnaireId
            GROUP BY fq.id, fq.title, fq.description, fq.isCompact, fq.colorCode, fq.questionCount""")
    suspend fun getAllDetails(): List<QuestionnaireDetails>

    @Query("""SELECT fq.*, COUNT(cq.id) AS completedCount, AVG(cq.answerDuration) AS averageDuration 
            FROM (
                SELECT q.id, q.title, q.description, q.isCompact, q.colorCode, Count(Question.id) AS questionCount 
                FROM Questionnaire q 
                LEFT JOIN Question ON q.id = Question.questionnaireId 
                GROUP BY q.id, q.title, q.description, q.isCompact, q.colorCode
                HAVING q.id = :questionnaireId AND q.isHidden = 0 AND Question.isHidden = 0
                ORDER BY q.id
            ) fq
            LEFT JOIN CompletedQuestionnaire cq ON fq.id = cq.questionnaireId""")
    suspend fun getDetailsById(questionnaireId: Int): QuestionnaireDetails

    @Query("SELECT (SELECT COUNT(*) FROM Question WHERE questionnaireId = :id) + (SELECT COUNT(*) FROM CompletedQuestionnaire WHERE questionnaireId = :id) > 0")
    suspend fun isReferenced(id: Int): Boolean

    @Query("SELECT * FROM Questionnaire WHERE id = :id")
    suspend fun getById(id: Int): Questionnaire

    @Update
    suspend fun update(entry: Questionnaire)

    @Insert
    suspend fun insert(entry: Questionnaire): Long

    @Delete
    suspend fun delete(entry: Questionnaire)

    @Delete
    suspend fun deleteAll(entries: List<Questionnaire>)

    @Query("SELECT COUNT(*) FROM Questionnaire WHERE title = :title")
    suspend fun exists(title: String): Boolean
}