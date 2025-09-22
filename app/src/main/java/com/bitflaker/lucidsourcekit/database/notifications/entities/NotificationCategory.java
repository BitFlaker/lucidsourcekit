package com.bitflaker.lucidsourcekit.database.notifications.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity
public class NotificationCategory {
    @NonNull
    @PrimaryKey
    private final String id;
    @NonNull
    private final String description;
    private long timeFrom;
    private long timeTo;
    private int dailyNotificationCount;
    private boolean isPermanent;
    private boolean isEnabled;

    @Ignore
    private String itemHeading;
    @Ignore
    private String itemDescription;
    @Ignore
    private int drawable;
    @Ignore
    private NotificationCategoryClicked categoryClickedListener;

    public NotificationCategory(@NonNull String id, @NonNull String description, long timeFrom, long timeTo, int dailyNotificationCount, boolean isPermanent, boolean isEnabled) {
        this.id = id;
        this.description = description;
        this.timeFrom = timeFrom;
        this.timeTo = timeTo;
        this.dailyNotificationCount = dailyNotificationCount;
        this.isPermanent = isPermanent;
        this.isEnabled = isEnabled;
    }

    public void setTimeFrom(long timeFrom) {
        this.timeFrom = timeFrom;
    }

    public void setTimeTo(long timeTo) {
        this.timeTo = timeTo;
    }

    public void setDailyNotificationCount(int dailyNotificationCount) {
        this.dailyNotificationCount = dailyNotificationCount;
    }

    public void setPermanent(boolean permanent) {
        isPermanent = permanent;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getDescription() {
        return description;
    }

    public long getTimeFrom() {
        return timeFrom;
    }

    public long getTimeTo() {
        return timeTo;
    }

    public int getDailyNotificationCount() {
        return dailyNotificationCount;
    }

    public boolean isPermanent() {
        return isPermanent;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public static NotificationCategory[] defaultData = new NotificationCategory[] {
            new NotificationCategory("DJR", "Dream journal reminder", 0, 0, 1, false, false),
            new NotificationCategory("RCR", "Reality check reminder", 0, 0, 4, false, false),
            new NotificationCategory("DGR", "Daily goals reminder", 0, 0, 4, false, false),
            new NotificationCategory("CR", "Custom reminder", 0, 0, 1, false, false),
            new NotificationCategory("PN", "Permanent notification", 0, 0, 0, true, false),
    };

    public void setItemHeading(String itemHeading) {
        this.itemHeading = itemHeading;
    }

    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }

    public void setDrawable(int drawable) {
        this.drawable = drawable;
    }

    public void setCategoryClickedListener(NotificationCategoryClicked categoryClickedListener) {
        this.categoryClickedListener = categoryClickedListener;
    }

    public NotificationCategoryClicked getCategoryClickedListener() {
        return categoryClickedListener;
    }

    public String getItemHeading() {
        return itemHeading;
    }

    public String getItemDescription() {
        return itemDescription;
    }

    public int getDrawable() {
        return drawable;
    }

    public interface NotificationCategoryClicked {
        void notificationCategoryClicked();
    }
}
