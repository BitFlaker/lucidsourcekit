package com.bitflaker.lucidsourcekit.notification;

public class NotificationMessageHeading extends NotificationMessageModel {
    private final int obfuscationTypeId;

    public NotificationMessageHeading(int obfuscationTypeId) {
        this.obfuscationTypeId = obfuscationTypeId;
    }

    @Override
    public int getId() {
        return -1;
    }

    @Override
    public int getObfuscationTypeId() {
        return obfuscationTypeId;
    }

    @Override
    public int getType() {
        return 1;
    }
}
