package com.bitflaker.lucidsourcekit.database.questionnaire.entities

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity
data class Questionnaire(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val title: String,
    val description: String?,
    val isHidden: Boolean,
    val isCompact: Boolean
) {
    @Ignore
    constructor(title: String, description: String?, isCompact: Boolean) : this(0, title, description, false, isCompact)
}
