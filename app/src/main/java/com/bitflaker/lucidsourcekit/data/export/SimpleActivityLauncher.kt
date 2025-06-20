package com.bitflaker.lucidsourcekit.data.export

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

class SimpleActivityLauncher(activity: ComponentActivity, launcherCount: Int) {
    private var callbacks: MutableList<((ActivityResult) -> Unit)?> = MutableList(launcherCount) { null }
    private val launchers: List<ActivityResultLauncher<Intent>> = List(launcherCount) { idx ->
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            callbacks[idx]?.invoke(result)
            callbacks[idx] = null
        }
    }

    fun launch(intent: Intent, resultCallback: (ActivityResult) -> Unit) {
        val index = callbacks.indexOfFirst { it == null }
        callbacks[index] = resultCallback
        launchers[index].launch(intent)
    }
}