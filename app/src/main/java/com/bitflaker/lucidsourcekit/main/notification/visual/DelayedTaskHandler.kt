package com.bitflaker.lucidsourcekit.main.notification.visual

import android.os.Handler
import android.os.Looper

class DelayedTaskHandler(private val runnable: Runnable, private val delay: Long) {
    private val handler = Handler(Looper.getMainLooper())
    private var isPaused = false
    private var remainingTime = delay
    private var startTime = 0L

    fun start() {
        startTime = System.currentTimeMillis()
        handler.postDelayed(runnable, remainingTime)
    }

    fun pause() {
        if (!isPaused) {
            handler.removeCallbacks(runnable)
            remainingTime -= (System.currentTimeMillis() - startTime)
            isPaused = true
        }
    }

    fun resume() {
        if (isPaused) {
            startTime = System.currentTimeMillis()
            handler.postDelayed(runnable, remainingTime)
            isPaused = false
        }
    }

    fun cancel() {
        handler.removeCallbacks(runnable)
        isPaused = false
        remainingTime = delay
    }
}
