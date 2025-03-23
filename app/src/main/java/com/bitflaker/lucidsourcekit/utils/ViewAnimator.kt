package com.bitflaker.lucidsourcekit.utils

import android.view.View
import android.view.animation.AlphaAnimation

fun View.fadeIn(fadeDuration: Long = 400) {
    alpha = 1f
    visibility = View.VISIBLE
    this.startAnimation(AlphaAnimation(0f, 1f).apply {
        duration = fadeDuration
        fillAfter = true
    })
}

fun View.fadeOut(fadeDuration: Long = 400) {
    this.startAnimation(AlphaAnimation(1f, 0f).apply {
        duration = fadeDuration
        fillAfter = true
    })
}

