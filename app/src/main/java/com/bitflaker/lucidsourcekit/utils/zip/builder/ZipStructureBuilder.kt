package com.bitflaker.lucidsourcekit.utils.zip.builder

import com.bitflaker.lucidsourcekit.utils.zip.ZipEntry
import com.bitflaker.lucidsourcekit.utils.zip.ZipEntryData
import com.bitflaker.lucidsourcekit.utils.zip.ZipEntryFile
import com.bitflaker.lucidsourcekit.utils.zip.ZipStructure
import java.io.File

class ZipStructureBuilder {
    private val structure: ZipStructure = ZipStructure("/")

    fun addDirectoryContent(file: File, vararg path: String): ZipStructureBuilder {
        if (!file.isDirectory || !file.exists()) throw IllegalArgumentException("Provided parameter 'file' has to be an existing directory")
        val pathParts = (getCheckedPathParts(path) ?: arrayOf()) + file.name
        val currentPath = pathParts.joinToString(File.separator)
        val content = file.listFiles()
        for (entry in content!!) {
            if (entry.isDirectory) {
                addDirectoryContent(entry, currentPath)
            }
            else if (entry.isFile) {
                addFile(entry, currentPath)
            }
        }
        return this
    }

    fun addFile(file: File, vararg path: String): ZipStructureBuilder {
        if (!file.isFile || !file.exists()) throw IllegalArgumentException("Provided parameter 'file' has to be an existing file")
        getZipStructure(path).addEntry(ZipEntryFileBuilder(file))
        return this
    }

    fun addFileData(fileName: String, data: ByteArray, vararg path: String): ZipStructureBuilder {
        getZipStructure(path).addEntry(ZipEntryDataBuilder(fileName, data))
        return this
    }

    fun build(): Array<ZipEntry> {
        val entries = mutableListOf<ZipEntry>()
        for (entry in structure) {
            when (entry) {
                is ZipEntryDataBuilder -> entries.add(ZipEntryData(entry.getPath(), entry.name, entry.data))
                is ZipEntryFileBuilder -> entries.add(ZipEntryFile(entry.getPath(), entry.file))
            }
        }
        return entries.toTypedArray()
    }

    private fun getZipStructure(path: Array<out String>): ZipStructure {
        val pathParts = getCheckedPathParts(path)
        return structure.create(pathParts)
    }

    private fun getCheckedPathParts(zipPath: Array<out String>): Array<String>? {
        val filtered = zipPath.filter {
            it.isNotBlank()
        }
        if (filtered.isEmpty()) {
            return null
        }
        return filtered.joinToString(File.separator)
            .split(File.separator)
            .filter { it.isNotBlank() }
            .toTypedArray()
    }
}