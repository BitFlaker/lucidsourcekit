package com.bitflaker.lucidsourcekit.general.database;

public class StoredSettings {
    public static final String TABLE_NAME = "settings";
    public static final String PROPERTY = "property";
    public static final String VALUE = "value";

    private final String property;
    private final String value;

    public StoredSettings(String property, String value) {
        this.property = property;
        this.value = value;
    }

    public String getProperty() {
        return property;
    }

    public String getValue() {
        return value;
    }
}
