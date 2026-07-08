package com.bitflaker.lucidsourcekit

import android.app.Activity
import java.io.File
import java.util.UUID

class GlobalExceptionHandler(val app: Activity) : Thread.UncaughtExceptionHandler {
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(t: Thread, e: Throwable) {
        val traceBuilder = StringBuilder(e.toString())

        // Write the full stack trace
        val stackTrace = e.stackTrace
        traceBuilder.append("\n\n----------- Stack trace -----------\n\n")
        for (line in stackTrace) {
            traceBuilder.append("    ").append(line).append("\n")
        }
        traceBuilder.append("-----------------------------------\n\n")

        // If there is a cause, append the cause stack trace
        traceBuilder.append("-------------- Cause --------------\n\n")
        e.cause?.let { cause ->
            traceBuilder.append(cause.toString()).append("\n\n")
            val stackTrace = cause.stackTrace
            for (line in stackTrace) {
                traceBuilder.append("    ").append(line).append("\n")
            }
        }
        traceBuilder.append("-----------------------------------\n\n")

        // Try to write the built trace log to a file
        try {
            val dir = File(app.cacheDir, "crashes").apply { mkdirs() }
            File(dir, "unhandled-${UUID.randomUUID()}-stacktrace.log").writeText(traceBuilder.toString())
        } catch (_: Throwable) {
            // Ignore to prevent endless loops
        }

        defaultHandler?.uncaughtException(t, e)
    }

    fun collectCrashTraces(crashDataHandler: ((List<String>) -> Unit)): Boolean {
        try {
            // Check if any unhandled crash traces are in the crashes directory
            val dir = File(app.cacheDir, "crashes")
            val files = dir.listFiles { file ->
                file.isFile && file.name.startsWith("unhandled-")
            } ?: return false

            // Try to get the contents of the crash traces and mark them as handled
            val reports = files.mapNotNull { file ->
                try {
                    val content = file.readText()
                    val renamed = File(file.parentFile, "handled-" + file.name.removePrefix("unhandled-"))
                    file.renameTo(renamed)
                    content
                } catch (t: Throwable) {
                    null
                }
            }

            // Forward the crash traces to the handler function
            if (reports.isNotEmpty()) {
                crashDataHandler.invoke(reports)
                return true
            }
        } catch (t: Throwable) {

        }
        return false
    }
}