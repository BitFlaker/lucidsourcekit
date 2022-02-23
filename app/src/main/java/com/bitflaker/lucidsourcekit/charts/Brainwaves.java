package com.bitflaker.lucidsourcekit.charts;

import android.graphics.Color;

public class Brainwaves {
    /**
     * The color codings for a gradient with sleep frequencies
     * @return an array of colors for frequency
     */
    public static int[] getStageColors() {
        return new int[] {
                Color.rgb(59, 144, 232), // beta = awake
                Color.rgb(90, 95, 223), // alpha = relaxed
                Color.rgb(121, 73, 207), // theta = dreams
                Color.rgb(116, 50, 159) // delta = deep sleep
        };
    }

    /**
     * The center frequency of a stage beginning with beta and ending with delta.
     * e.g. beta (32Hz-13Hz) => 22.5Hz avg.
     * @return an array of average frequencies in each stage
     */
    public static double[] getStageFrequencyCenters() {
        return new double[] {
                22.5, // beta = 32Hz - 13Hz
                10.5, // alpha = 13Hz - 8Hz
                6, // theta = 8Hz - 4Hz
                2.25 // delta = 4Hz - 0.5Hz
        };
    }
}
