package com.bitflaker.lucidsourcekit.utils.zip

data class ZipEntryData internal constructor(override val path: String, override val name: String, val data: ByteArray) : ZipEntry() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ZipEntryData

        if (path != other.path) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = path.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}
