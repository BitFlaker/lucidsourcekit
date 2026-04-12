package com.bitflaker.lucidsourcekit.main.dreamjournal.rating

enum class DreamTypes(val id: String) {
    Nightmare("NTM"),
    SleepParalysis("SPL"),
    FalseAwakening("FAW"),
    Lucid("LCD"),
    Recurring("REC"),
    None("");

    companion object {
        @JvmStatic
        fun getEnum(id: String) = DreamTypes.entries.find {
            it.id.equals(id, ignoreCase = true)
        }
    }
}
