package com.bitflaker.lucidsourcekit.notification;

import com.bitflaker.lucidsourcekit.database.notifications.entities.NotificationCategory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class NotificationOrderManager {
    private final static long MIN_NOTIFICATION_FUTURE_TIME = 60 * 1000;
    private final List<NotificationScheduleData> scheduleData;
    private final List<Long> markers;
    private boolean hasNotifications;

    public NotificationOrderManager() {
        scheduleData = new ArrayList<>();
        markers = new ArrayList<>();
        hasNotifications = false;
    }

    public static NotificationOrderManager load(List<NotificationCategory> notificationCategories) {
        NotificationOrderManager notOrdMan = new NotificationOrderManager();
        for (NotificationCategory cat : notificationCategories) {
            if(cat.isEnabled() && cat.getDailyNotificationCount() > 0) {
                notOrdMan.scheduleNotificationTimeSpan(cat.getTimeFrom(), cat.getTimeTo(), cat.getDailyNotificationCount(), cat.getId());
            }
        }
        return notOrdMan;
    }

    public void scheduleNotificationTimeSpan(long timeFrom, long timeTo, int count, String id) {
        if(count > 0) {
            hasNotifications = true;
        }
        insertMarkerIfNonExistent(timeFrom);
        insertMarkerIfNonExistent(timeTo);
        long duration = timeTo - timeFrom;
        long delay = duration / count;
        int itemCounter = 0;
        do {
            long time = timeFrom + (delay * itemCounter) + (delay / 2);
            int idx = scheduleData.size();
            for (int i = 0; i < scheduleData.size(); i++) {
                if(scheduleData.get(i).getTimestamp() > time){
                    idx = i;
                    break;
                }
            }
            scheduleData.add(idx, new NotificationScheduleData(time, id));
            itemCounter++;
        }
        while(itemCounter < count);
    }

    public List<NotificationScheduleData> getScheduledData() {
        List<NotificationScheduleData> finalItems = new ArrayList<>();
        for (int markerIndex = 0; markerIndex < markers.size() - 1; markerIndex++) {
            List<NotificationScheduleData> markerItems = getDataInMarker(markerIndex);
            long markerStart = markers.get(markerIndex);
            long markerEnd = markers.get(markerIndex  + 1);
            long markerDuration = markerEnd - markerStart;
            long inMarkerDelay = markerItems.size() == 0 ? 0 : (markerDuration / markerItems.size());

            for (int i = 0; i < markerItems.size(); i++) {
                long time = markerStart + (inMarkerDelay * i) + (inMarkerDelay / 2);
                finalItems.add(new NotificationScheduleData(time, markerItems.get(i).getId()));
            }
        }
        return finalItems;
    }

    private List<NotificationScheduleData> getDataInMarker(int markerIndex) {
        long markerStart = markers.get(markerIndex);
        long markerEnd = markers.get(markerIndex  + 1);
        boolean isInLastMarker = markers.size() == markerIndex + 1;
        List<NotificationScheduleData> items = new ArrayList<>();
        for (int i = 0; i < scheduleData.size(); i++) {
            long currTS = scheduleData.get(i).getTimestamp();
            if(currTS >= markerStart && currTS < markerEnd || isInLastMarker && (currTS >= markerStart && currTS <= markerEnd)){
                items.add(scheduleData.get(i));
            }
        }
        return items;
    }

    private void insertMarkerIfNonExistent(long marker) {
        if(!markers.contains(marker)){
            int idx = markers.size();
            for (int i = 0; i < markers.size(); i++) {
                if(markers.get(i) > marker){
                    idx = i;
                    break;
                }
            }
            markers.add(idx, marker);
        }
    }

    public NotificationScheduleData getNextNotification() {
        List<NotificationScheduleData> data = getScheduledData();
        if(data.size() == 0) {
            return null;
        }

        Calendar cal = Calendar.getInstance();
        long currTime = cal.get(Calendar.HOUR_OF_DAY) * 60 * 60 * 1000;
        currTime += cal.get(Calendar.MINUTE) * 60 * 1000;
        currTime += cal.get(Calendar.SECOND) * 1000;
        currTime += cal.get(Calendar.MILLISECOND);

        for (int i = 0; i < data.size(); i++) {
            if(data.get(i).getTimestamp() > currTime + MIN_NOTIFICATION_FUTURE_TIME){
                return data.get(i);
            }
        }
        NotificationScheduleData nsd = data.get(0);
        nsd.setNextDay(true);
        return nsd;
    }

    public boolean hasNotifications() {
        return hasNotifications;
    }
}
