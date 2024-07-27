package com.bitflaker.lucidsourcekit.data.enums.journalratings;

public enum DreamTypes {
    Nightmare("NTM"),
    SleepParalysis("SPL"),
    FalseAwakening("FAW"),
    Lucid("LCD"),
    Recurring("REC"),
    None("");

    private final String id;

    DreamTypes(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static DreamTypes getEnum(String id) {
        for (DreamTypes enm : DreamTypes.values()) {
            if (enm.getId().equalsIgnoreCase(id)) {
                return enm;
            }
        }
        return DreamTypes.None;
    }
}
