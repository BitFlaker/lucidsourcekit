package com.bitflaker.lucidsourcekit.main.export.templates.data

import java.util.Date

data class ExportData(
    val date: Date,
    val entries: List<JournalEntryExportData>,
    val questionnaires: List<QuestionnaireExportData>
)
