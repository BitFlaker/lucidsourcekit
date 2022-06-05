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

    /**
     * The lowest frequency of a stage beginning with beta and ending with delta.
     * e.g. beta (32Hz-13Hz) => 13Hz
     * @return an array of lowest frequencies in each stage
     */
    public static double[] getStageFrequencyLows() {
        return new double[] {
                13, // beta = 32Hz - 13Hz
                8, // alpha = 13Hz - 8Hz
                4, // theta = 8Hz - 4Hz
                0.5 // delta = 4Hz - 0.5Hz
        };
    }

    /**
     * The greek letter representing the frequency stage
     * e.g. beta (32Hz-13Hz) => β
     * @return an array of the greek letter for frequencies in each stage
     */
    public static String[] getStageFrequencyGreekLetters() {
        return new String[] {
                "β", // beta = 32Hz - 13Hz
                "α", // alpha = 13Hz - 8Hz
                "θ", // theta = 8Hz - 4Hz
                "δ" // delta = 4Hz - 0.5Hz
        };
    }

    /**
     * The greek letter's name representing the frequency stage
     * e.g. beta (32Hz-13Hz) => Beta
     * @return an array of greek letter's name representing frequencies in each stage
     */
    public static String[] getStageFrequencyGreekLetterNames() { // TODO make use of String resources file !
        return new String[] {
                "Beta", // beta = 32Hz - 13Hz
                "Alpha", // alpha = 13Hz - 8Hz
                "Theta", // theta = 8Hz - 4Hz
                "Delta" // delta = 4Hz - 0.5Hz
        };
    }

    public static int getStageIndex(double frequency) {
        double[] stages = getStageFrequencyLows();
        for (int i = 0; i < stages.length; i++) {
            if(frequency >= stages[i]) {
                return i;
            }
        }
        return stages.length - 1;
    }
}
