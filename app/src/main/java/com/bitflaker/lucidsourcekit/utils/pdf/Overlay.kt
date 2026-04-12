package com.bitflaker.lucidsourcekit.utils.pdf

import android.view.View

data class Overlay(val view: View, val displayCondition: (PageContext) -> Boolean)