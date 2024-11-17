package com.bitflaker.lucidsourcekit.utils.zip

import java.io.File

data class ZipEntryFile internal constructor(override val path: String, val file: File) : ZipEntry() {
    override val name: String = file.name
}