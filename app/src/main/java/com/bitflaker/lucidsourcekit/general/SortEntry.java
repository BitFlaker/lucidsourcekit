package com.bitflaker.lucidsourcekit.general;

public class SortEntry {
    private String sortText;
    private SortBy sortBy;
    private boolean isDescending;

    public SortEntry(String sortText, SortBy sortBy, boolean isDescending) {
        this.sortText = sortText;
        this.sortBy = sortBy;
        this.isDescending = isDescending;
    }

    public String getSortText() {
        return sortText;
    }

    public void setSortText(String sortText) {
        this.sortText = sortText;
    }

    public SortBy getSortBy() {
        return sortBy;
    }

    public void setSortBy(SortBy sortBy) {
        this.sortBy = sortBy;
    }

    public boolean isDescending() {
        return isDescending;
    }

    public void setDescending(boolean descending) {
        isDescending = descending;
    }
}
