package com.bitflaker.lucidsourcekit.main.dreamjournal;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class JournalInMemory {
    private Calendar time;
    private String title;
    private String description;
    private List<String> audioRecordingFiles;
    private List<String> tags;
    private int dreamMood;
    private int sleepQuality;
    private int dreamClarity;
    private boolean isNightmare;
    private boolean isParalysis;
    private boolean isLucid;
    private boolean isRecurring;
    private boolean isFalseAwakening;

    public JournalInMemory(){
        time = Calendar.getInstance();
        title = "";
        description = "";
        audioRecordingFiles = new ArrayList<>();
        tags = new ArrayList<>();
        dreamMood = 0;
        sleepQuality = 0;
        dreamClarity = 0;
        isNightmare = false;
        isParalysis = false;
        isLucid = false;
        isRecurring = false;
        isFalseAwakening = false;
    }

    public Calendar getTime() {
        return time;
    }

    public void setTime(Calendar time) {
        this.time = time;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getAudioRecordingFiles() {
        return audioRecordingFiles;
    }

    public void setAudioRecordingFiles(List<String> audioRecordingFiles) {
        this.audioRecordingFiles = audioRecordingFiles;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public int getDreamMood() {
        return dreamMood;
    }

    public void setDreamMood(int dreamMood) {
        this.dreamMood = dreamMood;
    }

    public int getSleepQuality() {
        return sleepQuality;
    }

    public void setSleepQuality(int sleepQuality) {
        this.sleepQuality = sleepQuality;
    }

    public int getDreamClarity() {
        return dreamClarity;
    }

    public void setDreamClarity(int dreamClarity) {
        this.dreamClarity = dreamClarity;
    }

    public boolean isNightmare() {
        return isNightmare;
    }

    public void setNightmare(boolean nightmare) {
        isNightmare = nightmare;
    }

    public boolean isParalysis() {
        return isParalysis;
    }

    public void setParalysis(boolean paralysis) {
        isParalysis = paralysis;
    }

    public boolean isLucid() {
        return isLucid;
    }

    public void setLucid(boolean lucid) {
        isLucid = lucid;
    }

    public boolean isRecurring() {
        return isRecurring;
    }

    public void setRecurring(boolean recurring) {
        isRecurring = recurring;
    }

    public boolean isFalseAwakening() {
        return isFalseAwakening;
    }

    public void setFalseAwakening(boolean falseAwakening) {
        isFalseAwakening = falseAwakening;
    }
}
