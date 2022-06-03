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
import java.util.concurrent.atomic.AtomicBoolean;

public class BinauralBeatsPlayer {
    private AudioTrack binauralAudioTrack;
    private final BinauralBeat binauralBeat;
    private final int sampleRate = 44100;
    private OnTrackProgress mProgressListener;
    private OnTrackFinished mFinishedListener;
    private boolean playing;
    private Thread trackThread;

    private int playerWrittenCount = 0;
    private AudioBufferPosition currentPosition = null;
    private FastSineTable fst = new FastSineTable(sampleRate);
    private short[] buffer = new short[NextBufferGenerator.MAX_SAMPLE_COUNT];
    private float[] normalSineWave = new float[buffer.length];
    private boolean isPaused = false;
    private int frameCounter = 0;
    private int lastSecUpdate = 0;
    private int preStopCount = 0;
    NextBufferGenerator nbg = null;
    Thread nextBufferGenThread = null;

    // TODO: when changing beat => stop previous one !

    public BinauralBeatsPlayer(BinauralBeat binauralBeat) {
        this.binauralBeat = binauralBeat;
        binauralAudioTrack = null;
        playing = false;
    }

    public void play() {
        if(isInitialized() && !isPlaying()){ resume(); return; }
        binauralAudioTrack = generateNewAudioTrack();
        trackThread = new Thread(() -> playAndBufferBeats(binauralAudioTrack));
        trackThread.start();
        playing = true;
    }

    public void pause() {
        playing = false;
        trackThread.interrupt();
        System.gc();
    }

    public void resume() {
        playing = true;
        trackThread = new Thread(() -> playAndBufferBeats(binauralAudioTrack));
        trackThread.start();
    }

    public void stop() {
        if (binauralAudioTrack != null)  {
            binauralAudioTrack.pause();
            binauralAudioTrack.flush();
        }
        trackThread.interrupt();
        playing = false;
        System.gc();
    }

    public boolean isPlaying() {
        return playing;
    }

    public boolean isInitialized() {
        return binauralAudioTrack != null;
    }

    @NonNull
    private AudioTrack generateNewAudioTrack() {
        int channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat);
        return new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .build())
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build();
    }

    private void playAndBufferBeats(AudioTrack track) {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
        if(!isPaused) {
            if(currentPosition == null) {
                currentPosition = new AudioBufferPosition();
            }
            currentPosition = NextBufferGenerator.generateSamples(binauralBeat, sampleRate, fst, buffer, normalSineWave, currentPosition);
        }
        else {
            isPaused = false;
        }
        boolean wasInFinal = false;
        while(!currentPosition.isFinished() && !wasInFinal || currentPosition.isFinished() && !wasInFinal) {
            if(currentPosition.isFinished()){
                wasInFinal = true;
            }
            nbg = new NextBufferGenerator(sampleRate, binauralBeat, fst, NextBufferGenerator.MAX_SAMPLE_COUNT, currentPosition);
            nextBufferGenThread = new Thread(nbg);
            nextBufferGenThread.start();
            if(playerWrittenCount != 0) {
                buffer = Arrays.copyOfRange(buffer, playerWrittenCount, buffer.length);
                playerWrittenCount = 0;
            }
            track.play();
            playerWrittenCount = track.write(buffer, 0, buffer.length, AudioTrack.WRITE_BLOCKING);

            frameCounter = track.getPlaybackHeadPosition() + preStopCount;
            if((int)((frameCounter / (float)sampleRate)) > lastSecUpdate){
                lastSecUpdate = (int)((frameCounter / (float)sampleRate));
                if(mProgressListener != null){
                    mProgressListener.onEvent(binauralBeat, lastSecUpdate);
                }
            }

            if(Thread.interrupted()) {
                preStopCount += track.getPlaybackHeadPosition();
                playerWrittenCount = preStopCount % NextBufferGenerator.MAX_SAMPLE_COUNT;
                track.pause();
                track.flush();
                isPaused = true;
                return;
            }
            else {
                if(playerWrittenCount == buffer.length) {
                    playerWrittenCount = 0;
                }
                while(!nbg.isFinished()){
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                buffer = nbg.getBuffer();
                currentPosition = nbg.getCurrentNextStartAfter();
            }
        }
        System.out.println(track.getPlaybackHeadPosition());     // 3.324.256 | 3.309.616 | 3.323.280
        System.out.println(track.getPlaybackHeadPosition() + preStopCount);     // 3.282.468 | 3.324.256
        playing = false;
        playerWrittenCount = 0;
        preStopCount = 0;
        buffer = new short[NextBufferGenerator.MAX_SAMPLE_COUNT];
        normalSineWave = new float[buffer.length];
        currentPosition = new AudioBufferPosition();
        binauralAudioTrack = null;
        mFinishedListener.onEvent(binauralBeat);
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

class NextBufferGenerator implements Runnable {
    private final BinauralBeat binauralBeat;
    private final int sampleRate;
    private boolean finished;
    private float[] normalSineWave;
    private short[] buffer;
    private FastSineTable fst;
    private AudioBufferPosition nextStartAfter;
    private AudioBufferPosition currentNextStartAfter;
    public static final int MAX_SAMPLE_COUNT = 44100/6;
    private final AtomicBoolean terminateOperation = new AtomicBoolean(false);

    public NextBufferGenerator(int sampleRate, BinauralBeat binauralBeat, FastSineTable fst, int bufferSize, AudioBufferPosition nextStartAfter) {
        this.sampleRate = sampleRate;
        this.binauralBeat = binauralBeat;
        this.fst = fst;
        this.buffer = new short[bufferSize];
        this.normalSineWave = new float[bufferSize];
        this.nextStartAfter = nextStartAfter;
        this.finished = false;
    }

    @Override
    public void run() {
        currentNextStartAfter = generateSamples(binauralBeat, sampleRate, fst, buffer, normalSineWave, nextStartAfter);
        finished = true;
    }

    public void stop() {
        terminateOperation.set(true);
    }

    public static AudioBufferPosition generateSamples(BinauralBeat binauralBeat, int sampleRate, FastSineTable fst, short[] buffer, float[] normalSineWave, AudioBufferPosition startAt) {
        AudioBufferPosition startingAfter = startAt;
        AudioBufferPosition abp = new AudioBufferPosition();
        Arrays.fill(normalSineWave, 0);

        boolean reachedCount = false;
        int counter = 0;
        int twoPi = fst.getSize();

        for (int j = startAt.getIndex(); j < binauralBeat.getFrequencyList().size(); j++) {
            FrequencyData fd = binauralBeat.getFrequencyList().get(j);
            float base_freq = binauralBeat.getBaseFrequency();
            long angle1 = startingAfter.getAngle1();
            long angle2 = startingAfter.getAngle2();

            int conditionVal = (int)(fd.getDuration()*sampleRate*2);
            for (int i = startingAfter.getBufferPosition(); i < conditionVal; i+=2) {
                normalSineWave[i + counter - startingAfter.getBufferPosition()] = fst.sineByDeg(angle1);
                normalSineWave[i + 1 + counter - startingAfter.getBufferPosition()] = fst.sineByDeg(angle2);
                float from = fd.getFrequency();
                float to = Float.isNaN(fd.getFrequencyTo()) ? from : fd.getFrequencyTo();
                float ratio = (to - from) / (fd.getDuration() * sampleRate);
                float freq = from + ratio * (i/2.0f);
                angle1 += (int) (twoPi * (base_freq + freq) / sampleRate);
                angle2 += (int) (twoPi * (base_freq) / sampleRate);
                angle1 %= twoPi;
                angle2 %= twoPi;
                if(i + counter + 2 - startingAfter.getBufferPosition() == MAX_SAMPLE_COUNT) {
                    if(i + counter + 2 == conditionVal) {
                        abp.setIndex(j+1);
                        abp.setBufferPosition(0);
                        abp.setAngle1(0);
                        abp.setAngle2(0);
                    }
                    else {
                        abp.setIndex(j);
                        abp.setBufferPosition(i+2);
                        abp.setAngle1(angle1);
                        abp.setAngle2(angle2);
                    }
                    reachedCount = true;
                    break;
                }
            }
            if(reachedCount){ break; }
            counter += conditionVal - startingAfter.getBufferPosition();
            startingAfter.setAngle1(0);
            startingAfter.setAngle2(0);
            startingAfter.setBufferPosition(0);
        }
        if(!reachedCount){ abp.setFinished(true); }

        for (int i = 0; i < buffer.length; i += 2) {
            float volume = 0.05f;
            buffer[i] = (short) (volume * normalSineWave[i] * Short.MAX_VALUE);
            buffer[i+1] = (short) (volume * normalSineWave[i + 1] * Short.MAX_VALUE);
        }

        return abp;
    }

    public boolean isFinished() {
        return finished;
    }

    public short[] getBuffer() {
        return buffer;
    }

    public AudioBufferPosition getCurrentNextStartAfter() {
        return currentNextStartAfter;
    }
}
