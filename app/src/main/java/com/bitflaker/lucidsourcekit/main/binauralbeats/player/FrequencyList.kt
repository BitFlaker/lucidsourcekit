package com.bitflaker.lucidsourcekit.main.binauralbeats.player

import kotlin.math.ceil

class FrequencyList {
    private val frequencyData = ArrayList<FrequencyData>()

    operator fun get(i: Int): FrequencyData {
        return frequencyData[i]
    }

    fun add(data: FrequencyData) {
        val maxDuration = 2000
        if (data.duration <= maxDuration) {
            frequencyData.add(data)
        } else {
            val from = data.frequency
            val to = if (data.getFrequencyTo().isNaN()) from else data.getFrequencyTo()
            val diff = to - from
            val count = ceil((data.duration / maxDuration.toFloat()).toDouble()).toInt()
            val step = diff / count
            val durInc = data.duration / count.toFloat()
            var currFreq = data.frequency
            for (i in 0..<count) {
                frequencyData.add(FrequencyData(currFreq, currFreq + step, durInc))
                currFreq += step
            }
        }
    }

    fun remove(i: Int) {
        frequencyData.removeAt(i)
    }

    fun remove(data: FrequencyData) {
        frequencyData.remove(data)
    }

    val duration: Float
        get() = frequencyData.stream().mapToDouble { it.duration.toDouble() }.sum().toFloat()

    fun size(): Int {
        return frequencyData.size
    }

    fun getDurationUntilAfter(i: Int): Float {
        return getDurationUntil(i) + frequencyData[i].duration
    }

    fun getDurationUntil(i: Int): Float {
        var duration = 0.0f
        for (j in 0..<i) {
            duration += frequencyData[j].duration
        }
        return duration
    }

    fun getFrequencyAtDuration(duration: Double): Double {
        var durationCounter = 0.0
        for (i in frequencyData.indices) {
            val current = frequencyData[i]
            durationCounter += current.duration.toDouble()
            if (durationCounter >= duration) {
                if (current.getFrequencyTo().isNaN()) {
                    return current.frequency.toDouble()
                }
                val k = (current.getFrequencyTo() - current.frequency) / current.duration.toDouble()
                return k * (duration - (durationCounter - current.duration)) + current.frequency
            }
        }
        return -1.0
    }
}
