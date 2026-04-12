package com.bitflaker.lucidsourcekit.utils.pdf

data class PageDefinition(
    val layoutIndex: Int,
    val offset: Int,
    val pageOffset: Int,
    val height: Int,
    val layoutPageNumber: Int
)