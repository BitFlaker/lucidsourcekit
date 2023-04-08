package com.bitflaker.lucidsourcekit.notification;

public class NotificationMessage_deprecated {
    private final int id;
    private String message;
    private int obfuscationTypeId;
    private int weight;

    public NotificationMessage_deprecated(int id, String message, int obfuscationTypeId, int weight) {
        this.id = id;
        this.message = message;
        this.obfuscationTypeId = obfuscationTypeId;
        this.weight = weight;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setObfuscationTypeId(int obfuscationTypeId) {
        this.obfuscationTypeId = obfuscationTypeId;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public int getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public int getObfuscationTypeId() {
        return obfuscationTypeId;
    }

    public int getWeight() {
        return weight;
    }
}
