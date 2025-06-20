package com.bitflaker.lucidsourcekit.utils

import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <T : Any> Single<T>.await(): T {
    return suspendCancellableCoroutine { cont ->
        val disposable = subscribe({ cont.resume(it) }, { cont.resumeWithException(it) })
        cont.invokeOnCancellation { disposable.dispose() }
    }
}

fun generateFileName(name: String, extension: String): String {
    val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val date = dateFormat.format(Calendar.getInstance().timeInMillis)
    return "${name}_${date}.$extension"
}