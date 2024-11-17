package com.bitflaker.lucidsourcekit.utils.zip.builder

internal data class ZipEntryDataBuilder(override val name: String, val data: ByteArray) : ZipEntryBuilder() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ZipEntryDataBuilder

        if (name != other.name) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}
