package com.bitflaker.lucidsourcekit.main.dreamjournal.rating

enum class SleepQuality(val id: String) {
    Terrible("TRB"),
    Poor("POR"),
    Great("GRT"),
    Outstanding("OSD"),
    None("");

    companion object {
        @JvmStatic
        fun getEnum(id: String) = SleepQuality.entries.find {
            it.id.equals(id, ignoreCase = true)
        }
    }
}
