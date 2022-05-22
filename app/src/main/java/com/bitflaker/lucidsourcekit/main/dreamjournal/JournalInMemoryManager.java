package com.bitflaker.lucidsourcekit.main.dreamjournal;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JournalInMemoryManager {
    private static JournalInMemoryManager instance;
    private List<String> loadedJournalEntryIds;
    private List<JournalInMemory> loadedJournalEntries;

    private JournalInMemoryManager() {
        loadedJournalEntryIds = new ArrayList<>();
        loadedJournalEntries = new ArrayList<>();
    }

    public static JournalInMemoryManager getInstance() {
        if(instance == null){
            instance = new JournalInMemoryManager();
        }
        return instance;
    }

    public String newEntry() {
        String id = UUID.randomUUID().toString();
        loadedJournalEntryIds.add(id);
        loadedJournalEntries.add(new JournalInMemory());
        return id;
    }

    public JournalInMemory getEntry(String id) {
        if(loadedJournalEntryIds.contains(id)) {
            int index = loadedJournalEntryIds.indexOf(id);
            return loadedJournalEntries.get(index);
        }
        return null;
    }

    public boolean unloadEntry(String id) {
        if(loadedJournalEntryIds.contains(id)) {
            int index = loadedJournalEntryIds.indexOf(id);
            loadedJournalEntryIds.remove(index);
            loadedJournalEntries.remove(index);
            return true;
        }
        return false;
    }
}
