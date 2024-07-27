package com.bitflaker.lucidsourcekit.data;

import androidx.annotation.NonNull;

public class AudioBufferPosition {
    int index;
    int bufferPosition;
    long angleLeftSpeaker;
    long angleRightSpeaker;
    boolean finished;

    private AudioBufferPosition(int index, int bufferPosition, long angleLeftSpeaker, long angleRightSpeaker, boolean finished) {
        this.index = index;
        this.bufferPosition = bufferPosition;
        this.angleLeftSpeaker = angleLeftSpeaker;
        this.angleRightSpeaker = angleRightSpeaker;
        this.finished = finished;
    }

    public AudioBufferPosition() {
        index = 0;
        bufferPosition = 0;
        angleLeftSpeaker = 0;
        angleRightSpeaker = 0;
        finished = false;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getBufferPosition() {
        return bufferPosition;
    }

    public void setBufferPosition(int bufferPosition) {
        this.bufferPosition = bufferPosition;
    }

    public long getAngleLeftSpeaker() {
        return angleLeftSpeaker;
    }

    public void setAngleLeftSpeaker(long angleLeftSpeaker) {
        this.angleLeftSpeaker = angleLeftSpeaker;
    }

    public long getAngleRightSpeaker() {
        return angleRightSpeaker;
    }

    public void setAngleRightSpeaker(long angleRightSpeaker) {
        this.angleRightSpeaker = angleRightSpeaker;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    @NonNull
    public AudioBufferPosition clone() {
        return new AudioBufferPosition(index, bufferPosition, angleLeftSpeaker, angleRightSpeaker, finished);
    }
}
