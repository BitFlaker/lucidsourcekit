package com.bitflaker.lucidsourcekit.main.binauralbeats

import android.graphics.Color
import android.util.Pair

class Brainwaves {
    companion object {
        var remAfterMinutes = intArrayOf(103, 81, 88, 72, 77)
        var remDuration = intArrayOf(7, 23, 33, 36, 40)
        var remDurationTolerance = 15

        /**
         * The color codings for a gradient with sleep frequencies
         * @return an array of colors for frequency
         */
        val stageColors = intArrayOf(
            Color.rgb(59, 144, 232),    // beta = awake
            Color.rgb(90, 95, 223),     // alpha = relaxed
            Color.rgb(121, 73, 207),    // theta = dreams
            Color.rgb(116, 50, 159)     // delta = deep sleep
        )

        /**
         * The center frequency of a stage beginning with beta and ending with delta.
         * e.g. beta (32Hz-13Hz) => 22.5Hz avg.
         * @return an array of average frequencies in each stage
         */
        val stageFrequencyCenters = doubleArrayOf(
            22.5,   // beta = 32Hz - 13Hz
            10.5,   // alpha = 13Hz - 8Hz
            6.0,    // theta = 8Hz - 4Hz
            2.25    // delta = 4Hz - 0.5Hz
        )

        /**
         * The lowest frequency of a stage beginning with beta and ending with delta.
         * e.g. beta (32Hz-13Hz) => 13Hz
         * @return an array of lowest frequencies in each stage
         */
        val stageFrequencyLows = doubleArrayOf(
            13.0,   // beta = 32Hz - 13Hz
            8.0,    // alpha = 13Hz - 8Hz
            4.0,    // theta = 8Hz - 4Hz
            0.5     // delta = 4Hz - 0.5Hz
        )

        /**
         * The greek letter representing the frequency stage
         * e.g. beta (32Hz-13Hz) => β
         * @return an array of the greek letter for frequencies in each stage
         */
        val stageFrequencyGreekLetters = arrayOf<String>(
            "β",    // beta = 32Hz - 13Hz
            "α",    // alpha = 13Hz - 8Hz
            "θ",    // theta = 8Hz - 4Hz
            "δ"     // delta = 4Hz - 0.5Hz
        )

        /**
         * The greek letter's name representing the frequency stage
         * e.g. beta (32Hz-13Hz) => Beta
         * @return an array of greek letter's name representing frequencies in each stage
         */
        val stageFrequencyGreekLetterNames = arrayOf<String>(
            "Beta",     // beta = 32Hz - 13Hz
            "Alpha",    // alpha = 13Hz - 8Hz
            "Theta",    // theta = 8Hz - 4Hz
            "Delta"     // delta = 4Hz - 0.5Hz
        )

        fun getStageIndex(frequency: Double): Int {
            val stages = stageFrequencyLows
            for (i in stages.indices) {
                if (frequency >= stages[i]) {
                    return i
                }
            }
            return stages.size - 1
        }

        fun getRemStageAfterAndDuration(stageNumber: Int): Pair<Int, Int>? {
            if (remAfterMinutes.size != remDuration.size || remAfterMinutes.size <= stageNumber) {
                return null
            }
            return Pair<Int, Int>(
                sumRemAftersUpTo(stageNumber),
                remDuration[stageNumber] + remDurationTolerance
            )
        }

        private fun sumRemAftersUpTo(stageNumber: Int): Int {
            var totalCount = 0
            for (i in 0..stageNumber) {
                totalCount += remAfterMinutes[i]
            }
            return totalCount
        }
    }
}
