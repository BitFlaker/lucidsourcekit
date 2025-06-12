package com.bitflaker.lucidsourcekit.database.questionnaire.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.bitflaker.lucidsourcekit.database.questionnaire.entities.QuestionOptions
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

@Dao
interface QuestionOptionsDao {
    @Query("SELECT * FROM QuestionOptions ORDER BY questionId, id")
    fun getAll(): Single<List<QuestionOptions>>

    @Query("SELECT * FROM QuestionOptions WHERE questionId = :questionId AND isHidden = 0 ORDER BY orderNr, id")
    fun getAllForQuestion(questionId: Int): Single<List<QuestionOptions>>

    @Query("SELECT MAX(id) + 1 FROM QuestionOptions WHERE questionId = :questionId")
    fun getNextId(questionId: Int): Single<Int>

    @Query("SELECT * FROM QuestionOptions WHERE questionId = :questionId AND id = :optionId")
    fun getById(questionId: Int, optionId: Int): Single<QuestionOptions>

    @Query("SELECT COUNT(*) > 0 FROM SelectedOptions WHERE questionId = :questionId AND optionId = :optionId")
    fun isReferenced(questionId: Int, optionId: Int): Single<Boolean>

    @Update
    fun update(entry: QuestionOptions): Completable

    @Insert
    fun insert(entry: QuestionOptions): Completable

    @Insert
    fun insertAll(entry: List<QuestionOptions>): Completable

    @Delete
    fun delete(entry: QuestionOptions): Completable
    @Delete
    fun deleteAll(entries: List<QuestionOptions>)
}