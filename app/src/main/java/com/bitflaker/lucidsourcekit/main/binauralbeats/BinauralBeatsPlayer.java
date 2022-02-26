package com.bitflaker.lucidsourcekit.main.binauralbeats;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import androidx.annotation.NonNull;

import com.bitflaker.lucidsourcekit.charts.FrequencyData;
import com.bitflaker.lucidsourcekit.main.BinauralBeat;

import java.util.Arrays;

public class BinauralBeatsPlayer {
    private AudioTrack leftEarAudioTrack;
    private AudioTrack rightEarAudioTrack;
    private final BinauralBeat binauralBeat;
    private final int sampleRate = 44100;
    private OnTrackProgress mProgressListener;
    private boolean playing;
    private Thread leftEarTrackThread;
    private Thread rightEarTrackThread;
    private int stoppedAtPacket = 0;
    private int wroteCount = 0;

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

        rightEarAudioTrack = generateNewAudioTrack();
        rightEarTrackThread = new Thread(() -> playAndBufferBeats(rightEarAudioTrack, SpeakerSide.RIGHT));
        rightEarTrackThread.start();

        playing = true;
    }

    public void pause() {
        playing = false;
        leftEarAudioTrack.pause();
        rightEarAudioTrack.pause();
    }

    public void resume() {
        playing = true;
        leftEarTrackThread = new Thread(() -> playAndBufferBeats(leftEarAudioTrack, SpeakerSide.LEFT));
        leftEarTrackThread.start();
        rightEarTrackThread = new Thread(() -> playAndBufferBeats(rightEarAudioTrack, SpeakerSide.RIGHT));
        rightEarTrackThread.start();
    }

    public boolean isPlaying() {
        return playing;
    }

    public boolean isInitialized() {
        return leftEarAudioTrack != null && rightEarAudioTrack != null;
    }

    @NonNull
    private AudioTrack generateNewAudioTrack() {
        int channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat);
        return new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channelConfig, audioFormat, bufferSize, AudioTrack.MODE_STREAM);
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
        double[] samples = generateNextTone(speakerSide, stoppedAtPacket);
        byte[] pcm = convertToPCM(samples.length, samples);
        track.play();
        byte[] newC = Arrays.copyOfRange(pcm, wroteCount, pcm.length);
        int wrote = track.write(newC, 0, newC.length, AudioTrack.WRITE_BLOCKING);
        if(speakerSide == SpeakerSide.LEFT) {
            System.out.println("1) Packet " + stoppedAtPacket + " finished with ratio " + (wrote+wroteCount)/(double)pcm.length);
            if(!isPlaying()){
                System.out.println("STOPPED at packet: " + stoppedAtPacket);
                wroteCount += wrote;
                return;
            }
            else { wroteCount = 0; }
        }
        for (int i = stoppedAtPacket+1; i < binauralBeat.getFrequencyList().size(); i++){
            samples = generateNextTone(speakerSide, i);
            pcm = convertToPCM(samples.length, samples);
            wrote = track.write(pcm, 0, pcm.length, AudioTrack.WRITE_BLOCKING);
            if(speakerSide == SpeakerSide.LEFT) {
                System.out.println("2) Packet " + i + " finished with ratio " + wrote/(double)pcm.length);
                if(!isPlaying()) {
                    stoppedAtPacket = i;
                    wroteCount = wrote;
                    System.out.println("STOPPED at packet: " + stoppedAtPacket);
                    break;
                }
            }
        }
    }

    private double[] generateNextTone(SpeakerSide speakerSide, int j) {
        FrequencyData current = binauralBeat.getFrequencyList().get(j);
        int freqSide = speakerSide == SpeakerSide.LEFT ? -1 : 1;
        float freqFrom = current.getFrequency() - (binauralBeat.getBaseFrequency()/2.0f*freqSide);
        float freqTo = current.getFrequencyTo() - (binauralBeat.getBaseFrequency()/2.0f*freqSide);
        if(Float.isNaN(freqTo)) { freqTo = freqFrom; }
        int numSamples = (int)(current.getDuration() * sampleRate * 2);
        // TODO handle too large sample count
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
}
