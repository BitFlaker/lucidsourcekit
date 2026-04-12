package com.bitflaker.lucidsourcekit.utils.pdf

data class ChildOffset @JvmOverloads constructor(
    var index: Int = 0,
    var heightOffset: Int = 0,
    var inner: ChildOffset? = null
)