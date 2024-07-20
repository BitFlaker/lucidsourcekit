package com.bitflaker.lucidsourcekit.database.dreamjournal.entities.resulttables;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntryHasTag;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntryTag;

import java.util.List;

public class TagElement {
    @Embedded
    public JournalEntryHasTag journalEntryHasTag;

    @Relation(parentColumn = "tagId", entityColumn = "tagId")
    public List<JournalEntryTag> journalEntryTags;
}
