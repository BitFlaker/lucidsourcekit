package com.bitflaker.lucidsourcekit.database.questionnaire.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.bitflaker.lucidsourcekit.database.questionnaire.entities.QuestionnaireAnswer
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

@Dao
interface QuestionnaireAnswerDao {
    @Query("SELECT * FROM QuestionnaireAnswer ORDER BY completedQuestionnaireId, questionId")
    fun getAll(): Single<List<QuestionnaireAnswer>>

    @Query("SELECT * FROM QuestionnaireAnswer WHERE completedQuestionnaireId = :completedQuestionnaireId AND questionId = :questionId")
    fun getById(completedQuestionnaireId: Int, questionId: Int): Single<QuestionnaireAnswer>

    @Query("SELECT * FROM QuestionnaireAnswer WHERE completedQuestionnaireId = :completedQuestionnaireId")
    fun getAll(completedQuestionnaireId: Int): Single<List<QuestionnaireAnswer>>

    @Update
    fun update(entry: QuestionnaireAnswer): Completable

    @Insert
    fun insert(entry: QuestionnaireAnswer): Completable

    @Delete
    fun delete(entry: QuestionnaireAnswer): Completable

    @Delete
    fun deleteAll(entries: List<QuestionnaireAnswer>): Completable

    @Query("DELETE FROM QuestionnaireAnswer")
    fun deleteAll(): Completable
}