package com.bitflaker.lucidsourcekit.database.questionnaire.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(foreignKeys = [
    ForeignKey(
        entity = Questionnaire::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("questionnaireId"),
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.CASCADE
    )],
    indices = [Index("questionnaireId")]
)
data class CompletedQuestionnaire(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val questionnaireId: Int,
    val timestamp: Long
) {
    @Ignore
    constructor(questionnaireId: Int, timestamp: Long): this(0, questionnaireId, timestamp)
}
