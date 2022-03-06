package com.bitflaker.lucidsourcekit.general;

public class FastSineTable
{
	float[] sineTable;
	float stepSize;
	public int size;

	public FastSineTable(int sampleRate)
	{
		size = sampleRate;
		sineTable = new float[size];
		this.stepSize = (float) (2d * Math.PI / size);
		for (int i = 0; i < size; i++) {
			sineTable[i] = (float) Math.sin(this.stepSize * ((float)i));
		}
	}

	public float sineByDeg(long angle)
	{
		int index = (int)(angle % size);
		return sineTable[index];
	}

	public int getSize() {
		return size;
	}
}