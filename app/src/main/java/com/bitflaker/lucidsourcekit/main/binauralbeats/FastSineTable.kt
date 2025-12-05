package com.bitflaker.lucidsourcekit.main.binauralbeats

import kotlin.math.sin

class FastSineTable
private constructor(private val sampleRate: Int) {
    companion object {
        val table: FastSineTable = FastSineTable(44100)
    }
    
    private val sineTable = generateSineTable()

    private fun generateSineTable(): FloatArray {
        val sineTable = FloatArray(this.sampleRate)
        val stepSize = (2.0 * Math.PI / this.sampleRate).toFloat()
        for (i in 0..<this.sampleRate) {
            sineTable[i] = sin((stepSize * (i.toFloat())).toDouble()).toFloat()
        }
        return sineTable
    }

    fun sineBySampleRateDeg(angle: Long): Float {
        return sineTable[(angle % sampleRate).toInt()]
    }
}