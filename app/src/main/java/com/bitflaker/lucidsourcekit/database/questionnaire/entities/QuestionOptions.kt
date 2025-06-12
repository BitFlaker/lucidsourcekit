package com.bitflaker.lucidsourcekit.database.questionnaire.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
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
    var questionId: Int,
    var id: Int,
    var text: String,
    var orderNr: Int,
    var isHidden: Boolean,
    val description: String?
) {
    @Ignore
    constructor(questionId: Int) : this(questionId, -1, "", 0, false, null)
}