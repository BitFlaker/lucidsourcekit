package com.bitflaker.lucidsourcekit.main.dreamjournal;

import java.util.Calendar;

public class RecordingData {
    private String filepath;
    private Calendar recordingTime;
    private long recordingLength;

    public RecordingData(String filepath) {
        this.filepath = filepath;
    }

    public RecordingData(String filepath, Calendar recordingTime, long recordingLength) {
        this.filepath = filepath;
        this.recordingTime = recordingTime;
        this.recordingLength = recordingLength;
    }

    public String getFilepath() {
        return filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    public Calendar getRecordingTime() {
        return recordingTime;
    }

    public void setRecordingTime(Calendar recordingTime) {
        this.recordingTime = recordingTime;
    }

    public long getRecordingLength() {
        return recordingLength;
    }

    public void setRecordingLength(long recordingLength) {
        this.recordingLength = recordingLength;
    }
}
