package com.bitflaker.lucidsourcekit.main.binauralbeats

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import com.bitflaker.lucidsourcekit.data.AudioBufferPosition
import com.bitflaker.lucidsourcekit.data.records.BinauralBeat
import com.bitflaker.lucidsourcekit.main.binauralbeats.NextBufferGenerator.BufferGeneratorResult
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

const val SAMPLE_RATE: Int = 44100
const val MAX_SAMPLE_COUNT: Int = SAMPLE_RATE / 6

class BinauralBeatsPlayer(private val binauralBeat: BinauralBeat) {
    private var buffer = ShortArray(MAX_SAMPLE_COUNT)
    private val executor = Executors.newCachedThreadPool()
    private var trackThread: Thread? = null
    private var currentPosition: AudioBufferPosition? = null
    private var binauralAudioTrack: AudioTrack? = null
    private var frameCounter = 0
    private var prePauseCount = 0
    private var playerWrittenCount = 0
    private var lastReportedProgressSeconds = 0
    var isPlaying: Boolean = false
        private set
    var onProgressListener: ((BinauralBeat, Int) -> Unit)? = null
    var onFinishedListener: ((BinauralBeat) -> Unit)? = null

    /**
     * Starts playing the selected binaural beat or resumes playing the already initialized player
     */
    fun play() {
        // Initialize player state or re-use already initialized values to resume
        binauralAudioTrack = binauralAudioTrack ?: initAudioTrack()

        // Launch player and buffer generator in new thread
        trackThread = Thread {
            playAndBufferBeats()
        }
        trackThread?.start()
        isPlaying = true
    }

    fun pause() {
        isPlaying = false
        trackThread?.interrupt()
        System.gc()
    }

    fun stop() {
        binauralAudioTrack?.pause()
        binauralAudioTrack?.flush()
        trackThread?.interrupt()
        reset()
        System.gc()
    }

    private fun reset() {
        buffer = ShortArray(MAX_SAMPLE_COUNT)
        trackThread = null
        currentPosition = null
        binauralAudioTrack = null
        prePauseCount = 0
        frameCounter = 0
        lastReportedProgressSeconds = 0
        playerWrittenCount = 0
        isPlaying = false
    }

    private fun initAudioTrack(): AudioTrack {
        val channelConfig = AudioFormat.CHANNEL_OUT_STEREO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, channelConfig, audioFormat)

        // Build and return new audio track
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(audioFormat)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(channelConfig)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    private fun playAndBufferBeats() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)

        // Ensure a track is initialized before playing and buffering
        val track = binauralAudioTrack ?: throw IllegalStateException("Unable to play without initialized audio track")

        // Ensure data is available to play
        currentPosition = currentPosition ?: NextBufferGenerator.generateSamples(binauralBeat, buffer, AudioBufferPosition())

        // Create a future to buffer the next player segment
        var nextBufferFuture: CompletableFuture<BufferGeneratorResult>? = null

        // Play until the currentPosition's `isFinished` flag is true and the buffer has been fully written
        while (true) {
            val position = currentPosition ?: throw IllegalStateException("Unable to play without initialized audio buffer position! This case should not be reachable")

            // Start generating data for next buffer segment (if not already cached)
            nextBufferFuture = nextBufferFuture ?: CompletableFuture.supplyAsync({
                NextBufferGenerator.generateSamples(binauralBeat, position)
            }, executor)

            // If not all bytes have been written in the last run, write the rest now
            if (playerWrittenCount != 0) {
                buffer = buffer.copyOfRange(playerWrittenCount, buffer.size)
            }

            // Stream all audio data
            track.play()
            playerWrittenCount = track.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)

            // Get the current playback seconds and raise a progress update event
            // after one second has passed since the last progress update event
            // NOTE: when the beat is like 26.95 seconds long, it will only fire 26 times,
            // so the last 0.95 seconds will be ignored
            frameCounter = track.playbackHeadPosition + prePauseCount
            val currentPlaybackSecond = (frameCounter / SAMPLE_RATE.toFloat()).toInt()
            if (currentPlaybackSecond > lastReportedProgressSeconds) {
                lastReportedProgressSeconds = currentPlaybackSecond
                onProgressListener?.invoke(binauralBeat, lastReportedProgressSeconds)
            }

            // Handle player paused event
            if (Thread.interrupted()) {
                prePauseCount += track.playbackHeadPosition
                playerWrittenCount = prePauseCount % MAX_SAMPLE_COUNT
                track.pause()
                track.flush()
                return
            }

            // Reset the written bytes counter if every byte in the buffer has been written
            playerWrittenCount = playerWrittenCount % buffer.size

            // If the entire sample buffer has been written, consume the next buffered data
            if (playerWrittenCount == 0) {
                val nextBufferResult = nextBufferFuture!!.get()
                buffer = nextBufferResult.buffer
                currentPosition = nextBufferResult.position
                nextBufferFuture = null

                // Stop playing after end has been reached. TODO: Check if the check for playerWrittenCount == 0 is OK or if this might cause endless loops
                if (position.isFinished) {
                    break
                }
            }
        }

        // Clean up player
        reset()
        onFinishedListener?.invoke(binauralBeat)
    }
}

