package com.bitflaker.lucidsourcekit.database.questionnaire.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(foreignKeys = [
    ForeignKey(
        entity = QuestionType::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("questionTypeId"),
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.CASCADE
    ),
    ForeignKey(
        entity = Questionnaire::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("questionnaireId"),
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.CASCADE
    )],
    indices = [Index("questionTypeId"), Index("questionnaireId")]
)
data class Question(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val question: String,
    val questionTypeId: Int,
    val questionnaireId: Int,
    val orderNr: Int,
    val valueFrom: Int?,
    val valueTo: Int?,
    val autoContinue: Boolean,
    val isHidden: Boolean
) {
    @Ignore
    constructor(question: String, questionTypeId: Int, questionnaireId: Int, valueFrom: Int?, valueTo: Int?, autoContinue: Boolean):
            this(0, question, questionTypeId, questionnaireId, 0, valueFrom, valueTo, autoContinue, false)
}
