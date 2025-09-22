package com.bitflaker.lucidsourcekit.database.notifications.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.bitflaker.lucidsourcekit.main.notification.NotificationMessageModel;

@Entity(foreignKeys = {
        @ForeignKey(entity = NotificationCategory.class,
            parentColumns = "id",
            childColumns = "notificationCategoryId",
            onDelete = ForeignKey.CASCADE)
    },
    indices = { @Index("notificationCategoryId") }
)
public class NotificationMessage extends NotificationMessageModel {
    @PrimaryKey(autoGenerate = true)
    private int id;
    @NonNull
    private final String notificationCategoryId;
    @NonNull
    private String message;
    private int weight;

    public NotificationMessage(int id, @NonNull String notificationCategoryId, @NonNull String message, int weight) {
        this.id = id;
        this.notificationCategoryId = notificationCategoryId;
        this.message = message;
        this.weight = weight;
    }

    @Ignore
    public NotificationMessage(@NonNull String notificationCategoryId, @NonNull String message, int weight) {
        this.notificationCategoryId = notificationCategoryId;
        this.message = message;
        this.weight = weight;
    }

    @Ignore
    public NotificationMessage(@NonNull String notificationCategoryId) {
        this.notificationCategoryId = notificationCategoryId;
        this.message = "";
    }

    public void setMessage(@NonNull String message) {
        this.message = message;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    @Override
    public int getId() {
        return id;
    }

    @NonNull
    public String getMessage() {
        return message;
    }

    public int getWeight() {
        return weight;
    }

    @NonNull
    public String getNotificationCategoryId() {
        return notificationCategoryId;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public int getType() {
        return 0;
    }
}
