package com.bitflaker.lucidsourcekit.main.export.templates.data

data class QuestionnaireExportData(
    val name: String,
    val questions: List<QuestionnaireExportQuestion>
)