package com.bitflaker.lucidsourcekit.main.binauralbeats;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import androidx.annotation.NonNull;

import com.bitflaker.lucidsourcekit.charts.FrequencyData;
import com.bitflaker.lucidsourcekit.main.BinauralBeat;

import java.util.concurrent.TimeUnit;

public class BinauralBeatsPlayer {
    private AudioTrack leftEarAudioTrack;
    private final AudioTrack rightEarAudioTrack;
    private final BinauralBeat binauralBeat;
    private final int sampleRate = 44100;
    private OnTrackProgress mProgressListener;
    private OnTrackFinished mFinishedListener;
    private boolean playing;
    private Thread leftEarTrackThread;
    private Thread rightEarTrackThread;
    private final int stoppedAtPacket = 0;
    private final int wroteCount = 0;
    //private final int maxSampleCount = 22459328/6;
    private final int maxSampleCount = 500000;
    private int samplesLeft = 0;
    private int samplesWritten = 0;

    public BinauralBeatsPlayer(BinauralBeat binauralBeat) {
        this.binauralBeat = binauralBeat;
        leftEarAudioTrack = null;
        rightEarAudioTrack = null;
        playing = false;
    }

    public void play() {
        if(isInitialized() && !isPlaying()){ resume(); return; }

        leftEarAudioTrack = generateNewAudioTrack();
        leftEarTrackThread = new Thread(() -> playAndBufferBeats(leftEarAudioTrack, SpeakerSide.LEFT));
        leftEarTrackThread.start();

        /*
        rightEarAudioTrack = generateNewAudioTrack();
        rightEarTrackThread = new Thread(() -> playAndBufferBeats(rightEarAudioTrack, SpeakerSide.RIGHT));
        rightEarTrackThread.start();

         */

        playing = true;
    }

    public void pause() {
        playing = false;
        leftEarAudioTrack.pause();
        rightEarAudioTrack.pause();
        System.gc();
    }

    public void resume() {
        playing = true;
        leftEarTrackThread = new Thread(() -> playAndBufferBeats(leftEarAudioTrack, SpeakerSide.LEFT));
        leftEarTrackThread.start();
        rightEarTrackThread = new Thread(() -> playAndBufferBeats(rightEarAudioTrack, SpeakerSide.RIGHT));
        rightEarTrackThread.start();
    }

    public void stop() {
        leftEarAudioTrack.pause();
        rightEarAudioTrack.pause();
        leftEarAudioTrack.flush();
        rightEarAudioTrack.flush();
        playing = false;
    }

    public boolean isPlaying() {
        return playing;
    }

    public boolean isInitialized() {
        return leftEarAudioTrack != null && rightEarAudioTrack != null;
    }

    @NonNull
    private AudioTrack generateNewAudioTrack() {
        //int channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
        int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat);
        return new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channelConfig, audioFormat, bufferSize, AudioTrack.MODE_STREAM);
    }

    private byte[] genTone(double startFreq, double endFreq, int dur) {
        int totalSamples = dur * sampleRate;
        samplesLeft = totalSamples - samplesWritten;
        int numSamples = Math.min(samplesLeft, maxSampleCount);
        samplesLeft -= numSamples;
        System.out.println(totalSamples + " | " + samplesLeft + " | " + samplesWritten + " | " + numSamples);
        double[] sample = new double[numSamples];
        double currentFreq, numerator;
        for (int i = samplesWritten; i < numSamples+samplesWritten; ++i) {
            numerator = (double) i / (double) totalSamples;
            currentFreq = startFreq + ((numerator * (endFreq - startFreq))/2.0);
            sample[i-samplesWritten] = Math.sin(2 * Math.PI * i / (sampleRate / currentFreq));
        }
        samplesWritten += numSamples;
        if(samplesLeft == 0) { samplesWritten = 0; }
        return convertToPCM(numSamples, sample);
    }

    private void playAndBufferBeats(AudioTrack track, SpeakerSide speakerSide) {
        if(speakerSide == SpeakerSide.LEFT){
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
        }
        //byte[] trackDataRise = genTone(440, 1000, 3);
        //byte[] trackDataFall = genTone(1000, 440, 3);
        /*
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
        try {
            outputStream.write(trackDataRise);
            outputStream.write(trackDataFall);
            outputStream.write(trackDataRise);
            outputStream.write(trackDataFall);
            outputStream.write(trackDataRise);
            outputStream.write(trackDataFall);
            outputStream.write(trackDataRise);
            outputStream.write(trackDataFall);
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] trackDataFinal = outputStream.toByteArray();
         */

        //byte[] trackDataFinal = generateSineWavefreqOLD(1000, 440, 3);
        track.play();
        NextBytesGenerator nbg = null;
        byte[] trackData;
        for (int i = 0; i < binauralBeat.getFrequencyList().size(); i++) {
            // TODO preload next
            long start = System.nanoTime();
            if(nbg == null){
                // calc by hand
                FrequencyData data = binauralBeat.getFrequencyList().get(i);
                float baseFreq = binauralBeat.getBaseFrequency();
                trackData = genTone(data.getFrequency() + baseFreq, (Float.isNaN(data.getFrequencyTo()) ? data.getFrequency() : data.getFrequencyTo()) + baseFreq, (int)data.getDuration());
            }
            else {
                trackData = nbg.getValue();
                samplesWritten = nbg.getSamplesWritten();
                samplesLeft = nbg.getSamplesLeft();
                if(trackData == null){
                    break;
                }
            }
            if(samplesLeft > 0){
                nbg = new NextBytesGenerator(sampleRate, binauralBeat, i, samplesWritten, samplesLeft, maxSampleCount);
            }
            else {
                nbg = new NextBytesGenerator(sampleRate, binauralBeat, i+1, samplesWritten, samplesLeft, maxSampleCount);
            }
            Thread t = new Thread(nbg);
            t.start();
            System.out.println("TIME CONSUMED: " + (TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS)));
            track.write(trackData, 0, trackData.length);
            if(samplesLeft > 0){ i--; }
        }
        /*track.write(trackDataFall, 0, trackDataFall.length);
        track.write(trackDataRise, 0, trackDataRise.length);
        track.write(trackDataFall, 0, trackDataFall.length);
        track.write(trackDataRise, 0, trackDataRise.length);
        track.write(trackDataFall, 0, trackDataFall.length);
        track.write(trackDataRise, 0, trackDataRise.length);
        track.write(trackDataFall, 0, trackDataFall.length);

         */
        /*track.play();
        NextBytesGenerator nbg;
        if(speakerSide == SpeakerSide.LEFT){ nbg = new NextBytesGenerator(sampleRate, binauralBeat, stoppedAtPacket, NextBytesGenerator.SpeakerSide.LEFT, leftEarAudioTrack); }
        else { nbg = new NextBytesGenerator(sampleRate, binauralBeat, stoppedAtPacket, NextBytesGenerator.SpeakerSide.RIGHT, rightEarAudioTrack); }
        Thread t = new Thread(nbg);
        t.start();
         */
        /*
        double[] samples = generateNextTone(speakerSide, stoppedAtPacket);
        byte[] pcm = convertToPCM(samples.length, samples);
        byte[] newC = Arrays.copyOfRange(pcm, wroteCount, pcm.length);
        int wrote = track.write(newC, 0, newC.length, AudioTrack.WRITE_BLOCKING);
        if(speakerSide == SpeakerSide.LEFT) {
            if(!isPlaying()){
                wroteCount += wrote;
                return;
            }
            wroteCount = 0;
        }
        else if(speakerSide == SpeakerSide.RIGHT && !isPlaying()) {
            return;
        }
        for (int i = stoppedAtPacket+1; i < binauralBeat.getFrequencyList().size(); i++) {
            // TODO use pre-generated lines instead of following lines
            //samples = generateNextTone(speakerSide, i);
            //pcm = convertToPCM(samples.length, samples);
            // TODO hit up next generation
            pcm = nbg.getValue();
            if(speakerSide == SpeakerSide.LEFT){
                nbg = new NextBytesGenerator(sampleRate, binauralBeat, i+1, NextBytesGenerator.SpeakerSide.LEFT, leftEarAudioTrack);
            }
            else {
                nbg = new NextBytesGenerator(sampleRate, binauralBeat, i+1, NextBytesGenerator.SpeakerSide.RIGHT, rightEarAudioTrack);
            }
            t = new Thread(nbg);
            t.start();
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            wrote = track.write(pcm, 0, pcm.length, AudioTrack.WRITE_BLOCKING);
            if(!isPlaying() && speakerSide == SpeakerSide.LEFT) {
                stoppedAtPacket = i;
                wroteCount = wrote;
                break;
            }
            else if(!isPlaying() && speakerSide == SpeakerSide.RIGHT) {
                break;
            }
        }
        if(isPlaying() && speakerSide == SpeakerSide.LEFT) {
            if(mFinishedListener != null) { mFinishedListener.onEvent(binauralBeat); }
            stop();
        }
         */
    }

    private double[] generateNextTone(SpeakerSide speakerSide, int j) {
        FrequencyData current = binauralBeat.getFrequencyList().get(j);
        int freqSide = speakerSide == SpeakerSide.LEFT ? -1 : 1;
        float freqFrom = current.getFrequency() - (binauralBeat.getBaseFrequency()/2.0f*freqSide);
        float freqTo = current.getFrequencyTo() - (binauralBeat.getBaseFrequency()/2.0f*freqSide);
        if(Float.isNaN(freqTo)) { freqTo = freqFrom; }
        int numSamples = (int)(current.getDuration() * sampleRate * 2);
        double[] samples = new double[numSamples];
        double currentFreq, numerator;
        for (int i = 0; i < numSamples; i+=2) {
            numerator = (i/2.0) / (numSamples/2.0);
            currentFreq = freqFrom + (numerator * (freqTo - freqFrom))/2.0;
            double sampleVal = Math.sin(2 * Math.PI * (i/2.0) / (sampleRate / currentFreq));
            samples[i] = speakerSide == SpeakerSide.LEFT ? sampleVal : 0;
            samples[i+1] = speakerSide == SpeakerSide.LEFT ? 0 : sampleVal;
        }
        return samples;
    }

    private byte[] convertToPCM(int numSamples, double[] sample) {
        int idx = 0;
        byte[] generatedSnd = new byte[2 * numSamples];
        for (final double dVal : sample) {
            final short val = (short) ((dVal * 32767));
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
        }
        return generatedSnd;
    }

    private enum SpeakerSide {
        LEFT,
        RIGHT
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

class NextBytesGenerator implements Runnable {
    private volatile byte[] value;
    private final int sampleRate;
    private final BinauralBeat binauralBeat;

    private int samplesWritten;
    private int samplesLeft;
    private final int maxSampleCount;
    private final int genNum;
    private boolean finished;

    public NextBytesGenerator(int sampleRate, BinauralBeat binauralBeat, int genNum, int samplesWritten, int samplesLeft, int maxSampleCount) {
        this.sampleRate = sampleRate;
        this.binauralBeat = binauralBeat;
        this.samplesWritten = samplesWritten;
        this.samplesLeft = samplesLeft;
        this.maxSampleCount = maxSampleCount;
        this.finished = false;
        this.genNum = genNum;
    }

    @Override
    public void run() {
        if(binauralBeat.getFrequencyList().size() == genNum){
            value = null;
            finished = true;
            return;
        }
        FrequencyData data = binauralBeat.getFrequencyList().get(genNum);
        int baseFreq = (int)binauralBeat.getBaseFrequency();
        System.out.println((data.getFrequency() + baseFreq) + " | " + ((Float.isNaN(data.getFrequencyTo()) ? data.getFrequency() : data.getFrequencyTo()) + baseFreq));
        value = genTone(data.getFrequency() + baseFreq, (Float.isNaN(data.getFrequencyTo()) ? data.getFrequency() : data.getFrequencyTo()) + baseFreq, (int)data.getDuration());
        finished = true;
    }


    private byte[] genTone(double startFreq, double endFreq, int dur) {
        int totalSamples = dur * sampleRate;
        samplesLeft = totalSamples - samplesWritten;
        int numSamples = Math.min(samplesLeft, maxSampleCount);
        samplesLeft -= numSamples;
        double[] sample = new double[numSamples];
        double currentFreq, numerator;
        for (int i = samplesWritten; i < numSamples+samplesWritten; ++i) {
            numerator = (double) i / (double) totalSamples;
            currentFreq = startFreq + ((numerator * (endFreq - startFreq))/2.0);
            sample[i-samplesWritten] = Math.sin(2 * Math.PI * i / (sampleRate / currentFreq));
        }
        samplesWritten += numSamples;
        if(samplesLeft == 0) { samplesWritten = 0; }
        return convertToPCM(numSamples, sample);
    }

    private byte[] convertToPCM(int numSamples, double[] sample) {
        int idx = 0;
        byte[] generatedSnd = new byte[2 * numSamples];
        for (final double dVal : sample) {
            final short val = (short) ((dVal * 32767));
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
        }
        return generatedSnd;
    }

    public boolean isFinished() {
        return finished;
    }

    public byte[] getValue() {
        return value;
    }

    public int getSamplesWritten() {
        return samplesWritten;
    }

    public int getSamplesLeft() {
        return samplesLeft;
    }

    public enum SpeakerSide {
        LEFT,
        RIGHT
    }
}
