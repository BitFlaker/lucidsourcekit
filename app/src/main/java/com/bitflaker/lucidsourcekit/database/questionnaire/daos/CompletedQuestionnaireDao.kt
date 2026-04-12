package com.bitflaker.lucidsourcekit.database.questionnaire.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.bitflaker.lucidsourcekit.database.questionnaire.entities.CompletedQuestionnaire
import com.bitflaker.lucidsourcekit.database.questionnaire.entities.results.CompletedQuestionnaireDetails

@Dao
interface CompletedQuestionnaireDao {
    @Query("SELECT * FROM CompletedQuestionnaire ORDER BY id")
    suspend fun getAll(): List<CompletedQuestionnaire>

    @Query("SELECT cq.id, cq.questionnaireId, cq.answerDuration, cq.timestamp, q.title, q.description, q.colorCode, q.orderNr FROM CompletedQuestionnaire cq LEFT JOIN Questionnaire q ON cq.questionnaireId = q.id ORDER BY timestamp DESC")
    suspend fun getAllDetails(): List<CompletedQuestionnaireDetails>

    @Query("SELECT * FROM CompletedQuestionnaire WHERE id = :id")
    suspend fun getById(id: Int): CompletedQuestionnaire

    @Query("SELECT cq.id, cq.questionnaireId, cq.answerDuration, cq.timestamp, q.title, q.description, q.colorCode, q.orderNr FROM CompletedQuestionnaire cq LEFT JOIN Questionnaire q ON cq.questionnaireId = q.id WHERE cq.id = :id")
    suspend fun getDetailsById(id: Int): CompletedQuestionnaireDetails

    @Query("SELECT COALESCE(MIN(timestamp), -1) FROM CompletedQuestionnaire")
    suspend fun getOldestTime(): Long

    @Query("SELECT timestamp FROM CompletedQuestionnaire WHERE timestamp >= :from AND timestamp < :to ORDER BY timestamp DESC")
    suspend fun getTimestampsBetween(from: Long, to: Long): List<Long>

    @Query("SELECT * FROM CompletedQuestionnaire WHERE timestamp >= :startTimestamp AND timestamp < :endTimestamp")
    suspend fun getEntriesInTimestampRange(startTimestamp: Long, endTimestamp: Long): List<CompletedQuestionnaire>

    @Update
    suspend fun update(entry: CompletedQuestionnaire)

    @Insert
    suspend fun insert(entry: CompletedQuestionnaire): Long

    @Insert
    suspend fun insertAll(entry: List<CompletedQuestionnaire>)

    @Delete
    suspend fun delete(entry: CompletedQuestionnaire)

    @Delete
    suspend fun deleteAll(entries: List<CompletedQuestionnaire>)

    @Query("DELETE FROM CompletedQuestionnaire")
    suspend fun deleteAll()

    @Query("SELECT cq.id, cq.questionnaireId, cq.answerDuration, cq.timestamp, q.title, q.description, q.colorCode, q.orderNr FROM CompletedQuestionnaire cq LEFT JOIN Questionnaire q ON cq.questionnaireId = q.id WHERE timestamp >= :dayFrom AND timestamp < :dayTo ORDER BY timestamp DESC")
    suspend fun getByTimeFrame(dayFrom: Long, dayTo: Long): MutableList<CompletedQuestionnaireDetails>

    @Query("SELECT COUNT(*) FROM CompletedQuestionnaire WHERE timestamp >= :dayFrom AND timestamp < :dayTo")
    suspend fun getQuestionnaireCount(dayFrom: Long, dayTo: Long): Int
}