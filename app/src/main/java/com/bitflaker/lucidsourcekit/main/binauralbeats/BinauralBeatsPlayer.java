package com.bitflaker.lucidsourcekit.main.binauralbeats;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import androidx.annotation.NonNull;

import com.bitflaker.lucidsourcekit.charts.FrequencyData;
import com.bitflaker.lucidsourcekit.general.AudioBufferPosition;
import com.bitflaker.lucidsourcekit.general.FastSineTable;
import com.bitflaker.lucidsourcekit.main.BinauralBeat;

import java.util.Arrays;

public class BinauralBeatsPlayer {
    private AudioTrack binauralAudioTrack;
    private final BinauralBeat binauralBeat;
    private final int sampleRate = 44100;
    private OnTrackProgress mProgressListener;
    private OnTrackFinished mFinishedListener;
    private boolean playing;
    private Thread trackThread;

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
        binauralAudioTrack.pause();
        System.gc();
    }

    public void resume() {
        playing = true;
        trackThread = new Thread(() -> playAndBufferBeats(binauralAudioTrack));
        trackThread.start();
    }

    public void stop() {
        binauralAudioTrack.pause();
        binauralAudioTrack.flush();
        playing = false;
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
        return new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channelConfig, audioFormat, bufferSize, AudioTrack.MODE_STREAM);
    }

    private void playAndBufferBeats(AudioTrack track) {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
        if(mProgressListener != null) {
            mProgressListener.onEvent(binauralBeat, (int) (track.getPlaybackHeadPosition() / (double) sampleRate));
        }
        track.setPositionNotificationPeriod(sampleRate);    // getting an update every second
        track.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
            @Override
            public void onMarkerReached(AudioTrack audioTrack) {}

            @Override
            public void onPeriodicNotification(AudioTrack audioTrack) {
                if(mProgressListener != null){
                    mProgressListener.onEvent(binauralBeat, (int)(track.getPlaybackHeadPosition()/(double)sampleRate));
                }
            }
        });
        // TODO calculate only once
        FastSineTable fst = new FastSineTable(sampleRate);
        short[] buffer = new short[50000];
        float[] normalSineWave = new float[buffer.length];
        AudioBufferPosition nextStartAt = new AudioBufferPosition();
        nextStartAt = NextBufferGenerator.generateSamples(binauralBeat, sampleRate, fst, buffer, normalSineWave, nextStartAt);
        boolean wasInFinal = false;
        while(!nextStartAt.isFinished() && !wasInFinal || nextStartAt.isFinished() && !wasInFinal) {
            if(nextStartAt.isFinished()){
                wasInFinal = true;
            }
            NextBufferGenerator nbg = new NextBufferGenerator(sampleRate, binauralBeat, fst, buffer.length, nextStartAt);
            Thread t = new Thread(nbg);
            t.start();
            track.play();
            track.write(buffer, 0, buffer.length);
            while(!nbg.isFinished()){
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            buffer = nbg.getBuffer();
            nextStartAt = nbg.getCurrentNextStartAfter();
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

class NextBufferGenerator implements Runnable {
    private final BinauralBeat binauralBeat;
    private final int sampleRate;
    private boolean finished;
    private float[] normalSineWave;
    private short[] buffer;
    private FastSineTable fst;
    private AudioBufferPosition nextStartAfter;
    private AudioBufferPosition currentNextStartAfter;
    private static long MAX_SAMPLE_COUNT = 50000;

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

        /*if(binauralBeat.getFrequencyList().size() == abp.getIndex() && (int)(binauralBeat.getFrequencyList().get(binauralBeat.getFrequencyList().size()-1).getDuration()*sampleRate*2) == abp.getBufferPosition()) {
            abp.setFinished(true);
        }*/

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
