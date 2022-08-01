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
    private OnAlarmAdded mAlarmAddedListener;
    private OnAlarmUpdated mAlarmUpdatedListener;
    private OnAlarmsLoaded mAlarmsLoadedListener;
    private boolean isLoaded;

    private AlarmStorage(Context context) {
        alarms = new ArrayList<>();
        db = MainDatabase.getInstance(context);
        isLoaded = false;
        loadAllAlarmsFromDatabase();
    }

    public static AlarmStorage getInstance(Context context) {
        if (instance == null) {
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
            isLoaded = true;
            if(mAlarmsLoadedListener != null){
                mAlarmsLoadedListener.onEvent();
            }
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
            addAlarmIsOnWeekday(alarmId.intValue(), alarmItem.getAlarmRepeatWeekdays(), 0, false);
        });
    }

    private void addAlarmIsOnWeekday(int alarmId, List<Integer> alarmRepeatWeekdays, int i, boolean causesUpdate) {
        if(i == alarmRepeatWeekdays.size()) {
            if(causesUpdate && mAlarmUpdatedListener != null) {
                mAlarmUpdatedListener.onEvent(alarmId);
            }
            return;
        }
        db.getAlarmIsOnWeekdayDao().insert(new AlarmIsOnWeekday(alarmId, alarmRepeatWeekdays.get(i))).subscribe(() -> {
            addAlarmIsOnWeekday(alarmId, alarmRepeatWeekdays, i+1, causesUpdate);
        });
    }

    private void removeAlarmFromDatabase(AlarmItem alarmItem) {
        Alarm alarm = getEntityFromObject(alarmItem, alarmItem.getAlarmId());
        db.getAlarmDao().delete(alarm).subscribe(() -> System.out.println("DELETED"));
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
        if(alarmItem.getAlarmId() != -1){
            alarm.setAlarmId(alarmItem.getAlarmId());
        }

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
            if(alarm.getAlarmId() == alarmId) {
                return alarm.copy();
            }
        }
        return null;
    }

    public void modifyAlarm(AlarmItem alarmItem) {
        for (int i = 0; i < alarms.size(); i++) {
            if(alarms.get(i).getAlarmId() == alarmItem.getAlarmId()){
                alarms.set(i, alarmItem);
                break;
            }
        }
        Alarm alarm = getEntityFromObject(alarmItem);
        db.getAlarmDao().update(alarm).subscribe(() -> {
            db.getAlarmIsOnWeekdayDao().deleteAllFromAlarm(alarm.alarmId).subscribe(() -> {
                addAlarmIsOnWeekday(alarm.alarmId, alarmItem.getAlarmRepeatWeekdays(), 0, true);
            });
        });
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    public interface OnAlarmAdded {
        void onEvent(AlarmItem alarm);
    }

    public void setOnAlarmAddedListener(OnAlarmAdded eventListener) {
        mAlarmAddedListener = eventListener;
    }

    public interface OnAlarmUpdated {
        void onEvent(int alarmId);
    }

    public void setOnAlarmUpdatedListener(OnAlarmUpdated eventListener) {
        mAlarmUpdatedListener = eventListener;
    }

    public interface OnAlarmsLoaded {
        void onEvent();
    }

    public void setOnAlarmsLoadedListener(OnAlarmsLoaded eventListener) {
        mAlarmsLoadedListener = eventListener;
    }
}
