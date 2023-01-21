package com.bitflaker.lucidsourcekit.main.binauralbeats;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

import androidx.annotation.NonNull;

import com.bitflaker.lucidsourcekit.charts.FrequencyData;
import com.bitflaker.lucidsourcekit.general.AudioBufferPosition;
import com.bitflaker.lucidsourcekit.general.FastSineTable;
import com.bitflaker.lucidsourcekit.main.BinauralBeat;

import java.util.Arrays;

import io.reactivex.rxjava3.core.Single;

public class BinauralBeatsPlayer {
    protected final static int SAMPLE_RATE = 44100;
    protected final static int MAX_SAMPLE_COUNT = BinauralBeatsPlayer.SAMPLE_RATE / 6;
    protected final static FastSineTable FAST_SINE_TABLE = new FastSineTable(SAMPLE_RATE);
    private final BinauralBeat binauralBeat;
    private int lastSecUpdate;
    private int frameCounter;
    private int preStopCount;
    private int playerWrittenCount;
    private boolean playing;
    private boolean isPaused;
    private short[] buffer;
    private OnTrackProgress mProgressListener;
    private OnTrackFinished mFinishedListener;
    private Thread trackThread;
    private AudioTrack binauralAudioTrack;
    private AudioBufferPosition currentPosition;
    private int updateSecondsCounter;

    public BinauralBeatsPlayer(BinauralBeat binauralBeat) {
        this.binauralBeat = binauralBeat;
        setDefaultPlayerValues();
    }

    public void play() {
        if(isInitialized() && !isPlaying()){ resume(); return; }
        binauralAudioTrack = generateNewAudioTrack();
        currentPosition = new AudioBufferPosition();
        trackThread = new Thread(() -> {
            try {
                playAndBufferBeats(binauralAudioTrack);
            } catch (InterruptedException e) { e.printStackTrace(); }
        });
        trackThread.start();
        playing = true;
    }

    public void pause() {
        playing = false;
        trackThread.interrupt();
        System.gc();
    }

    public void resume() {
        if(!isInitialized() || currentPosition == null) { return; }
        playing = true;
        trackThread = new Thread(() -> {
            try {
                playAndBufferBeats(binauralAudioTrack);
            } catch (InterruptedException e) { e.printStackTrace(); }
        });
        trackThread.start();
    }

    public void stop() {
        if (binauralAudioTrack != null)  {
            binauralAudioTrack.pause();
            binauralAudioTrack.flush();
        }
        if(trackThread != null){
            trackThread.interrupt();
        }
        playing = false;
        System.gc();
    }

    public boolean isPlaying() {
        return playing;
    }

    public boolean isInitialized() {
        return binauralAudioTrack != null;
    }

    private void setDefaultPlayerValues() {
        buffer = new short[MAX_SAMPLE_COUNT];
        trackThread = null;
        currentPosition = null;
        binauralAudioTrack = null;
        preStopCount = 0;
        frameCounter = 0;
        lastSecUpdate = 0;
        playerWrittenCount = 0;
        updateSecondsCounter = 0;
        playing = false;
        isPaused = false;
    }

    @NonNull
    private AudioTrack generateNewAudioTrack() {
        int channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, channelConfig, audioFormat);
        return new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(channelConfig)
                        .build())
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build();
    }

    private void playAndBufferBeats(AudioTrack track) throws InterruptedException {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

        // Make sure there is data to play
        if(!isPaused) {
            currentPosition = NextBufferGenerator.generateSamples(binauralBeat, buffer, currentPosition);
        }

        // Play as long as the beat has not ended yet
        isPaused = false;
        boolean wasInFinal = false;
        while(!wasInFinal) {
            // Check if this is the last run
            if(currentPosition.isFinished()){
                wasInFinal = true;
            }

            // Start generating data for next run (so we have some data buffered)
            Single<NextBufferGenerator.BufferGeneratorResult> bufferGeneratorResultSingle = NextBufferGenerator.generateSamples(binauralBeat, currentPosition);

            // If not all bytes have been written in the last run, write the rest now
            if(playerWrittenCount != 0) {
                buffer = Arrays.copyOfRange(buffer, playerWrittenCount, buffer.length);
            }

            // Stream all audio data
            track.play();
            playerWrittenCount = track.write(buffer, 0, buffer.length, AudioTrack.WRITE_BLOCKING);

            // Get the current playback seconds and raise a progress update event
            // after one second has passed since the last progress update event
            // NOTE: when the beat is like 26.95 seconds long, it will only fire 26 times,
            // so the last 0.95 seconds will be ignored
            frameCounter = track.getPlaybackHeadPosition() + preStopCount;
            int currentPlaybackSecond = (int)(frameCounter / (float) SAMPLE_RATE);
            if(currentPlaybackSecond > lastSecUpdate){
//                System.out.println("UPDATING " + ++updateSecondsCounter);
                lastSecUpdate = currentPlaybackSecond;
                if(mProgressListener != null){
                    mProgressListener.onEvent(binauralBeat, lastSecUpdate);
                }
            }

            // Behaviour if the player was paused
            if(Thread.interrupted()) {
                preStopCount += track.getPlaybackHeadPosition();
                playerWrittenCount = preStopCount % MAX_SAMPLE_COUNT;
                track.pause();
                track.flush();
                isPaused = true;
                return;
            }

            // Reset the written bytes counter if every byte in the buffer has been written
            playerWrittenCount = playerWrittenCount % buffer.length;

            // If the audio has been fully output, get the buffered next data
            if (playerWrittenCount == 0) {
                NextBufferGenerator.BufferGeneratorResult bufferGeneratorResult = bufferGeneratorResultSingle.blockingGet();
                buffer = bufferGeneratorResult.getBuffer();
                currentPosition = bufferGeneratorResult.getNextStartAfter();
            }
        }

        // Clean up player
        setDefaultPlayerValues();
        if(mFinishedListener != null){
            mFinishedListener.onEvent(binauralBeat);
        }
    }

    public interface OnTrackProgress {
        void onEvent(BinauralBeat binauralBeat, int progress);
    }

    public void setOnTrackProgressListener(OnTrackProgress eventListener) {
        mProgressListener = eventListener;
    }

    public interface OnTrackFinished {
        void onEvent(BinauralBeat binauralBeat);
    }

    public void setOnTrackFinishedListener(OnTrackFinished eventListener) {
        mFinishedListener = eventListener;
    }
}

class NextBufferGenerator {
    private final static int TWO_PI = BinauralBeatsPlayer.FAST_SINE_TABLE.getSampleRate();

    private NextBufferGenerator() { }

    public static Single<BufferGeneratorResult> generateSamples(BinauralBeat binauralBeat, AudioBufferPosition startAfter) {
        return Single.fromCallable(() -> {
            short[] buffer = new short[BinauralBeatsPlayer.MAX_SAMPLE_COUNT];
            AudioBufferPosition nextStartAfter = generateSamples(binauralBeat, buffer, startAfter);
            return new BufferGeneratorResult(buffer, nextStartAfter);
        });
    }

    public static AudioBufferPosition generateSamples(BinauralBeat binauralBeat, short[] buffer, AudioBufferPosition startAt) {
        int prevWrittenFrameCount = 0;
        float baseFrequency = binauralBeat.getBaseFrequency();
        float[] normalSineWave = new float[buffer.length];
        AudioBufferPosition abp = new AudioBufferPosition();

        for (int j = startAt.getIndex(); j < binauralBeat.getFrequencyList().size(); j++) {
            FrequencyData fd = binauralBeat.getFrequencyList().get(j);
            long angle1 = startAt.getAngle1();
            long angle2 = startAt.getAngle2();

            float fdFromFrequency = fd.getFrequency();
            float fdToFrequency = Float.isNaN(fd.getFrequencyTo()) ? fdFromFrequency : fd.getFrequencyTo();
            float fdFrequencyStepSize = (fdToFrequency - fdFromFrequency) / (fd.getDuration() * BinauralBeatsPlayer.SAMPLE_RATE);

            int fdFrameCount = (int)(fd.getDuration() * BinauralBeatsPlayer.SAMPLE_RATE * 2);
            for (int i = startAt.getBufferPosition(); i < fdFrameCount; i += 2) {
                int currentSineWaveIndex = i + prevWrittenFrameCount - startAt.getBufferPosition();

                normalSineWave[currentSineWaveIndex] = BinauralBeatsPlayer.FAST_SINE_TABLE.sineByDeg(angle1);
                normalSineWave[currentSineWaveIndex + 1] = BinauralBeatsPlayer.FAST_SINE_TABLE.sineByDeg(angle2);

                float frequency = fdFromFrequency + fdFrequencyStepSize * (i / 2.0f);

                angle1 = (angle1 + getAngleIncrementAtFrequency(baseFrequency + frequency)) % TWO_PI;
                angle2 = (angle2 + getAngleIncrementAtFrequency(baseFrequency)) % TWO_PI;

                boolean samplesFilledAfterNextRun = currentSineWaveIndex + 2 == normalSineWave.length;
                boolean fdFinishedAfterNextRun = currentSineWaveIndex + startAt.getBufferPosition() + 2 == fdFrameCount;

                if(samplesFilledAfterNextRun) {
                    if(fdFinishedAfterNextRun) {
                        abp.setIndex(j+1);
                        abp.setBufferPosition(0);
                        abp.setAngle1(0);
                        abp.setAngle2(0);
                        if(j + 1 == binauralBeat.getFrequencyList().size()){
                            abp.setFinished(true);
                        }
                    }
                    else {
                        abp.setIndex(j);
                        abp.setBufferPosition(i+2);
                        abp.setAngle1(angle1);
                        abp.setAngle2(angle2);
                    }
                    fillBuffer(buffer, normalSineWave);
                    return abp;
                }
            }
            prevWrittenFrameCount += fdFrameCount - startAt.getBufferPosition();
            startAt.setAngle1(0);
            startAt.setAngle2(0);
            startAt.setBufferPosition(0);
        }
        abp.setFinished(true);
        fillBuffer(buffer, normalSineWave);
        return abp;
    }

    private static void fillBuffer(short[] buffer, float[] normalSineWave) {
        for (int i = 0; i < buffer.length; i += 2) {
            float volume = 0.05f;
            buffer[i] = (short) (volume * normalSineWave[i] * Short.MAX_VALUE);
            buffer[i+1] = (short) (volume * normalSineWave[i + 1] * Short.MAX_VALUE);
        }
    }

    private static int getAngleIncrementAtFrequency(float frequency) {
        return (int) (TWO_PI * frequency / BinauralBeatsPlayer.SAMPLE_RATE);
    }

    static class BufferGeneratorResult {
        private final short[] buffer;
        private final AudioBufferPosition nextStartAfter;

        public BufferGeneratorResult(short[] buffer, AudioBufferPosition nextStartAfter) {
            this.buffer = buffer;
            this.nextStartAfter = nextStartAfter;
        }

        public short[] getBuffer() {
            return buffer;
        }

        public AudioBufferPosition getNextStartAfter() {
            return nextStartAfter;
        }
    }
}
