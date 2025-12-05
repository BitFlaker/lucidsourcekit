package com.bitflaker.lucidsourcekit.database.goals.entities

import android.content.Context
import androidx.annotation.Nullable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.bitflaker.lucidsourcekit.R

/**
 * Default difficulty values for default goals defined in the `goals` string array element
 * in the strings.xml file. The order of items in the string array maps to these difficulties
 */
val DEFAULT_GOAL_DIFFICULTIES: FloatArray = floatArrayOf(
    1.6f,
    3f,
    1.2f,
    1.8f,
    1.3f,
    1f,
    2f,
    1.5f
)

@Entity
class Goal {
    @PrimaryKey(autoGenerate = true)
    var goalId: Int = 0
    var description: String = ""
    var difficulty: Float = 0f
    @ColumnInfo(defaultValue = "0")
    var difficultyLocked: Boolean = false

    @Ignore
    var isSelected: Boolean = false

    constructor(goalId: Int, description: String, difficulty: Float, difficultyLocked: Boolean) {
        this.goalId = goalId
        this.description = description
        this.difficulty = difficulty
        this.difficultyLocked = difficultyLocked
    }

    @Ignore
    constructor(description: String, difficulty: Float) {
        this.description = description
        this.difficulty = difficulty
    }

    @Ignore
    constructor()

    override fun equals(other: Any?): Boolean {
        return other is Goal &&
            other.goalId == goalId &&
            other.description == description &&
            other.isSelected == isSelected &&
            other.difficulty == difficulty &&
            other.difficultyLocked == difficultyLocked
    }

    override fun hashCode(): Int {
        var result = goalId
        result = 31 * result + difficulty.hashCode()
        result = 31 * result + difficultyLocked.hashCode()
        result = 31 * result + isSelected.hashCode()
        result = 31 * result + description.hashCode()
        return result
    }

    companion object {
        @Ignore
        fun defaultData(context: Context): List<Goal> {
            return context.resources.getStringArray(R.array.goals)
                .mapIndexed { i, description -> Goal(description, DEFAULT_GOAL_DIFFICULTIES[i]) }
                .toList()
        }
    }
}
