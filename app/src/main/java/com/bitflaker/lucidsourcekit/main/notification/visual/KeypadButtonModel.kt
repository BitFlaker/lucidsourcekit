package com.bitflaker.lucidsourcekit.main.notification.visual

import android.graphics.drawable.Drawable

data class KeypadButtonModel @JvmOverloads constructor(
    val buttonValue: Char?,
    var buttonIcon: Drawable? = null
)
