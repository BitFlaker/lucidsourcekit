package com.bitflaker.lucidsourcekit.main.export.templates.data

data class QuestionExportValueRating(
    val minValue: Int,
    val maxValue: Int,
    val value: Int,
) : QuestionExportValue