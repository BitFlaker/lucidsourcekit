package com.bitflaker.lucidsourcekit.utils.zip.builder

import java.io.File

data class ZipEntryFileBuilder internal constructor(val file: File) : ZipEntryBuilder() {
    override val name: String = file.name

    init {
        if (!file.isFile) throw IllegalArgumentException("Provided parameter 'file' is not a file")
    }
}