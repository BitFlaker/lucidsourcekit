package com.bitflaker.lucidsourcekit.main.binauralbeats

import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.roundToLong
import kotlin.concurrent.thread
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process

const val TWO_PI = 2.0 * PI
const val CHANNELS = 2
const val CHUNK_FRAMES = 1024
const val FADE_SECONDS = 0.008
const val GAIN_EPSILON = 1e-4

class BinauralBeatsPlayer(
    private val sampleRate: Int = 48_000,
    private val amplitude: Double = 0.85,
) {
    private val phaseStep = TWO_PI / sampleRate
    private val gainStep = 1.0 / (FADE_SECONDS * sampleRate)
    private val endFadeSamples = FADE_SECONDS * sampleRate
    private val buffer = FloatArray(CHUNK_FRAMES * CHANNELS)

    private val lock = Object()
    private var state = State.IDLE
    private var stopRequested = false
    private var currentTimeline: Timeline? = null
    private var playerThread: Thread? = null
    private var lastReportedProgressSeconds = -1

    val binauralBeat: BinauralBeat? get() = currentTimeline?.binauralBeat
    val isPlaying: Boolean get() = synchronized(lock) { state == State.PLAYING }
    var onProgressListener: ((BinauralBeat, Int) -> Unit)? = null
    var onFinishedListener: ((BinauralBeat) -> Unit)? = null

    fun setBinauralBeat(binauralBeat: BinauralBeat) {
        reset()
        currentTimeline = Timeline(binauralBeat, sampleRate)
    }

    fun play() {
        // Check if track just has to be resumed
        synchronized(lock) {
            if (state == State.PAUSED) {
                state = State.PLAYING
                lock.notifyAll()
                return
            }
        }

        // Start new binaural beat playback
        stop()
        currentTimeline?.let {
            setBinauralBeat(it.binauralBeat)
        }
        synchronized(lock) {
            state = State.PLAYING
            stopRequested = false
            playerThread = thread(name = "BinauralPlayer") {
                playerLoop()
            }
        }
    }

    fun pause() {
        synchronized(lock) { if (state == State.PLAYING) state = State.PAUSED }
    }

    fun stop() {
        val running = synchronized(lock) {
            stopRequested = true
            lock.notifyAll()
            playerThread
        }
        running?.join()
    }

    fun reset() {
        stop()
        lastReportedProgressSeconds = -1
        synchronized(lock) { currentTimeline = null }
    }

    private fun playerLoop() {
        val timeline = currentTimeline ?: return
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)

        val track = buildTrack()
        track.play()

        try {
            while (timeline.positionSample < timeline.totalSamples) {
                val currentState = synchronized(lock) { if (stopRequested) null else state } ?: break

                if (currentState != State.PAUSED) {
                    sampleAndWrite(track, timeline, target = 1.0)
                    continue
                }
                if (timeline.gain > GAIN_EPSILON) {
                    sampleAndWrite(track, timeline, target = 0.0)
                    continue
                }

                // Player was paused, wait for stop/resume
                padWithSilence(track)
                track.pause()
                val stopped = synchronized(lock) {
                    while (state == State.PAUSED && !stopRequested) lock.wait()
                    stopRequested
                }
                if (stopped) break
                track.flush()
                track.play()
            }

            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                while (timeline.gain > GAIN_EPSILON && timeline.positionSample < timeline.totalSamples) {
                    sampleAndWrite(track, timeline, target = 0.0)
                }
                padWithSilence(track)
            }
        } finally {
            runCatching {
                track.pause()
                track.flush()
                track.release()
            }
            synchronized(lock) {
                state = State.IDLE
                if (playerThread === Thread.currentThread()) playerThread = null
            }
        }

        if (timeline.positionSample == timeline.totalSamples) {
            onFinishedListener?.invoke(timeline.binauralBeat)
        }
    }

    private fun sampleAndWrite(track: AudioTrack, timeline: Timeline, target: Double) {
        val frames = min(CHUNK_FRAMES.toLong(), timeline.totalSamples - timeline.positionSample).toInt()
        for (i in 0 until frames) {
            val halfBinauralFrequency = timeline.currentBinauralFrequency() * 0.5
            timeline.phaseLeft = (timeline.phaseLeft + (timeline.binauralBeat.baseFrequency - halfBinauralFrequency) * phaseStep) % TWO_PI
            timeline.phaseRight = (timeline.phaseRight + (timeline.binauralBeat.baseFrequency + halfBinauralFrequency) * phaseStep) % TWO_PI

            timeline.gain += (target - timeline.gain).coerceIn(-gainStep, gainStep)
            val endFade = min(1.0, (timeline.totalSamples - timeline.positionSample) / endFadeSamples)
            val a = amplitude * timeline.gain * endFade

            buffer[i * 2] = (sin(timeline.phaseLeft) * a).toFloat()
            buffer[i * 2 + 1] = (sin(timeline.phaseRight) * a).toFloat()
            timeline.moveToNextSample()
        }

        track.write(buffer, 0, frames * CHANNELS, AudioTrack.WRITE_BLOCKING)
        val currentPlaybackSecond = (timeline.positionSample / sampleRate).toInt()
        if (currentPlaybackSecond > lastReportedProgressSeconds) {
            lastReportedProgressSeconds = currentPlaybackSecond
            onProgressListener?.invoke(timeline.binauralBeat, lastReportedProgressSeconds)
        }
    }

    private fun padWithSilence(track: AudioTrack) {
        buffer.fill(0f)
        var remaining = track.bufferSizeInFrames
        while (remaining > 0) {
            val n = min(remaining, CHUNK_FRAMES)
            track.write(buffer, 0, n * CHANNELS, AudioTrack.WRITE_BLOCKING)
            remaining -= n
        }
    }

    private fun buildTrack(): AudioTrack {
        val channelConfig = AudioFormat.CHANNEL_OUT_STEREO
        val audioFormat = AudioFormat.ENCODING_PCM_FLOAT
        val bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(audioFormat)
                    .setChannelMask(channelConfig)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(maxOf(bufferSize * 2, CHUNK_FRAMES * CHANNELS * Float.SIZE_BYTES * 2))
            .build()
    }

    private class Timeline(val binauralBeat: BinauralBeat, sampleRate: Int) {
        private class Span(val positionSampleStart: Long, val positionSampleEnd: Long, val from: Double, val to: Double)

        private val spans: List<Span>
        private var index = 0
        val totalSamples: Long
        var phaseLeft = 0.0
        var phaseRight = 0.0
        var gain = 0.0

        var positionSample: Long = 0
            private set

        init {
            var positionSampleStart = 0L
            spans = binauralBeat.segments.map {
                val positionSampleEnd = positionSampleStart + (it.duration * sampleRate).roundToLong()
                val span = Span(positionSampleStart, positionSampleEnd, it.frequencyFrom, it.frequencyTo)
                positionSampleStart = positionSampleEnd
                span
            }
            totalSamples = positionSampleStart
        }

        fun currentBinauralFrequency(): Double {
            while (index < spans.lastIndex && positionSample >= spans[index].positionSampleEnd) index++
            val span = spans[index]
            if (positionSample >= span.positionSampleEnd) return span.to
            val t = (positionSample - span.positionSampleStart).toDouble() / (span.positionSampleEnd - span.positionSampleStart)
            return span.from + (span.to - span.from) * t
        }

        fun moveToNextSample() {
            positionSample++
        }
    }

    private enum class State { IDLE, PLAYING, PAUSED }
}