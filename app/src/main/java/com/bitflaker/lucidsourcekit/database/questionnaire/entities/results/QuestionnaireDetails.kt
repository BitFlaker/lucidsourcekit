package com.bitflaker.lucidsourcekit.database.questionnaire.entities.results

data class QuestionnaireDetails(
    val id: Int,
    val title: String,
    val description: String?,
    val isCompact: Boolean,
    val colorCode: String?,
    val questionCount: Int,
    val completedCount: Int,
    val averageDuration: Int,
)
