package com.bitflaker.lucidsourcekit.data.records;

import com.bitflaker.lucidsourcekit.data.enums.SortBy;

public record SortEntry(String sortText, SortBy sortBy, boolean isDescending) {

}
