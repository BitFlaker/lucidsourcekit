package com.bitflaker.lucidsourcekit.data;

import androidx.annotation.NonNull;

public class FastSineTable
{
	private static final FastSineTable instance = new FastSineTable(44100);
	private final float[] sineTable;
	private final int sampleRate;

	private FastSineTable(int sampleRate)
	{
		this.sampleRate = sampleRate;
		sineTable = generateSineTable();
	}

	public static FastSineTable getTable(){
		return instance;
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

	public float sineBySampleRateDeg(long angle)
	{
		return sineTable[(int)(angle % sampleRate)];
	}
}