package com.bitflaker.lucidsourcekit.utils.pdf.layout

data class LayoutContext @JvmOverloads constructor(
    var spanSizeLookupGrid: Array<IntArray>? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LayoutContext

        return spanSizeLookupGrid.contentDeepEquals(other.spanSizeLookupGrid)
    }

    override fun hashCode(): Int {
        return spanSizeLookupGrid?.contentDeepHashCode() ?: 0
    }
}