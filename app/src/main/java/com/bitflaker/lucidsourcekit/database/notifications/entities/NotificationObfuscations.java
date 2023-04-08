package com.bitflaker.lucidsourcekit.database.notifications.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class NotificationObfuscations {
    @PrimaryKey
    public int obfuscationTypeId;
    @NonNull
    public String description;

    public NotificationObfuscations(int obfuscationTypeId, @NonNull String description) {
        this.obfuscationTypeId = obfuscationTypeId;
        this.description = description;
    }

    public static NotificationObfuscations[] populateData() {
        return new NotificationObfuscations[] {
                new NotificationObfuscations(0, "Transparent"),
                new NotificationObfuscations(1, "Neutral"),
                new NotificationObfuscations(2, "Obfuscated")
        };
    }
}
