package com.bitflaker.lucidsourcekit.database.questionnaire.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(primaryKeys = ["questionId", "id"],
    foreignKeys = [
        ForeignKey(
            entity = Question::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("questionId"),
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index("questionId")]
)
data class QuestionOptions(
    val questionId: Int,
    val id: Int,
    val text: String,
    val description: String?
)