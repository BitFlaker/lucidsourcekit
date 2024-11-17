package com.bitflaker.lucidsourcekit.utils.zip

import com.bitflaker.lucidsourcekit.utils.zip.builder.ZipEntryBuilder

internal class ZipStructure(val name: String) : Iterator<ZipEntryBuilder> {
    private val subStructures: MutableList<ZipStructure> = mutableListOf()
    private val entries: MutableList<ZipEntryBuilder> = mutableListOf()
    private var position: Int = 0

    fun create(pathParts: Array<String>?): ZipStructure {
        if (pathParts.isNullOrEmpty()) return this
        val currentPathName = pathParts[0]
        for (subStructure in subStructures) {
            if (subStructure.name == currentPathName) {
                return subStructure.create(skipFirstArrayItem(pathParts))
            }
        }
        val newEntry = ZipStructure(currentPathName)
        subStructures.add(newEntry)
        return newEntry.create(skipFirstArrayItem(pathParts))
    }

    private fun skipFirstArrayItem(array: Array<String>?): Array<String>? {
        if (array == null || array.size == 1) return null
        return array.copyOfRange(1, array.size - 1)
    }

    fun addEntry(entry: ZipEntryBuilder) {
        entries.add(entry)
    }

    override fun hasNext(): Boolean {
        if (position < subStructures.size + entries.size) {
            return true
        }
        else if (position == subStructures.size - 1 && entries.isEmpty()) {
            return subStructures[position].hasNext()
        }
        return false
    }

    override fun next(): ZipEntryBuilder {
        if (position < subStructures.size && (subStructures[position].hasNext() || ++position < subStructures.size)) {
            return subStructures[position].next().addParentDirectory(name)
        }
        return getNextEntry().addParentDirectory(name)
    }

    private fun getNextEntry(): ZipEntryBuilder {
        if (position >= subStructures.size + entries.size) throw IllegalStateException("Impossible iterator position")
        val entryIndex = position++ - subStructures.size
        return if (entryIndex < entries.size) entries[entryIndex] else throw IllegalStateException("Impossible iterator position")
    }
}