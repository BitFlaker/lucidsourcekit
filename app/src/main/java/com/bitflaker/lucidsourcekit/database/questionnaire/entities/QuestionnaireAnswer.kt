package com.bitflaker.lucidsourcekit.database.questionnaire.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(primaryKeys = ["completedQuestionnaireId", "questionId"],
    foreignKeys = [
        ForeignKey(
            entity = CompletedQuestionnaire::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("completedQuestionnaireId"),
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Question::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("questionId"),
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index("completedQuestionnaireId"), Index("questionId")]
)
data class QuestionnaireAnswer(
    val completedQuestionnaireId: Int,
    val questionId: Int,
    val value: String?,
)
