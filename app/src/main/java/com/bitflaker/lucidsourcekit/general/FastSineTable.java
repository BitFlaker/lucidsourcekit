package com.bitflaker.lucidsourcekit.general;

import androidx.annotation.NonNull;

public class FastSineTable
{
	private final float[] sineTable;
	private final int sampleRate;

	public FastSineTable(int sampleRate)
	{
		this.sampleRate = sampleRate;
		sineTable = generateSineTable();
	}

	@NonNull
	private float[] generateSineTable() {
		final float[] sineTable = new float[this.sampleRate];
		float stepSize = (float) (2d * Math.PI / this.sampleRate);
		for (int i = 0; i < this.sampleRate; i++) {
			sineTable[i] = (float) Math.sin(stepSize * ((float)i));
		}
		return sineTable;
	}

	public float sineByDeg(long angle)
	{
		return sineTable[(int)(angle % sampleRate)];
	}

	public int getSampleRate() {
		return sampleRate;
	}
}