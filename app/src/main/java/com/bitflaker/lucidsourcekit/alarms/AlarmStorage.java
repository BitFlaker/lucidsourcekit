package com.bitflaker.lucidsourcekit.alarms;

import android.content.Context;
import android.net.Uri;

import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.alarms.entities.Alarm;
import com.bitflaker.lucidsourcekit.database.alarms.entities.AlarmIsOnWeekday;

import java.util.ArrayList;
import java.util.List;

public class AlarmStorage {
    private static AlarmStorage instance;
    private List<AlarmItem> alarms;
    private MainDatabase db;
    private boolean finishedLoading = false;
    private OnAlarmAdded mAlarmAddedListener;

    private AlarmStorage(Context context) {
        alarms = new ArrayList<>();
        db = MainDatabase.getInstance(context);
        loadAllAlarmsFromDatabase();
    }

    public static AlarmStorage getInstance(Context context) {
        if (instance == null){
            instance = new AlarmStorage(context);
        }
        return instance;
    }

    private void loadAllAlarmsFromDatabase() {
        db.getAlarmDao().getAll().subscribe((alarmsList, throwable) -> {
            finishFullDataGatheringForAlarm(alarmsList, 0);
        });
    }

    private void finishFullDataGatheringForAlarm(List<Alarm> alarmsList, int i) {
        if(i == alarmsList.size()) {
            // TODO: maybe give event when finished with loading to prevent displaying errors
            finishedLoading = true;
            return;
        }
        db.getAlarmIsOnWeekdayDao().getAllForAlarm(alarmsList.get(i).alarmId).subscribe((alarmIsOnWeekdays, throwable) -> {
            AlarmItem alarmItem = getObjectFromEntity(alarmsList.get(i));
            for (AlarmIsOnWeekday alarmIsOnWeekday : alarmIsOnWeekdays) {
                alarmItem.addAlarmRepeatWeekdays(alarmIsOnWeekday.weekdayId);
            }
            alarms.add(alarmItem);
            finishFullDataGatheringForAlarm(alarmsList, i+1);
        });
    }

    private AlarmItem getObjectFromEntity(Alarm alarm) {
        AlarmItem alarmItem = new AlarmItem(
                alarm.title,
                alarm.bedtimeHour,
                alarm.bedtimeMinute,
                alarm.alarmHour,
                alarm.alarmMinute,
                new ArrayList<>(),
                AlarmItem.AlarmToneType.values()[alarm.alarmToneType],
                Uri.parse(alarm.alarmUri),
                alarm.alarmVolume,
                alarm.alarmVolumeIncreaseMinutes,
                alarm.alarmVolumeIncreaseSeconds,
                alarm.vibrate,
                alarm.useFlashlight
        );
        alarmItem.setAlarmId(alarm.alarmId);
        alarmItem.setActive(alarm.isActive);
        return alarmItem;
    }

    public void addAlarm(AlarmItem alarmItem) {
        writeAlarmToDatabase(alarmItem);
    }

    public void removeAlarm(AlarmItem alarmItem) {
        removeAlarmFromDatabase(alarmItem);
//        alarms.remove(alarmItem);
    }

    public AlarmItem getAlarmAt(int index){
        return alarms.get(index);
    }

    public void setAlarmActive(int alarmId, boolean active){
        for (AlarmItem alarm : alarms) {
            if (alarm.getAlarmId() == alarmId) {
                alarm.setActive(active);
                break;
            }
        }
    }

    private void writeAlarmToDatabase(AlarmItem alarmItem) {
        Alarm alarm = getEntityFromObject(alarmItem);
        db.getAlarmDao().insert(alarm).subscribe((alarmId, throwable) -> {
            alarmItem.setAlarmId(alarmId.intValue());
            alarms.add(alarmItem);
            if(mAlarmAddedListener != null) {
                mAlarmAddedListener.onEvent(alarmItem);
            }
            addAlarmIsOnWeekday(alarmId.intValue(), alarmItem.getAlarmRepeatWeekdays(), 0);
        });
    }

    private void addAlarmIsOnWeekday(int alarmId, List<Integer> alarmRepeatWeekdays, int i) {
        if(i == alarmRepeatWeekdays.size()) { return; }
        db.getAlarmIsOnWeekdayDao().insert(new AlarmIsOnWeekday(alarmId, alarmRepeatWeekdays.get(i))).subscribe(() -> {
            addAlarmIsOnWeekday(alarmId, alarmRepeatWeekdays, i+1);
        });
    }

    private void removeAlarmFromDatabase(AlarmItem alarmItem) {
        Alarm alarm = getEntityFromObject(alarmItem, alarmItem.getAlarmId());
//        db.getAlarmDao().getAll().subscribe((alarms1, throwable) -> {
//            Alarm al = alarms1.get(0);
        db.getAlarmDao().delete(alarm).subscribe(() -> {
            // JUST HERE FOR TESTING PURPOSES
            db.getAlarmIsOnWeekdayDao().getAll().subscribe((alarmIsOnWeekdays, throwable2) -> {
                System.out.println(alarmIsOnWeekdays.size());
            });
        });
//        });
    }

    private Alarm getEntityFromObject(AlarmItem alarmItem) {
        Alarm alarm = new Alarm(
                alarmItem.getTitle(),
                alarmItem.getBedtimeHour(),
                alarmItem.getBedtimeMinute(),
                alarmItem.getAlarmHour(),
                alarmItem.getAlarmMinute(),
                alarmItem.getAlarmToneType().ordinal(),
                alarmItem.getAlarmUri().toString(),
                alarmItem.getAlarmVolume(),
                alarmItem.getAlarmVolumeIncreaseMinutes(),
                alarmItem.getAlarmVolumeIncreaseSeconds(),
                alarmItem.isVibrate(),
                alarmItem.isUseFlashlight(),
                alarmItem.isActive()
        );

        return alarm;
    }

    private Alarm getEntityFromObject(AlarmItem alarmItem, int alarmId) {
        Alarm alarm = getEntityFromObject(alarmItem);
        alarm.setAlarmId(alarmId);
        return alarm;
    }

    public int size() {
        return alarms.size();
    }

    public boolean isFinishedLoading() {
        return finishedLoading;
    }

    public void removedAlarmIds(List<Integer> alarmsToDelete) {
        int i = 0;
        int remCount = 0;
        while(i - remCount < alarms.size()){
            if(alarmsToDelete.contains(alarms.get(i - remCount).getAlarmId())){
                alarms.remove(i - remCount);
                remCount++;
            }
            i++;
        }
    }

    public AlarmItem getAlarmItemWithId(int alarmId) {
        for (AlarmItem alarm : alarms) {
            if(alarm.getAlarmId() == alarmId){
                return alarm;
            }
        }
        return null;
    }

    public void modifyAlarm(AlarmItem alarmItem) {

    }

    public interface OnAlarmAdded {
        void onEvent(AlarmItem alarm);
    }

    public void setOnAlarmAddedListener(OnAlarmAdded eventListener) {
        mAlarmAddedListener = eventListener;
    }
}
