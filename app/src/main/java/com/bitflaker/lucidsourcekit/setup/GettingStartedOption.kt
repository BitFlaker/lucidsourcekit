package com.bitflaker.lucidsourcekit.setup

data class GettingStartedOption(
    val icon: Int,
    val title: String,
    val description: String,
    var alreadyCompleted: Boolean,
    val action: (() -> Unit)
)