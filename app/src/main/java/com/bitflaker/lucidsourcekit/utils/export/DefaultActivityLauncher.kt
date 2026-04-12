package com.bitflaker.lucidsourcekit.utils.export

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

class DefaultActivityLauncher(activity: ComponentActivity) {
    private var callback: ((ActivityResult) -> Unit)? = null
    private val launcher: ActivityResultLauncher<Intent> = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        callback?.invoke(result)
        callback = null
    }

    fun launch(intent: Intent, resultCallback: (ActivityResult) -> Unit) {
        callback = resultCallback
        launcher.launch(intent)
    }
}