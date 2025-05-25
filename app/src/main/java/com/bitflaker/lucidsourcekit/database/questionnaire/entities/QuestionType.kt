package com.bitflaker.lucidsourcekit.database.questionnaire.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class QuestionType(
    @PrimaryKey val id: Int,
    val description: String
) {
    companion object {
        var defaults: List<QuestionType> = listOf(
            QuestionType(0, "Rate"),
            QuestionType(1, "Single selection"),
            QuestionType(2, "Multi selection"),
            QuestionType(3, "True / False"),
            QuestionType(4, "Text"),
        )
    }
}
