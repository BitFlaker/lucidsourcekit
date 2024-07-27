package com.bitflaker.lucidsourcekit.data.records;

public record NotificationObfuscationCount(
        String notificationCategoryId,
        long obfuscationTypeId,
        long messageCount) {
}