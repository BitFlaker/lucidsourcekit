package com.bitflaker.lucidsourcekit.data

import kotlin.Int

class FrequencyData {
    val frequency: Float
    val duration: Float
    private val frequencyTo: Float

    constructor(frequency: Float, duration: Float) {
        this.frequency = frequency
        this.frequencyTo = Float.NaN
        this.duration = duration
    }

    constructor(frequencyFrom: Float, frequencyTo: Float, duration: Float) {
        this.frequency = frequencyFrom
        this.frequencyTo = frequencyTo
        this.duration = duration
    }

    fun getFrequencyTo(): Float {
        return if (frequencyTo.isNaN()) frequency else frequencyTo
    }

    fun getFrequencyStepSize(sampleRate: Int): Float {
        return (getFrequencyTo() - frequency) / (duration * sampleRate)
    }
}
