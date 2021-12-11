package com.bitflaker.lucidsourcekit.general;

public class StoredSettings {
    public static final String TABLE_NAME = "settings";
    public static final String COLUMN_NAME_PROPERTY = "property";
    public static final String COLUMN_NAME_VALUE = "value";

    private String property;
    private String value;

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
