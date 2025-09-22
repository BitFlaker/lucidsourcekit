package com.bitflaker.lucidsourcekit.main.goals

import com.bitflaker.lucidsourcekit.database.goals.entities.Goal
import java.util.Random
import java.util.TreeMap

class RandomGoalPicker {
    private val map = TreeMap<Float, Goal>()
    private val random: Random = Random()
    private var total = 0f

    fun add(weight: Float, goal: Goal) {
        require(weight > 0.0f) { "weight must be greater than zero. Provided value: $weight" }
        total += weight
        map.put(total, goal)
    }

    val randomGoal: Goal?
        get() {
            // Get a random value from 0 .. total and get the goal with the next highest value
            val value = random.nextFloat() * total
            val entry = map.higherEntry(value) ?: return null

            // TODO: Try to find a good and efficient approach for the following issue.
            //       The problem is that by selecting a key somewhere in the middle of the list,
            //       the item will be removed, but the weights are not being adjusted accordingly
            //       therefore the weight of the item following the currently selected one, will be
            //       increased by the current items weight: e.g.:
            //       ... (10; Item1) (12; Item2) (14; Item3) ...
            //       ... (10; Item1) (14; Item3) ...

            // Flawed workaround for weight adjustment to not have issues with exceeding max value of tree map key
            if (total == entry.key) {
                val lower = map.lowerEntry(entry.key)
                if (lower != null) {
                    total = lower.key
                }
            }

            // Remove pair to prevent double selection
            map.remove(entry.key, entry.value)
            return entry.value
        }
}
