package com.bitflaker.lucidsourcekit.alarms;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.bitflaker.lucidsourcekit.general.Tools;
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

    public static void scheduleAlarm(Context applicationContext, AlarmItem alarmItem) {
        PendingIntent alarmIntent;
        AlarmManager alarmManager = (AlarmManager) applicationContext.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(applicationContext, AlarmReceiverManager.class);
        intent.putExtra("ALARM_ID", alarmItem.getAlarmId());
        List<AlarmTimeSpan> timeSpans = alarmItem.getTimesTo();

//        if(alarmItem.getAlarmRepeatWeekdays().size() == 0) {
            alarmIntent = PendingIntent.getBroadcast(applicationContext, Tools.getBroadcastReqCodeFromID(alarmItem.getAlarmId(), -1), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeSpans.get(0).getMillisTimeStamp(), alarmIntent);
//        }
//        else {
//            List<Integer> activeDays = alarmItem.getActiveDaysSorted();
//            for (int i = 0; i < activeDays.size(); i++) {
//                alarmIntent = PendingIntent.getBroadcast(applicationContext, Tools.getBroadcastReqCodeFromID(alarmItem.getAlarmId(), activeDays.get(i)), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
//                 TODO replace setRepeating with multiple setExact calls as repeating alarms are always inexact!
//                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, timeSpans.get(i).getMillisTimeStamp(), 7*24*60*60*1000, alarmIntent);
//            }
//        }
    }

    public static void cancelAlarm(Context applicationContext, AlarmItem alarmItem) {
        PendingIntent alarmIntent;
        AlarmManager alarmManager = (AlarmManager) applicationContext.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(applicationContext, AlarmReceiverManager.class);
        intent.putExtra("ALARM_ID", alarmItem.getAlarmId());

//        if(alarmItem.getAlarmRepeatWeekdays().size() == 0) {
            alarmIntent = PendingIntent.getBroadcast(applicationContext, Tools.getBroadcastReqCodeFromID(alarmItem.getAlarmId(), -1), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            alarmManager.cancel(alarmIntent);
//        }
//        else {
//            List<Integer> activeDays = alarmItem.getActiveDaysSorted();
//            for (int i = 0; i < activeDays.size(); i++) {
//                alarmIntent = PendingIntent.getBroadcast(applicationContext, Tools.getBroadcastReqCodeFromID(alarmItem.getAlarmId(), activeDays.get(i)), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
//                alarmManager.cancel(alarmIntent);
//            }
//        }
    }
}
