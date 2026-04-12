package com.bitflaker.lucidsourcekit.utils.export

import java.io.OutputStream

data class ExportConfiguration(
    val exportFromTS: Long,
    val exportToTS: Long,
    val outputStream: OutputStream
)