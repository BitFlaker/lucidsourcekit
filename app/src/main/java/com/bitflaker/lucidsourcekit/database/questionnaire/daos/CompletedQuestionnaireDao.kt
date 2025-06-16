package com.bitflaker.lucidsourcekit.database.questionnaire.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bitflaker.lucidsourcekit.database.questionnaire.entities.CompletedQuestionnaire
import com.bitflaker.lucidsourcekit.database.questionnaire.entities.resulttables.CompletedQuestionnaireDetails
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

@Dao
interface CompletedQuestionnaireDao {
    @Query("SELECT * FROM CompletedQuestionnaire ORDER BY id")
    fun getAll(): Single<List<CompletedQuestionnaire>>

    @Query("SELECT cq.id, cq.questionnaireId, cq.answerDuration, cq.timestamp, q.title, q.description, q.colorCode, q.orderNr FROM CompletedQuestionnaire cq LEFT JOIN Questionnaire q ON cq.questionnaireId = q.id ORDER BY timestamp DESC")
    fun getAllDetails(): Single<List<CompletedQuestionnaireDetails>>

    @Query("SELECT * FROM CompletedQuestionnaire WHERE id = :id")
    fun getById(id: Int): Single<CompletedQuestionnaire>

    @Query("SELECT cq.id, cq.questionnaireId, cq.answerDuration, cq.timestamp, q.title, q.description, q.colorCode, q.orderNr FROM CompletedQuestionnaire cq LEFT JOIN Questionnaire q ON cq.questionnaireId = q.id WHERE cq.id = :id")
    fun getDetailsById(id: Int): Single<CompletedQuestionnaireDetails>

    @Update
    fun update(entry: CompletedQuestionnaire): Completable

    @Insert
    fun insert(entry: CompletedQuestionnaire): Single<Long>

    @Insert
    fun insertAll(entry: List<CompletedQuestionnaire>): Completable

    @Delete
    fun delete(entry: CompletedQuestionnaire): Completable

    @Delete
    fun deleteAll(entries: List<CompletedQuestionnaire>)

    @Query("DELETE FROM CompletedQuestionnaire")
    fun deleteAll(): Completable

    @Query("SELECT cq.id, cq.questionnaireId, cq.answerDuration, cq.timestamp, q.title, q.description, q.colorCode, q.orderNr FROM CompletedQuestionnaire cq LEFT JOIN Questionnaire q ON cq.questionnaireId = q.id WHERE timestamp >= :dayFrom AND timestamp < :dayTo")
    fun getByTimeFrame(dayFrom: Long, dayTo: Long): Single<List<CompletedQuestionnaireDetails>>

    @Query("SELECT COUNT(*) FROM CompletedQuestionnaire WHERE timestamp >= :dayFrom AND timestamp < :dayTo")
    fun getQuestionnaireCount(dayFrom: Long, dayTo: Long): Single<Int>
}