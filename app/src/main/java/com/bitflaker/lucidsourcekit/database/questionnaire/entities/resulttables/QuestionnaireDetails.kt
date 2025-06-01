package com.bitflaker.lucidsourcekit.database.questionnaire.entities.resulttables

data class QuestionnaireDetails(
    val id: Int,
    val title: String,
    val description: String?,
    val isHidden: Boolean,
    val isCompact: Boolean,
    val questionCount: Int,
    val completedCount: Int,
    val averageDuration: Int,
)
