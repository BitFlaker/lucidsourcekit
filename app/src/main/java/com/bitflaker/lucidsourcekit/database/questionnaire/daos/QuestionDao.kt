package com.bitflaker.lucidsourcekit.database.questionnaire.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.bitflaker.lucidsourcekit.database.questionnaire.entities.Question
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

@Dao
interface QuestionDao {
    @Query("SELECT * FROM Question ORDER BY id")
    fun getAll(): Single<List<Question>>

    @Query("SELECT * FROM Question WHERE questionnaireId = :id AND isHidden = 0 ORDER BY orderNr, id")
    fun getAllForQuestionnaire(id: Int): Single<List<Question>>

    @Query("SELECT * FROM Question WHERE id = :id")
    fun getById(id: Int): Single<Question>

    @Query("SELECT (SELECT COUNT(*) FROM QuestionnaireAnswer WHERE questionId = :id) + (SELECT COUNT(*) FROM SelectedOptions WHERE questionId = :id) > 0")
    fun isQuestionReferenced(id: Int): Single<Boolean>

    @Update
    fun update(entry: Question): Completable

    @Insert
    fun insert(entry: Question): Single<Long>

    @Insert
    fun insertAll(entry: List<Question>): Completable

    @Delete
    fun delete(entry: Question): Completable
    @Delete
    fun deleteAll(entries: List<Question>)
}