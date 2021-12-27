package com.bitflaker.lucidsourcekit.general.database.values;

public enum SleepQuality {
    Terrible("TRB"),
    Poor("POR"),
    Great("GRT"),
    Outstanding("OSD"),
    None("");

    private String id;

    SleepQuality(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static SleepQuality getEnum(String id){
        for (SleepQuality enm : SleepQuality.values()) {
            if (enm.getId().equalsIgnoreCase(id)) {
                return enm;
            }
        }
        return null;
    }
}
