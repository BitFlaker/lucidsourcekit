package com.bitflaker.lucidsourcekit.database.questionnaire.entities.resulttables

data class CompletedQuestionnaireDetails(
    val id: Int,
    val questionnaireId: Int,
    val answerDuration: Long,
    val timestamp: Long,
    var title: String,
    var description: String?,
    var orderNr: Int,
    var colorCode: String?
)
