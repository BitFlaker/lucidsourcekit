package com.bitflaker.lucidsourcekit.main.dreamjournal;

import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntryHasTag;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntryHasType;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntryTag;
import com.bitflaker.lucidsourcekit.general.database.values.DreamClarity;
import com.bitflaker.lucidsourcekit.general.database.values.DreamMoods;
import com.bitflaker.lucidsourcekit.general.database.values.DreamTypes;
import com.bitflaker.lucidsourcekit.general.database.values.SleepQuality;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class JournalInMemory {
    private int entryId;
    private Calendar time;
    private String title;
    private String description;
    private List<RecordingData> audioRecordings;
    private List<String> tags;
    private String dreamMood;
    private String sleepQuality;
    private String dreamClarity;
    private boolean isNightmare;
    private boolean isParalysis;
    private boolean isLucid;
    private boolean isRecurring;
    private boolean isFalseAwakening;
    private EditMode editMode;
    private EntryType entryType;

    public JournalInMemory() {
        entryId = 0;
        time = Calendar.getInstance();
        title = "";
        description = "";
        audioRecordings = new ArrayList<>();
        tags = new ArrayList<>();
        dreamMood = DreamMoods.values()[0].getId();
        sleepQuality = SleepQuality.values()[0].getId();
        dreamClarity = DreamClarity.values()[0].getId();
        isNightmare = false;
        isParalysis = false;
        isLucid = false;
        isRecurring = false;
        isFalseAwakening = false;
        editMode = EditMode.CREATE;
        entryType = EntryType.PLAIN_TEXT;
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

    public List<RecordingData> getAudioRecordings() {
        return audioRecordings;
    }

    public void setAudioRecordings(List<RecordingData> audioRecordings) {
        this.audioRecordings = audioRecordings;
    }

    public void addAudioRecording(RecordingData audioRecording) {
        this.audioRecordings.add(audioRecording);
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getDreamMood() {
        return dreamMood;
    }

    public void setDreamMood(String dreamMood) {
        this.dreamMood = dreamMood;
    }

    public String getSleepQuality() {
        return sleepQuality;
    }

    public void setSleepQuality(String sleepQuality) {
        this.sleepQuality = sleepQuality;
    }

    public String getDreamClarity() {
        return dreamClarity;
    }

    public void setDreamClarity(String dreamClarity) {
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

    public void removeTag(String tag) {
        tags.remove(tag);
    }

    public void addTag(String tag) {
        tags.add(tag);
    }

    public void removeAudioRecording(RecordingData audioRecording) {
        this.audioRecordings.remove(audioRecording);
    }

    public List<JournalEntryHasType> getDreamTypes(int entryId) {
        List<String> dreamTypes = new ArrayList<>();
        if(isNightmare) { dreamTypes.add(DreamTypes.Nightmare.getId()); }
        if(isParalysis) { dreamTypes.add(DreamTypes.SleepParalysis.getId()); }
        if(isFalseAwakening) { dreamTypes.add(DreamTypes.FalseAwakening.getId()); }
        if(isLucid) { dreamTypes.add(DreamTypes.Lucid.getId()); }
        if(isRecurring) { dreamTypes.add(DreamTypes.Recurring.getId()); }
        List<JournalEntryHasType> jeht = new ArrayList<>();
        for (String type : dreamTypes) {
            jeht.add(new JournalEntryHasType(entryId, type));
        }
        return jeht;
    }

    public List<JournalEntryTag> getJournalEntryTag() {
        List<JournalEntryTag> jet = new ArrayList<>();
        for (String tag : tags) {
            jet.add(new JournalEntryTag(tag));
        }
        return jet;
    }

    public List<JournalEntryHasTag> getJournalEntryHasTag(int entryId, List<JournalEntryTag> journalEntryTags) {
        List<JournalEntryHasTag> journalEntryHasTags = new ArrayList<>();
        for (int i = 0; i < journalEntryTags.size(); i++) {
            journalEntryHasTags.add(new JournalEntryHasTag(entryId, journalEntryTags.get(i).tagId));
        }
        return journalEntryHasTags;
    }

    public int getEntryId() {
        return entryId;
    }

    public void setEntryId(int entryId) {
        this.entryId = entryId;
    }

    public EntryType getEntryType() {
        return entryType;
    }

    public void setEntryType(EntryType entryType) {
        this.entryType = entryType;
    }

    public EditMode getEditMode() {
        return editMode;
    }

    public void setEditMode(EditMode editMode) {
        this.editMode = editMode;
    }

    public enum EditMode {
        CREATE,
        EDIT
    }

    public enum EntryType {
        PLAIN_TEXT,
        FORMS_TEXT
    }
}
