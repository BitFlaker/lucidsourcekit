package com.bitflaker.lucidsourcekit.database.notifications.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(foreignKeys = {
        @ForeignKey(entity = NotificationObfuscations.class,
            parentColumns = "obfuscationTypeId",
            childColumns = "obfuscationTypeId",
            onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = NotificationCategory.class,
            parentColumns = "id",
            childColumns = "notificationCategoryId",
            onDelete = ForeignKey.CASCADE)
    },
    indices = { @Index("notificationCategoryId"), @Index("obfuscationTypeId") }
)
public class NotificationMessage {
    @PrimaryKey(autoGenerate = true)
    private int id;
    @NonNull
    private final String notificationCategoryId;
    @NonNull
    private String message;
    private int obfuscationTypeId;
    private int weight;

    public NotificationMessage(int id, @NonNull String notificationCategoryId, @NonNull String message, int obfuscationTypeId, int weight) {
        this.id = id;
        this.notificationCategoryId = notificationCategoryId;
        this.message = message;
        this.obfuscationTypeId = obfuscationTypeId;
        this.weight = weight;
    }

    @Ignore
    public NotificationMessage(@NonNull String notificationCategoryId, @NonNull String message, int obfuscationTypeId, int weight) {
        this.notificationCategoryId = notificationCategoryId;
        this.message = message;
        this.obfuscationTypeId = obfuscationTypeId;
        this.weight = weight;
    }

    public void setMessage(@NonNull String message) {
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

    @NonNull
    public String getMessage() {
        return message;
    }

    public int getObfuscationTypeId() {
        return obfuscationTypeId;
    }

    public int getWeight() {
        return weight;
    }

    @NonNull
    public String getNotificationCategoryId() {
        return notificationCategoryId;
    }

    public void setId(int messageId) {
        this.id = messageId;
    }
}
