package com.bitflaker.lucidsourcekit.utils.zip

import android.os.Build
import com.bitflaker.lucidsourcekit.utils.BackupTaskCallback
import dalvik.system.ZipPathValidator
import kotlinx.coroutines.coroutineScope
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private const val BUFFER_SIZE = 1024

class ZipUtil {
    companion object {
        private fun addFileToZip(out: ZipOutputStream, entry: ZipEntryFile) {
            FileInputStream(entry.file).use { fileStream ->
                BufferedInputStream(fileStream).use { bufferedStream ->
                    out.putNextEntry(ZipEntry(entry.path))
                    bufferedStream.copyTo(out, BUFFER_SIZE)
                    out.closeEntry()
                }
            }
        }

        private fun addDataToZip(out: ZipOutputStream, entry: ZipEntryData) {
            ByteArrayInputStream(entry.data).use { byteStream ->
                out.putNextEntry(ZipEntry(entry.path))
                byteStream.copyTo(out, BUFFER_SIZE)
                out.closeEntry()
            }
        }
    }

    private var callback: BackupTaskCallback? = null

    fun setCallback(callback: BackupTaskCallback?): ZipUtil {
        this.callback = callback
        return this
    }

    suspend fun createZipFile(entries: Array<com.bitflaker.lucidsourcekit.utils.zip.ZipEntry>, outputStream: OutputStream) = coroutineScope {
        var filesFinished = 0
        ZipOutputStream(BufferedOutputStream(outputStream)).use { out ->
            for (entry in entries) {
                callback?.onProgress(entry.name, filesFinished, entries.size)
                when (entry) {
                    is ZipEntryData -> addDataToZip(out, entry)
                    is ZipEntryFile -> addFileToZip(out, entry)
                }
                callback?.onProgress(entry.name, ++filesFinished, entries.size)
            }
        }
    }

    fun unzipFile(inputStream: InputStream, outputDirectory: File) {
        checkAndPrepareUnzip(outputDirectory)
        ZipInputStream(inputStream).use { input ->
            while (true) {
                val entry = input.getNextEntry() ?: break
                val entryOutput = outputDirectory.resolve(entry.name)
                callback?.onProgress(entryOutput.name, -1, -1)

                ensureDirectoryCreated(entryOutput)
                if (!entry.isDirectory) {
                    BufferedOutputStream(FileOutputStream(entryOutput, false)).use { out ->
                        input.copyTo(out, BUFFER_SIZE)
                        input.closeEntry()
                    }
                }
            }
        }
    }

    private fun checkAndPrepareUnzip(outputDirectory: File) {
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs()
        }
        if (!outputDirectory.isDirectory) {
            throw IllegalArgumentException("Provided output path is not a directory")
        }
        if (outputDirectory.listFiles()?.isNotEmpty() == true) {
            throw IllegalArgumentException("Provided output path is not empty")
        }
        if (Build.VERSION.SDK_INT >= 34) {  // TODO: Remove! This is only here to allow for old backups to be able to be restored
            ZipPathValidator.clearCallback()
        }
    }

    private fun extractDirectory(entryOutput: File) {
        if (!entryOutput.exists() && !entryOutput.mkdirs()) {
            throw IOException("Failed to extract directory \"" + entryOutput.absolutePath + "\"")
        }
    }

    private fun ensureDirectoryCreated(entryOutput: File) {
        val directory = if (entryOutput.isDirectory) entryOutput else entryOutput.parentFile ?: return
        extractDirectory(directory)
    }
}