package com.bitflaker.lucidsourcekit.database.questionnaire.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.bitflaker.lucidsourcekit.database.questionnaire.entities.SelectedOptions
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

@Dao
interface SelectedOptionsDao {
    @Query("SELECT * FROM SelectedOptions ORDER BY completedQuestionnaireId, questionId, optionId")
    fun getAll(): Single<List<SelectedOptions>>

    @Query("SELECT * FROM SelectedOptions WHERE completedQuestionnaireId = :completedQuestionnaireId AND questionId = :questionId ORDER BY completedQuestionnaireId, questionId, optionId")
    fun getById(completedQuestionnaireId: Int, questionId: Int): Single<List<SelectedOptions>>

    @Insert
    fun insert(entry: SelectedOptions): Completable

    @Insert
    fun insertAll(entry: List<SelectedOptions>): Completable

    @Delete
    fun delete(entry: SelectedOptions): Completable

    @Delete
    fun deleteAll(entries: List<SelectedOptions>)
}