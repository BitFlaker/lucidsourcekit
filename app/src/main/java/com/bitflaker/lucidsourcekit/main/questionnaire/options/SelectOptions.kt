package com.bitflaker.lucidsourcekit.main.questionnaire.options

data class SelectOptions(val values: Array<String>) : ControlOptions {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SelectOptions
        return values.contentEquals(other.values)
    }

    override fun hashCode(): Int {
        return values.contentHashCode()
    }
}