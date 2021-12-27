package com.bitflaker.lucidsourcekit.general.database.values;

public enum DreamMoods {
    Terrible("TRB"),
    Poor("POR"),
    Ok("OKM"),
    Great("GRT"),
    Outstanding("OSD"),
    None("");

    private String id;

    DreamMoods(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static DreamMoods getEnum(String id){
        for (DreamMoods enm : DreamMoods.values()) {
            if (enm.getId().equalsIgnoreCase(id)) {
                return enm;
            }
        }
        return null;
    }
}
