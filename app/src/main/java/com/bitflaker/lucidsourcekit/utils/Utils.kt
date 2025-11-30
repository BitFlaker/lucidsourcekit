package com.bitflaker.lucidsourcekit.utils

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.VIBRATOR_MANAGER_SERVICE
import android.content.Context.VIBRATOR_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.content.res.Resources
import android.content.res.Resources.getSystem
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.LocaleList
import android.os.Parcelable
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.InputType
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import android.window.OnBackInvokedDispatcher
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.bitflaker.lucidsourcekit.data.datastore.DataStoreKeys
import com.bitflaker.lucidsourcekit.data.datastore.getSetting
import com.google.android.material.materialswitch.MaterialSwitch
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <T : Any> Single<T>.await(): T {
    return suspendCancellableCoroutine { cont ->
        val disposable = subscribe({ cont.resume(it) }, { cont.resumeWithException(it) })
        cont.invokeOnCancellation { disposable.dispose() }
    }
}

fun ComponentActivity.onBackPressed(action: (() -> Unit)) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT) { action() }
    } else {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                action()
            }
        })
    }
}

fun BroadcastReceiver.goAsync(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.(BroadcastReceiver.PendingResult) -> Unit
) {
    val pendingResult = goAsync()
    @OptIn(DelicateCoroutinesApi::class)
    GlobalScope.launch(context) {
        try {
            block(pendingResult)
        } finally {
            pendingResult.finish()
        }
    }
}

fun Context.isPermissionGranted(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

inline fun<reified T : Parcelable> Intent.getParcelableExtraSafe(name: String, clazz: Class<T>): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(name, clazz)
    }
    else {
        @Suppress("DEPRECATION")
        getParcelableExtra<T>(name)
    }
}

fun Context.getDefaultVibrator(): Vibrator {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    }
    else {
        @Suppress("DEPRECATION")
        getSystemService(VIBRATOR_SERVICE) as Vibrator
    }
}

fun Vibrator.vibrateFor(milliseconds: Long) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrate(milliseconds)
    }
}

fun createTimerAction(delay: Long, period: Long, task: (() -> Unit)): Timer {
    return Timer().apply {
        schedule(object : TimerTask() {
            override fun run() {
                task()
            }
        }, delay, period)
    }
}

fun Context.resolveDrawable(drawable: Int): Drawable? {
    return ResourcesCompat.getDrawable(resources, drawable, theme)
}

val Int.pxToDp: Int get() = (this / getSystem().displayMetrics.density).toInt()

val Int.dpToPx: Int get() = (this * getSystem().displayMetrics.density).toInt()

val Double.dpToPx: Double get() = this * getSystem().displayMetrics.density

val Int.spToPx: Int get() = toFloat().spToPx

val Float.spToPx: Int get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, this, getSystem().displayMetrics).toInt()


fun Context.attrColorStateList(colorAttr: Int): ColorStateList {
    return ColorStateList.valueOf(attrColor(colorAttr))
}

fun Context.attrColor(colorAttr: Int): Int {
    return typedValue(colorAttr).data
}

fun Context.typedValue(attr: Int): TypedValue {
    val typedValue = TypedValue()
    theme.resolveAttribute(attr, typedValue, true)
    return typedValue
}

suspend fun Activity.loadLanguage() {
    val lang = getSetting(DataStoreKeys.LANGUAGE)
    val locale = Locale(lang)
    Locale.setDefault(locale)
    val config = Configuration()
    config.setLocales(LocaleList(locale))
    baseContext.resources.updateConfiguration(config, baseContext.resources.displayMetrics)
}

fun EditText.singleLine() {
    setInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
    setMaxLines(6)
    setHorizontallyScrolling(false)
    setImeOptions(EditorInfo.IME_ACTION_DONE)
}

fun generateFileName(name: String, extension: String): String {
    val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val date = dateFormat.format(Calendar.getInstance().timeInMillis)
    return "${name}_${date}.$extension"
}

fun showToastLong(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}

class Ref<T>(var value: T)