package com.bitflaker.lucidsourcekit.views.types

data class DataValue(val value: Double, val label: String?) {
    private lateinit var lines: List<String>

    /**
     * Gets the individual lines of the label. Every item represents a new line.
     * This will be lazily evaluated.
     */
    fun getLines(): List<String> {
        if (!this::lines.isInitialized) {
            lines = label?.split("\n".toRegex())
                ?.dropLastWhile { line -> line.isEmpty() }
                ?: listOf()
        }
        return lines
    }
}