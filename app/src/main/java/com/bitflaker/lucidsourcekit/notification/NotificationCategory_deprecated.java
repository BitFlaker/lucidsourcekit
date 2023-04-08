package com.bitflaker.lucidsourcekit.notification;

public class NotificationCategory_deprecated {
    private int categoryDrawable;
    private String categoryHeading;
    private String categoryDescription;
    private int notificationCount;
    private boolean isActive;
    private NotificationCategoryClicked categoryClickedListener;

    public NotificationCategory_deprecated() {

    }

    public NotificationCategory_deprecated(int categoryDrawable, String categoryHeading, String categoryDescription, int notificationCount, boolean isActive, NotificationCategoryClicked categoryClickedListener) {
        this.categoryDrawable = categoryDrawable;
        this.categoryHeading = categoryHeading;
        this.categoryDescription = categoryDescription;
        this.notificationCount = notificationCount;
        this.isActive = isActive;
        this.categoryClickedListener = categoryClickedListener;
    }

    public int getCategoryDrawable() {
        return categoryDrawable;
    }

    public void setCategoryDrawable(int categoryDrawable) {
        this.categoryDrawable = categoryDrawable;
    }

    public String getCategoryHeading() {
        return categoryHeading;
    }

    public void setCategoryHeading(String categoryHeading) {
        this.categoryHeading = categoryHeading;
    }

    public String getCategoryDescription() {
        return categoryDescription;
    }

    public void setCategoryDescription(String categoryDescription) {
        this.categoryDescription = categoryDescription;
    }

    public int getNotificationCount() {
        return notificationCount;
    }

    public void setNotificationCount(int notificationCount) {
        this.notificationCount = notificationCount;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public NotificationCategoryClicked getCategoryClickedListener() {
        return categoryClickedListener;
    }

    public void setCategoryClickedListener(NotificationCategoryClicked categoryClickedListener) {
        this.categoryClickedListener = categoryClickedListener;
    }

    public interface NotificationCategoryClicked {
        void notificationCategoryClicked();
    }
}
