package com.bitflaker.lucidsourcekit.utils.zip.builder

import java.io.File

abstract class ZipEntryBuilder {
    abstract val name: String

    private val pathParts = mutableListOf<String>()

    fun getPath(): String {
        return (pathParts + name).joinToString(File.separator)
    }

    fun addParentDirectory(name: String): ZipEntryBuilder {
        if (name == "/") return this    // Ignore the root directory
        pathParts.add(name)
        return this
    }
}