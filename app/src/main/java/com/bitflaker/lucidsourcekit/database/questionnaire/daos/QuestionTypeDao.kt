package com.bitflaker.lucidsourcekit.database.questionnaire.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bitflaker.lucidsourcekit.database.questionnaire.entities.QuestionType
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

@Dao
interface QuestionTypeDao {
    @Query("SELECT * FROM QuestionType ORDER BY id")
    fun getAll(): Single<List<QuestionType>>

    @Query("SELECT * FROM QuestionType WHERE id = :id")
    fun getById(id: Int): Single<QuestionType>

    @Update
    fun update(entry: QuestionType): Completable

    @Insert
    fun insert(entry: QuestionType): Completable

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAll(entry: List<QuestionType>): Completable

    @Delete
    fun delete(entry: QuestionType): Completable

    @Delete
    fun deleteAll(entries: List<QuestionType>)
}