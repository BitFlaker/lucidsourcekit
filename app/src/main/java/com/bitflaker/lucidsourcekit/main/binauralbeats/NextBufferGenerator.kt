package com.bitflaker.lucidsourcekit.main.binauralbeats

import com.bitflaker.lucidsourcekit.utils.Ref

object NextBufferGenerator {
    private const val VOLUME = 0.05f
    private const val AMPLITUDE = VOLUME * Short.Companion.MAX_VALUE

    /**
     * Generates the samples for the AudioTrack's audioData for a specified BinauralBeat with the
     * positions and values from past packets of the current BinauralBeat
     * @param binauralBeat the binaural beat to generate the samples for
     * @param position the positions and values of past packets of the current BinauralBeat
     * @return a BufferGeneratorResult with the AudioTrack audioData as buffer and an updated
     * clone of the passed AudioBufferPosition
     */
    fun generateSamples(binauralBeat: BinauralBeat, position: AudioBufferPosition): BufferGeneratorResult {
        val buffer = ShortArray(MAX_SAMPLE_COUNT)
        val nextStartAfter = generateSamples(binauralBeat, buffer, position)
        return BufferGeneratorResult(buffer, nextStartAfter)
    }

    /**
     * Generates the samples for the AudioTrack's audioData for a specified BinauralBeat with the
     * positions and values from past packets of the current BinauralBeat
     * @param binauralBeat the binaural beat to generate the samples for
     * @param buffer the buffer to write the generated samples to
     * @param position the positions and values of past packets of the current BinauralBeat
     * @return an updated clone of the passed AudioBufferPosition
     */
    fun generateSamples(binauralBeat: BinauralBeat, buffer: ShortArray, position: AudioBufferPosition): AudioBufferPosition {
        require(buffer.size % 2 == 0) { "Buffer has to be a multiple of 2 as this is a stereo audio buffer" }
        val updatedPosition = position.clone()
        var bufferOffset = 0

        for (j in updatedPosition.index..<binauralBeat.frequencyList.size()) {
            val segment = binauralBeat.frequencyList[j]
            val segmentSize = (segment.duration * SAMPLE_RATE * 2).toInt()
            val remainingSegmentSize = segmentSize - updatedPosition.bufferPosition

            // Try to fill the buffer according to the current segment values
            val written = fillBuffer(
                buffer,
                binauralBeat.baseFrequency,
                bufferOffset,
                segment.frequency,
                segment.getFrequencyStepSize(SAMPLE_RATE),
                segmentSize,
                updatedPosition
            )

            // In case the segment has not been fully written (therefore the buffer is full and
            // the remaining data has not been written), return the current position data
            if (written != remainingSegmentSize) {
                updatedPosition.index = j
                updatedPosition.bufferPosition = updatedPosition.bufferPosition + written
                return updatedPosition
            }

            // Set the current position for the next segment to be handled by the buffer. This
            // means index will be `frequencyList.size()` in the end
            updatedPosition.index = j + 1
            updatedPosition.bufferPosition = 0
            bufferOffset += written

            // In case the buffer has been fully filled, immediately return the updated position
            // as well as the current finished state
            if (bufferOffset == buffer.size) {
                updatedPosition.isFinished = updatedPosition.index == binauralBeat.frequencyList.size()
                return updatedPosition
            }
        }

        // All segments of the track have already been written to the buffer
        updatedPosition.isFinished = true
        return updatedPosition
    }

    /**
     * Fills a short[] buffer with binaural beats PCM data for a frequency data packet
     * @param buffer the buffer to fill
     * @param baseFrequency the base frequency of the binaural beat
     * @param bufferOffset the amount of frames written to a previous packet in the current buffer
     * @param segmentFromFrequency the binaural frequency
     * @param stepSize the step size of the transition of the binaural beat
     * @param segmentSize the total count of frames of the frequency data packet
     * @param position the position snapshot information of the other packet passes
     * @return the amount of values written to the buffer array
     */
    private fun fillBuffer(
        buffer: ShortArray,
        baseFrequency: Float,
        bufferOffset: Int,
        segmentFromFrequency: Float,
        stepSize: Float,
        segmentSize: Int,
        position: AudioBufferPosition
    ): Int {
        var written = 0
        val bufferIndex = Ref(bufferOffset)

        while (written + position.bufferPosition < segmentSize && bufferIndex.value + 2 < buffer.size) {
            val stepCount = (position.bufferPosition + written) / 2.0f
            val offsetFrequency = baseFrequency + segmentFromFrequency + stepSize * stepCount

            position.angleLeftSpeaker = moveAngle(buffer, bufferIndex, position.angleLeftSpeaker, offsetFrequency)
            position.angleRightSpeaker = moveAngle(buffer, bufferIndex, position.angleRightSpeaker, baseFrequency)

            written = bufferIndex.value - bufferOffset
        }

        return written
    }

    private fun moveAngle(buffer: ShortArray, bufferIndex: Ref<Int>, angle: Long, frequency: Float): Long {
        buffer[bufferIndex.value] = (AMPLITUDE * FastSineTable.table.sineBySampleRateDeg(angle)).toInt().toShort()
        bufferIndex.value++
        return (angle + frequency.toLong()) % SAMPLE_RATE
    }

    class BufferGeneratorResult(
        val buffer: ShortArray,
        val position: AudioBufferPosition
    )
}