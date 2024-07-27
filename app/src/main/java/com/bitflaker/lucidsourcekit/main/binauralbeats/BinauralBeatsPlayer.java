package com.bitflaker.lucidsourcekit.main.binauralbeats;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

import androidx.annotation.NonNull;

import com.bitflaker.lucidsourcekit.data.FrequencyData;
import com.bitflaker.lucidsourcekit.data.records.BinauralBeat;
import com.bitflaker.lucidsourcekit.data.AudioBufferPosition;
import com.bitflaker.lucidsourcekit.data.FastSineTable;

import java.util.Arrays;

import io.reactivex.rxjava3.core.Single;

public class BinauralBeatsPlayer {
    protected final static int SAMPLE_RATE = 44100;
    protected final static int MAX_SAMPLE_COUNT = BinauralBeatsPlayer.SAMPLE_RATE / 6;
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
                buffer = bufferGeneratorResult.buffer();
                currentPosition = bufferGeneratorResult.nextStartAfter();
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
    private final static float VOLUME = 0.05f;
    private final static float AMPLITUDE = VOLUME * Short.MAX_VALUE;

    private NextBufferGenerator() { }

    /**
     * Generates the samples for the AudioTrack's audioData for a specified BinauralBeat with the positions and values from past packets of the current BinauralBeat
     * @param binauralBeat the binaural beat to generate the samples for
     * @param audioBufferPosition the positions and values of past packets of the current BinauralBeat
     * @return a BufferGeneratorResult with the AudioTrack audioData as buffer and an updated clone of the passed AudioBufferPosition
     */
    public static Single<BufferGeneratorResult> generateSamples(BinauralBeat binauralBeat, AudioBufferPosition audioBufferPosition) {
        return Single.fromCallable(() -> {
            short[] buffer = new short[BinauralBeatsPlayer.MAX_SAMPLE_COUNT];
            AudioBufferPosition nextStartAfter = generateSamples(binauralBeat, buffer, audioBufferPosition);
            return new BufferGeneratorResult(buffer, nextStartAfter);
        });
    }

    /**
     * Generates the samples for the AudioTrack's audioData for a specified BinauralBeat with the positions and values from past packets of the current BinauralBeat
     * @param binauralBeat the binaural beat to generate the samples for
     * @param buffer the buffer to write the generated samples to
     * @param audioBufferPosition the positions and values of past packets of the current BinauralBeat
     * @return an updated clone of the passed AudioBufferPosition
     */
    public static AudioBufferPosition generateSamples(BinauralBeat binauralBeat, short[] buffer, AudioBufferPosition audioBufferPosition) {
        float baseFrequency = binauralBeat.baseFrequency();
        AudioBufferPosition abp = audioBufferPosition.clone();
        int prevWrittenFrameCount = 0;

        for (int j = abp.getIndex(); j < binauralBeat.frequencyList().size(); j++) {
            FrequencyData fd = binauralBeat.frequencyList().get(j);
            int fdFrameCount = (int)(fd.getDuration() * BinauralBeatsPlayer.SAMPLE_RATE * 2);
            int written = fillFrequencyPacket(buffer, baseFrequency, prevWrittenFrameCount, fd.getFrequency(), fd.getFrequencyStepSize(BinauralBeatsPlayer.SAMPLE_RATE), fdFrameCount, abp);
            int fullCurrentPacketSize = fdFrameCount - abp.getBufferPosition();

            // Check whether the frequency data packet was fully written (then continue with the
            // next frequency data in the list to fill the remaining buffer space) or not (then save
            // the current position data to the AudioBufferPosition and return it)
            if(fdFrameCount == written + abp.getBufferPosition()) {
                abp.setBufferPosition(0);
                if(prevWrittenFrameCount + fullCurrentPacketSize == buffer.length) {
                    abp.setIndex(j + 1);
                    if(j + 1 != binauralBeat.frequencyList().size()){
                        return abp;
                    }
                }
                prevWrittenFrameCount += fullCurrentPacketSize;
            }
            else {
                abp.setIndex(j);
                abp.setBufferPosition(abp.getBufferPosition() + written);
                return abp;
            }
        }
        abp.setFinished(true);
        return abp;
    }

    /**
     * Fills a short[] buffer with binaural beats PCM data for a frequency data packet
     * @param buffer the buffer to fill
     * @param baseFrequency the base frequency of the binaural beat
     * @param prevWrittenFrameCount the amount of frames written to a previous packet in the current buffer
     * @param fdFromFrequency the binaural frequency
     * @param fdFrequencyStepSize the step size of the transition of the binaural beat
     * @param fdFrameCount the total count of frames of the frequency data packet
     * @param audioBufferPosition the position snapshot information of the other packet passes
     * @return the amount of values written to the buffer array
     */
    private static int fillFrequencyPacket(short[] buffer, float baseFrequency, int prevWrittenFrameCount, float fdFromFrequency, float fdFrequencyStepSize, int fdFrameCount, AudioBufferPosition audioBufferPosition) {
        int written = 0;
        int currentBufferIndex = prevWrittenFrameCount;
        for (int i = audioBufferPosition.getBufferPosition(); i < fdFrameCount && currentBufferIndex + 2 < buffer.length; i += 2) {
            currentBufferIndex = i + prevWrittenFrameCount - audioBufferPosition.getBufferPosition();
            float offsetFrequency = baseFrequency + (fdFromFrequency + fdFrequencyStepSize * (i / 2.0f));

            audioBufferPosition.setAngleLeftSpeaker(applyCurrentSinePosition(buffer, currentBufferIndex, audioBufferPosition.getAngleLeftSpeaker(), offsetFrequency));
            audioBufferPosition.setAngleRightSpeaker(applyCurrentSinePosition(buffer, currentBufferIndex + 1, audioBufferPosition.getAngleRightSpeaker(), baseFrequency));

            written += 2;
        }
        return written;
    }

    private static long applyCurrentSinePosition(short[] buffer, int bufferIndex, long angle, float frequency) {
        buffer[bufferIndex] = (short) (AMPLITUDE * FastSineTable.getTable().sineBySampleRateDeg(angle));
        return (angle + (long) frequency) % BinauralBeatsPlayer.SAMPLE_RATE;
    }

    record BufferGeneratorResult(short[] buffer, AudioBufferPosition nextStartAfter) { }
}
