package com.bitflaker.lucidsourcekit.database.questionnaire.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.bitflaker.lucidsourcekit.database.questionnaire.entities.Questionnaire
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

@Dao
interface QuestionnaireDao {
    @Query("SELECT * FROM Questionnaire ORDER BY id")
    fun getAll(): Single<List<Questionnaire>>

    @Query("SELECT * FROM Questionnaire WHERE id = :id")
    fun getById(id: Int): Single<Questionnaire>

    @Update
    fun update(entry: Questionnaire): Completable

    @Insert
    fun insert(entry: Questionnaire): Single<Long>

    @Delete
    fun delete(entry: Questionnaire): Completable

    @Delete
    fun deleteAll(entries: List<Questionnaire>)

    @Query("SELECT COUNT(*) FROM Questionnaire WHERE title = :title")
    fun exists(title: String): Single<Boolean>
}