package com.bitflaker.lucidsourcekit.alarms;

import com.bitflaker.lucidsourcekit.main.AlarmData;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AlarmTools {
    public static AlarmData getAlarmDataFromItem(AlarmItem alarmAt) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, alarmAt.getAlarmHour());
        cal.set(Calendar.MINUTE, alarmAt.getAlarmMinute());

        List<AlarmData.ActiveDays> activeDays = new ArrayList<>();
        List<Integer> days = alarmAt.getAlarmRepeatWeekdays();

        for (int day : days) {
            activeDays.add(AlarmData.ActiveDays.values()[day-2 < 0 ? AlarmData.ActiveDays.SUNDAY.ordinal() : day-2]);
        }

        AlarmData ad = new AlarmData(alarmAt.getTitle(), cal, activeDays, alarmAt.isActive());
        if(alarmAt.getAlarmId() != -1){
            ad.setAlarmId(alarmAt.getAlarmId());
        }
        return ad;
    }
}
