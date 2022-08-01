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
        alarmIntent = PendingIntent.getBroadcast(applicationContext, Tools.getBroadcastReqCodeFromID(alarmItem.getAlarmId()), intent, PendingIntent.FLAG_UPDATE_CURRENT);

//        alarmItem.getTimesTo();  // TODO: implement with this --> and set repeating depending on if it is only once or has repeat days

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, alarmItem.getAlarmHour());
        calendar.set(Calendar.MINUTE, alarmItem.getAlarmMinute());
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), alarmIntent);
    }
}
