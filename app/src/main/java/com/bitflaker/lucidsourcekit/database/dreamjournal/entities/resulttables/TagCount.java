package com.bitflaker.lucidsourcekit.database.dreamjournal.entities.resulttables;

public class TagCount {
    private String tag;
    private int count;

    public TagCount(String tag, int count) {
        this.tag = tag;
        this.count = count;
    }

    public String getTag() {
        return tag;
    }

    public int getCount() {
        return count;
    }
}
