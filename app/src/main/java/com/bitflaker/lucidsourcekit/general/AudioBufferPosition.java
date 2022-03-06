package com.bitflaker.lucidsourcekit.general;

public class AudioBufferPosition {
    int index;
    int bufferPosition;
    long angle1;
    long angle2;
    int counter;
    boolean finished;

    public AudioBufferPosition() {
        index = 0;
        bufferPosition = 0;
        angle1 = 0;
        angle2 = 0;
        counter = 0;
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

    public long getAngle1() {
        return angle1;
    }

    public void setAngle1(long angle1) {
        this.angle1 = angle1;
    }

    public long getAngle2() {
        return angle2;
    }

    public void setAngle2(long angle2) {
        this.angle2 = angle2;
    }

    public int getCounter() {
        return counter;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }
}
