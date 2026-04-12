package com.bitflaker.lucidsourcekit.main.dreamjournal.rating

enum class DreamMoods(val id: String) {
    Terrible("TRB"),
    Poor("POR"),
    Ok("OKY"),
    Great("GRT"),
    Outstanding("OSD"),
    None("");

    companion object {
        @JvmStatic
        fun getEnum(id: String) = DreamMoods.entries.find {
            it.id.equals(id, ignoreCase = true)
        }
    }
}
