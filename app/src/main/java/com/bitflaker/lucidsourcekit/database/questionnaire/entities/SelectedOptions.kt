package com.bitflaker.lucidsourcekit.database.questionnaire.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(primaryKeys = ["completedQuestionnaireId", "questionId", "optionId"],
    foreignKeys = [
        ForeignKey(
            entity = QuestionnaireAnswer::class,
            parentColumns = arrayOf("completedQuestionnaireId", "questionId"),
            childColumns = arrayOf("completedQuestionnaireId", "questionId"),
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = QuestionOptions::class,
            parentColumns = arrayOf("questionId", "id"),
            childColumns = arrayOf("questionId", "optionId"),
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index("completedQuestionnaireId", "questionId"), Index("questionId", "optionId")]
)
data class SelectedOptions(
    val completedQuestionnaireId: Int,
    val questionId: Int,
    val optionId: Int
)
