package com.bitflaker.lucidsourcekit.utils;

import com.bitflaker.lucidsourcekit.data.records.NotificationObfuscationCount;

import java.util.HashMap;
import java.util.List;

public class NotificationObfuscationLookup {
    private final List<NotificationObfuscationCount> data;
    private HashMap<String, HashMap<Long, Long>> mappedDataLookup;

    private NotificationObfuscationLookup(List<NotificationObfuscationCount> data) {
        this.data = data;
        processData();
    }

    public static NotificationObfuscationLookup parse(List<NotificationObfuscationCount> data) {
        return new NotificationObfuscationLookup(data);
    }

    private void processData() {
        mappedDataLookup = new HashMap<>();
        for (NotificationObfuscationCount item : data) {
            HashMap<Long, Long> dataItems = mappedDataLookup.getOrDefault(item.notificationCategoryId(), new HashMap<>());
            dataItems.put(item.obfuscationTypeId(), item.messageCount());
            mappedDataLookup.put(item.notificationCategoryId(), dataItems);
        }
    }

    public long getNotificationCount(String notificationCategoryId, long obfuscationTypeId) {
        if(mappedDataLookup.containsKey(notificationCategoryId)) {
            HashMap<Long, Long> obfuscationCounts = mappedDataLookup.get(notificationCategoryId);
            if (obfuscationCounts != null && obfuscationCounts.containsKey(obfuscationTypeId)) {
                Long count = obfuscationCounts.get(obfuscationTypeId);
                return count == null ? 0 : count;
            }
        }
        return 0;
    }
}
