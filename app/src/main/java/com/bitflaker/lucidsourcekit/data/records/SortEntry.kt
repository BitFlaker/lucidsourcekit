package com.bitflaker.lucidsourcekit.data.records

import com.bitflaker.lucidsourcekit.data.enums.SortBy

data class SortEntry(val sortText: String, val sortBy: SortBy, val isDescending: Boolean)
