package com.bitflaker.lucidsourcekit.utils

import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener

fun View.fadeIn(fadeDuration: Long = 400) {
    alpha = 1f
    visibility = View.VISIBLE
    this.startAnimation(AlphaAnimation(0f, 1f).apply {
        duration = fadeDuration
        fillAfter = true
    })
}

fun View.fadeOut(fadeDuration: Long = 400) {
    val view = this
    this.startAnimation(AlphaAnimation(1f, 0f).apply {
        duration = fadeDuration
        fillAfter = true
        setAnimationListener(object : AnimationListener {
            override fun onAnimationRepeat(animation: Animation?) { }
            override fun onAnimationStart(animation: Animation?) { }
            override fun onAnimationEnd(animation: Animation?) {
                view.visibility = View.GONE
            }
        })
    })
}

