package com.bitflaker.lucidsourcekit.database.questionnaire.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bitflaker.lucidsourcekit.database.questionnaire.entities.CompletedQuestionnaire
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

@Dao
interface CompletedQuestionnaireDao {
    @Query("SELECT * FROM CompletedQuestionnaire ORDER BY id")
    fun getAll(): Single<List<CompletedQuestionnaire>>

    @Query("SELECT * FROM CompletedQuestionnaire WHERE id = :id")
    fun getById(id: Int): Single<CompletedQuestionnaire>

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
}