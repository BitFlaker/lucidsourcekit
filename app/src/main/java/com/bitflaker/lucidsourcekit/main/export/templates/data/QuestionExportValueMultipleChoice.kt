package com.bitflaker.lucidsourcekit.main.export.templates.data

data class QuestionExportValueMultipleChoice(
    val options: Array<String>,
    val selectedIndices: IntArray,
) : QuestionExportValue {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as QuestionExportValueMultipleChoice

        if (!options.contentEquals(other.options)) return false
        if (!selectedIndices.contentEquals(other.selectedIndices)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = options.contentHashCode()
        result = 31 * result + selectedIndices.contentHashCode()
        return result
    }
}