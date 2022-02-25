package com.bitflaker.lucidsourcekit.main.binauralbeats;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import com.bitflaker.lucidsourcekit.main.BinauralBeat;

public class BinauralBeatsPlayer {
    AudioTrack leftEarAudioTrack;
    AudioTrack rightEarAudioTrack;
    BinauralBeat binauralBeat;
    byte[] generatedSnd;
    int sampleRate = 44100;

    public BinauralBeatsPlayer(BinauralBeat binauralBeat) {
        this.binauralBeat = binauralBeat;
    }

    public void play() {
        setupLeftEarPlayer();
        setupRightEarPlayer();

        leftEarAudioTrack.play();
        rightEarAudioTrack.play();

        /*
        // In case the audio has to be changed, the player has to be stopped before writing to it in order to stop the previous sound!

        rightEarAudioTrack.stop();
        genTone(1200, 1300, 9, SpeakerSide.RIGHT);
        rightEarAudioTrack.play();

         */
    }

    private void setupRightEarPlayer() {
        genTone(555, 755, 10, SpeakerSide.RIGHT);
    }

    private void setupLeftEarPlayer() {
        genTone(545, 745, 10, SpeakerSide.LEFT);
    }

    private void genTone(double startFreq, double endFreq, int dur, SpeakerSide speakerSide) {
        int numSamples = dur * sampleRate * 2;
        double[] sample = new double[numSamples];
        double currentFreq, numerator;
        for (int i = 0; i < numSamples; i+=2) {
            numerator = (double) (i/2.0) / (double) (numSamples/2.0);
            currentFreq = startFreq + (numerator * (endFreq - startFreq))/2.0;
            double sampleVal = Math.sin(2 * Math.PI * (i/2.0) / (sampleRate / currentFreq));
            if(speakerSide == SpeakerSide.LEFT) {
                sample[i] = sampleVal;
                sample[i+1] = 0;
            }
            else {
                sample[i] = 0;
                sample[i+1] = sampleVal;
            }
        }
        convertToPCM(numSamples, sample);
        if(speakerSide == SpeakerSide.LEFT){
            leftEarAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length, AudioTrack.MODE_STATIC);
            leftEarAudioTrack.write(generatedSnd, 0, generatedSnd.length);
        }
        else {
            rightEarAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length, AudioTrack.MODE_STATIC);
            rightEarAudioTrack.write(generatedSnd, 0, generatedSnd.length);
        }
    }

    private void convertToPCM(int numSamples, double[] sample) {
        int idx = 0;
        generatedSnd = new byte[2 * numSamples];
        for (final double dVal : sample) {
            final short val = (short) ((dVal * 32767));
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
        }
    }

    private enum SpeakerSide {
        LEFT,
        RIGHT
    }
}
