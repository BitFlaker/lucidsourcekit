package com.bitflaker.lucidsourcekit.utils

interface BackupTaskCallback {
    fun onCompleted()
    fun onError(cause: Throwable)
    fun onProgress(fileName: String, finished: Int, total: Int)
}