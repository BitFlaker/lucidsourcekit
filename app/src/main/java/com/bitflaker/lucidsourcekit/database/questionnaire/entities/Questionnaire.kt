package com.bitflaker.lucidsourcekit.database.questionnaire.entities

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity
data class Questionnaire(
    @PrimaryKey(autoGenerate = true) val id: Int,
    var title: String,
    var description: String?,
    var orderNr: Int,
    var colorCode: String?,
    var isHidden: Boolean,
    var isCompact: Boolean
) {
    @Ignore
    constructor(title: String, description: String?, colorCode: String?, isCompact: Boolean) : this(0, title, description, -1, colorCode, false, isCompact)

    @Ignore
    constructor() : this(0, "", null, -1, null, false, false)
}
