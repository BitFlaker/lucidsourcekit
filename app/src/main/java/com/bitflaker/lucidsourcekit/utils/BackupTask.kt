package com.bitflaker.lucidsourcekit.utils

import android.content.Context
import android.net.Uri
import com.bitflaker.lucidsourcekit.data.datastore.DataStoreManager
import com.bitflaker.lucidsourcekit.database.MainDatabase
import com.bitflaker.lucidsourcekit.utils.zip.ZipUtil
import com.bitflaker.lucidsourcekit.utils.zip.builder.ZipStructureBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.io.StreamCorruptedException

class BackupTask private constructor(
    private val context: Context,
    private val uri: Uri,
    private val callback: BackupTaskCallback
) {
    companion object {

        /**
         * Starts a backup process and writes the backup data zip file to [uri].
         * The [callback] informs about the current progress of the backup process
         */
        fun startBackup(context: Context, uri: Uri, callback: BackupTaskCallback) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    BackupTask(context, uri, callback).backupDatabase()
                    callback.onCompleted()
                }
                catch (ex: Throwable) {
                    callback.onError(ex)
                }
            }
        }

        /**
         * Starts a backup restore process for the backup data zip file at [uri].
         * The [callback] informs about the current progress of the backup restore process
         */
        fun startRestore(context: Context, uri: Uri, callback: BackupTaskCallback) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    BackupTask(context, uri, callback).restoreDatabase()
                    callback.onCompleted()
                }
                catch (ex: Throwable) {
                    callback.onError(ex)
                }
            }
        }

        /**
         * Deletes the directory containing the application data backed up before
         * the import process was performed, to prevent data loss in case of a failed import.
         * This function must only be called after ensuring the import succeeded
         */
        fun deleteOldBeforeImportBackup(context: Context) {
            val backupTempOldDirectory = File(context.cacheDir, "tmpBackupOld/")
            if (backupTempOldDirectory.exists()) {
                backupTempOldDirectory.deleteRecursively()
            }
        }
    }

    // RESTORE DIRECTORIES
    private val backupTempImportDirectory = File(context.cacheDir, "tmpBackupImport/")
    private val backupTempOldDirectory = File(context.cacheDir, "tmpBackupOld/")

    // DEFAULT DATA DIRECTORIES
    private val dataDBFile = context.getDatabasePath(MainDatabase.MAIN_DATABASE_NAME)
    private val dataDBWal = File(dataDBFile.path + "-wal")
    private val dataDBShm = File(dataDBFile.path + "-shm")
    private val dataDataStore = File(context.filesDir, "datastore" + File.separator + DataStoreManager.DATA_STORE_FILE_NAME + ".preferences_pb")
    private val dataRecordings = File(context.filesDir, "Recordings")

    // IMPORTED FILES
    private val impDBFile = backupTempImportDirectory.resolve("database" + File.separator + MainDatabase.MAIN_DATABASE_NAME)
    private val impDBWal = File(impDBFile.path + "-wal")
    private val impDBShm = File(impDBFile.path + "-shm")
    private val impDataStore = File(backupTempImportDirectory, DataStoreManager.DATA_STORE_FILE_NAME + ".preferences_pb")
    private val impRecordings = File(backupTempImportDirectory, "Recordings")
    private val impPreferenceEntry = File(backupTempImportDirectory, "preferences.json")

    /**
     * Creates a zip file which contains the database, all recordings,
     * the DataStore and the (now deprecated) shared preferences
     */
    private suspend fun backupDatabase() = coroutineScope {
        val outputStream = context.contentResolver.openOutputStream(uri) ?: throw StreamCorruptedException("Failed to open export stream")

        val preferenceEntry = MainDatabase.exportSharedPreferences(context)

        val zipStructure = ZipStructureBuilder()
            .addDirectoryContent(dataRecordings)
            .addFile(dataDBFile, "database")
            .addFile(dataDBWal, "database")
            .addFile(dataDBShm, "database")
            .addFile(dataDataStore)
            .addFileData("preferences.json", preferenceEntry)
            .build()

        ZipUtil()
            .setCallback(callback)
            .createZipFile(zipStructure, outputStream)

        outputStream.close()
    }

    /**
     * Restores the database, recordings and the DataStore from a
     * previously created backup zip file. The deprecated shared preferences
     * will not be imported and will simply be ignored.
     *
     * The current application data is moved to a temporary directory in
     * the cache before the import to prevent data loss
     */
    private suspend fun restoreDatabase() = coroutineScope {
        val inputStream = context.contentResolver.openInputStream(uri) ?: throw StreamCorruptedException("Failed to open export stream")

        ZipUtil()
            .setCallback(callback)
            .unzipFile(inputStream, backupTempImportDirectory)

        inputStream.close()

        backupCurrentData()
        moveNewData()
    }

    /**
     * Moves the data extracted from the imported zip file into the current
     * application data locations to restore the database, recordings and
     * datastore from the backup
     */
    private fun moveNewData() {
        dataRecordings.deleteRecursively()
        moveFile(impDBFile, dataDBFile)
        moveFile(impDBWal, dataDBWal)
        moveFile(impDBShm, dataDBShm)
        moveFile(impDataStore, dataDataStore)
        moveFile(impRecordings, dataRecordings)
        backupTempImportDirectory.deleteRecursively()
    }

    /**
     * Moves all current application data to a temporary directory in the
     * cache directory to prevent data loss
     */
    private fun backupCurrentData() {
        if (backupTempOldDirectory.exists() && !backupTempOldDirectory.deleteRecursively()) {
            throw IOException("Error deleting temp old backup directory")
        }
        backupTempOldDirectory.mkdirs()
        val dbTempOldDirectory = backupTempOldDirectory.resolve("database/")
        dbTempOldDirectory.mkdir()
        moveFile(dataDBFile, dbTempOldDirectory)
        moveFile(dataDBWal, dbTempOldDirectory)
        moveFile(dataDBShm, dbTempOldDirectory)
        moveFile(dataDataStore, backupTempOldDirectory)
        moveFile(dataRecordings, backupTempOldDirectory)
    }

    /**
     * Moves a source [file] to the [destination] directory.
     * In case [destination] is a file, the source [file] will
     * be moved to the [destination] path
     */
    private fun moveFile(file: File, destination: File) {
        if (!file.exists()) return
        val target = if (destination.isDirectory) destination.resolve(file.name) else destination
        file.let { sourceFile ->
            sourceFile.copyRecursively(target)
            sourceFile.delete()
        }
    }
}