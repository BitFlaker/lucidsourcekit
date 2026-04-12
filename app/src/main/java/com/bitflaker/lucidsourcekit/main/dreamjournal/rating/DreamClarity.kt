package com.bitflaker.lucidsourcekit.main.dreamjournal.rating

enum class DreamClarity(val id: String) {
    VeryCloudy("VCL"),
    Cloudy("CLD"),
    Clear("CLR"),
    CrystalClear("CCL"),
    None("");

    companion object {
        @JvmStatic
        fun getEnum(id: String) = DreamClarity.entries.find {
            it.id.equals(id, ignoreCase = true)
        }
    }
}
