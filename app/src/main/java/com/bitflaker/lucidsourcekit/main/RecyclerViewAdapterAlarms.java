package com.bitflaker.lucidsourcekit.main;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.alarms.updated.AlarmHandler;
import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.alarms.updated.entities.StoredAlarm;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

public class RecyclerViewAdapterAlarms extends RecyclerView.Adapter<RecyclerViewAdapterAlarms.MainViewHolderAlarms> {
    private final List<MainViewHolderAlarms> loadedItems = new ArrayList<>();
    private final List<Integer> loadedPositions = new ArrayList<>();

    private final static String[] weekdayShorts = new String[] { "Su", "Mo", "Tu", "We", "Th", "Fr", "Sa" };
    private OnSelectionModeStateChanged mSelectionModeStateChangedListener;
    private OnEntryActiveStateChanged mEntryActiveStateChangedListener;
    private OnEntryClicked mOnEntryClickedListener;
    private final Context context;
    private boolean isInSelectionMode = false;
    private List<StoredAlarm> storedAlarms;
    private List<Long> selectedAlarmIds;
    private List<Integer> selectedIndexes;
    private boolean selectionModeEnabled = true;
    private boolean controlsVisible = true;
    private boolean elevatedBackground = false;

    public RecyclerViewAdapterAlarms(Context context, List<StoredAlarm> storedAlarms) {
        this.context = context;
        this.storedAlarms = storedAlarms;
        selectedAlarmIds = new ArrayList<>();
        selectedIndexes = new ArrayList<>();
    }

    @NonNull
    @Override
    public MainViewHolderAlarms onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.alarm_entry, parent, false);
        return new MainViewHolderAlarms(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MainViewHolderAlarms holder, int position) {
        if(!controlsVisible){
            holder.hideControls();
        }
        StoredAlarm alarm = storedAlarms.get(position);
        loadedItems.add(holder);
        loadedPositions.add(position);
        holder.title.setText(alarm.title);

        long alarmHours = TimeUnit.MILLISECONDS.toHours(alarm.alarmTimestamp);
        long alarmMinutes = TimeUnit.MILLISECONDS.toMinutes(alarm.alarmTimestamp) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(alarm.alarmTimestamp));
        boolean isPmAm = !android.text.format.DateFormat.is24HourFormat(context);//tString.toUpperCase().contains("PM") || tString.toUpperCase().contains("AM");
        String alarmTime = alarmHours + ":" + alarmMinutes;
        SimpleDateFormat clock24H = new SimpleDateFormat("HH:mm");
        SimpleDateFormat clock12H = new SimpleDateFormat("hh:mm");
        try {
            Date date = clock24H.parse(alarmTime);
            alarmTime = isPmAm ? clock12H.format(date) : alarmTime;
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }

        holder.timePrimary.setText(alarmTime);
        holder.timeSecondary.setText(isPmAm ? (alarmHours < 12 ? " AM" : " PM") : "");

//        holder.active.setOnCheckedChangeListener(null);
        if(alarm.isAlarmActive && alarm.requestCodeActiveAlarm == -1){
            System.out.println("INCONSISTENCY DETECTED! ALARM IS ACTIVE BUT REQCODEACTIVEALARM => -1");
        }
        holder.active.setChecked(alarm.isAlarmActive && alarm.requestCodeActiveAlarm != -1);
        holder.active.setOnClickListener(e -> {
            SwitchMaterial currSwitch = ((SwitchMaterial) e);
            alarm.isAlarmActive = currSwitch.isChecked();
            if(currSwitch.isChecked()){
                // get the current weekday and reduce it by 1 to get the index
                // because the week starts with SUNDAY [which has id 1]
                int index = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1;
                AlarmHandler.scheduleAlarmRepeatedlyAt(context.getApplicationContext(), alarm.alarmId, getMillisUntilMidnight() + alarm.alarmTimestamp, alarm.pattern, index, 1000 * 60 * 60 * 24).subscribe(() -> {
                    updateStoredAlarmFromDatabase(position, alarm);
                }).dispose();
            }
            else {
                // cancel the alarm
                AlarmHandler.cancelRepeatingAlarm(context.getApplicationContext(), alarm.alarmId).subscribe(() -> {
                    updateStoredAlarmFromDatabase(position, alarm);
                }).dispose();
            }
            if(mEntryActiveStateChangedListener != null){
                mEntryActiveStateChangedListener.onEvent(storedAlarms.get(position), currSwitch.isChecked());
            }
        });
//        Typeface tfThin = FontHandler.getInstance().getFontByName("sans-serif-thin");
//        Typeface tfNormal = FontHandler.getInstance().getFontByName("sans-serif");
//        // TODO: enable toggle for either not showing disabled days or showing them only slightly
//        for (int i = 0; i < holder.weekdays.size(); i++) {
//            TextView dayT = holder.weekdays.get(i);
//            dayT.setTypeface(tfThin);
//            dayT.setTextColor(Tools.getAttrColor(R.attr.secondaryTextColor, context.getTheme()));
//            dayT.setVisibility(View.GONE);
//        }
//        for (AlarmData.ActiveDays day : alarmData.get(position).getActiveDays()) {
//            TextView dayT = holder.weekdays.get(day.ordinal());
//            dayT.setTypeface(tfNormal);
//            dayT.setTextColor(Tools.getAttrColor(R.attr.primaryTextColor, context.getTheme()));
//            dayT.setVisibility(View.VISIBLE);
//        }
        StringJoiner joiner = new StringJoiner(", ");
        joiner.setEmptyValue("No repeat");
        for (int i = 0; i < alarm.pattern.length; i++) {
            if(alarm.pattern[i]){
                joiner.add(weekdayShorts[i]);
            }
        }
        holder.activeDays.setText(joiner.toString());
        holder.setSelectionModeActive(isInSelectionMode && selectionModeEnabled);
        holder.chkChecked.setOnTouchListener((v, event) -> {
            View par = ((View) v.getParent().getParent());
            event.setLocation(par.getWidth(), 0);
            par.onTouchEvent(event);
            return true;
        });
        holder.chkChecked.setChecked(selectedAlarmIds.contains(alarm.alarmId));
        holder.card.setOnClickListener(e -> {
            if(isInSelectionMode && selectionModeEnabled){
                holder.chkChecked.setChecked(!holder.chkChecked.isChecked());
                if(holder.chkChecked.isChecked()){
                    selectedIndexes.add(position);
                    selectedAlarmIds.add(alarm.alarmId);
                    return;
                }
                selectedIndexes.remove(position);
                selectedAlarmIds.remove(alarm.alarmId);
                if(selectedIndexes.size() == 0){
                    setIsInSelectionMode(false);
                }
                return;
            }
            if(mOnEntryClickedListener != null){
                mOnEntryClickedListener.onEvent(storedAlarms.get(position));
            }
        });
        if(selectionModeEnabled){
            holder.card.setOnLongClickListener(e -> {
                if(!isInSelectionMode){
                    setIsInSelectionMode(true);
                    holder.chkChecked.setChecked(true);
                    selectedIndexes.add(position);
                    selectedAlarmIds.add(alarm.alarmId);
                }
                return true;
            });
        }
        if(elevatedBackground){
            holder.card.setCardBackgroundColor(Tools.getAttrColorStateList(R.attr.slightElevated, context.getTheme()));
        }
    }

    private void updateStoredAlarmFromDatabase(int position, StoredAlarm alarm) {
        MainDatabase.getInstance(context).getStoredAlarmDao().getById(alarm.alarmId).subscribe(storedAlarm -> {
            storedAlarms.set(position, storedAlarm);
        }).dispose();
    }

    private long getMillisSinceMidnight() {
        return Calendar.getInstance().getTimeInMillis() - getMillisUntilMidnight();
    }

    private long getMillisUntilMidnight() {
        Calendar midnightCalendar = Calendar.getInstance();
        midnightCalendar.set(Calendar.HOUR_OF_DAY, 0);
        midnightCalendar.set(Calendar.MINUTE, 0);
        midnightCalendar.set(Calendar.SECOND, 0);
        midnightCalendar.set(Calendar.MILLISECOND, 0);
        return midnightCalendar.getTimeInMillis();
    }

    @Override
    public void onViewRecycled(@NonNull MainViewHolderAlarms holder) {
        super.onViewRecycled(holder);
        loadedPositions.add(holder.getAdapterPosition());
        loadedItems.remove(holder);
    }

    private void setSelectionModeInItems(boolean selMode) {
        if(selectionModeEnabled){
            for (MainViewHolderAlarms al : loadedItems) {
                al.setSelectionModeActive(selMode);
            }
        }
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setData(List<StoredAlarm> storedAlarms) {
        this.storedAlarms = storedAlarms;
        notifyDataSetChanged();
    }

    public List<Long> getSelectedStoredAlarmIds() {
        return selectedAlarmIds;
    }

    @Override
    public int getItemCount() {
        return storedAlarms.size();
    }

    public void setIsInSelectionMode(boolean isInSelectionMode) {
        if(selectionModeEnabled){
            selectedAlarmIds = new ArrayList<>();
            selectedIndexes = new ArrayList<>();
            this.isInSelectionMode = isInSelectionMode;
            setSelectionModeInItems(isInSelectionMode);
            if(mSelectionModeStateChangedListener != null){
                if(isInSelectionMode){
                    mSelectionModeStateChangedListener.onSelectionModeEntered();
                }
                else {
                    mSelectionModeStateChangedListener.onSelectionModeLeft();
                }
            }
        }
    }

    public void removeSelectedAlarmIds() {
        selectedIndexes.sort(Comparator.reverseOrder());
        for (int i = 0; i < selectedIndexes.size(); i++) {
            storedAlarms.remove(selectedIndexes.get(i).intValue());
            notifyItemRemoved(selectedIndexes.get(i));
        }
        setIsInSelectionMode(false);
        loadedPositions.forEach(this::notifyItemChanged);
    }

    public void reloadModifiedAlarmWithId(long alarmId) {
        MainDatabase.getInstance(context).getStoredAlarmDao().getById(alarmId).subscribe(alarm -> {
            int updatedIndex = -1;
            for (int i = 0; i < storedAlarms.size(); i++) {
                if(storedAlarms.get(i).alarmId == alarmId){
                    storedAlarms.set(i, alarm);
                    updatedIndex = i;
                    break;
                }
            }
            if(updatedIndex != -1){
                notifyItemChanged(updatedIndex);
            }
        }).dispose();
    }

    public void loadAddedAlarmWithId(long alarmId) {
        MainDatabase.getInstance(context).getStoredAlarmDao().getById(alarmId).subscribe(alarm -> {
            storedAlarms.add(alarm);
            notifyItemInserted(storedAlarms.size() - 1);
        }).dispose();
    }

    public void alarmWentOff(long alarmTimestamp) {
        MainDatabase.getInstance(context).getActiveAlarmDao().getStoredAlarmByAlarmTime(alarmTimestamp).blockingSubscribe(refreshStoredAlarms -> {
            List<Integer> positions = new ArrayList<>();
            for (int i = 0; i < refreshStoredAlarms.size(); i++) {
                for (int j = 0; j < storedAlarms.size(); j++) {
                    if(storedAlarms.get(j).alarmId == refreshStoredAlarms.get(i).alarmId){
                        positions.add(j);
                        break;
                    }
                }
            }
            for (int i = 0; i < refreshStoredAlarms.size(); i++) {
                storedAlarms.set(positions.get(i), refreshStoredAlarms.get(i));
                notifyItemChanged(positions.get(i));
            }
        });
    }

    public void setSelectionModeEnabled(boolean enabled) {
        selectionModeEnabled = enabled;
    }

    public boolean isSelectionModeEnabled() {
        return selectionModeEnabled;
    }

    public void setControlsVisible(boolean visible) {
        controlsVisible = visible;
    }

    public boolean isControlsVisible() {
        return controlsVisible;
    }

    public void setElevatedBackground(boolean elevatedBackground) {
        this.elevatedBackground = elevatedBackground;
    }

    public boolean isElevatedBackground() {
        return elevatedBackground;
    }

    public static class MainViewHolderAlarms extends RecyclerView.ViewHolder {
        TextView title, timePrimary, timeSecondary, activeDays;
        TextView dayMo, dayTu, dayWe, dayTh, dayFr, daySa, daySu;
        MaterialCardView card;
        SwitchMaterial active;
        List<TextView> weekdays;
        CheckBox chkChecked;
        boolean controlsHidden;

        public MainViewHolderAlarms(@NonNull View itemView) {
            super(itemView);
            controlsHidden = false;
            card = itemView.findViewById(R.id.crd_alarm);
            title = itemView.findViewById(R.id.txt_alarms_title);
            timePrimary = itemView.findViewById(R.id.txt_alarms_time_prim);
            timeSecondary = itemView.findViewById(R.id.txt_alarms_time_sec);
            active = itemView.findViewById(R.id.swt_alarm_active);
            activeDays = itemView.findViewById(R.id.txt_alarms_weekdays_active);
            dayMo = itemView.findViewById(R.id.txt_alarms_week_mo);
            dayTu = itemView.findViewById(R.id.txt_alarms_week_tu);
            dayWe = itemView.findViewById(R.id.txt_alarms_week_we);
            dayTh = itemView.findViewById(R.id.txt_alarms_week_th);
            dayFr = itemView.findViewById(R.id.txt_alarms_week_fr);
            daySa = itemView.findViewById(R.id.txt_alarms_week_sa);
            daySu = itemView.findViewById(R.id.txt_alarms_week_su);
            chkChecked = itemView.findViewById(R.id.chk_alarm_selected);
            weekdays = Arrays.asList(dayMo, dayTu, dayWe, dayTh, dayFr, daySa, daySu);
        }

        public void hideControls(){
            chkChecked.setVisibility(View.GONE);
            active.setVisibility(View.GONE);
            controlsHidden = true;
        }

        public void setSelectionModeActive(boolean isInSelectionMode) {
            if(!controlsHidden){
                if(isInSelectionMode){
                    chkChecked.setVisibility(View.VISIBLE);
                    active.setVisibility(View.GONE);
                }
                else {
                    chkChecked.setChecked(false);
                    chkChecked.setVisibility(View.GONE);
                    active.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    public interface OnEntryClicked {
        void onEvent(StoredAlarm storedAlarm);
    }

    public void setOnEntryClickedListener(OnEntryClicked eventListener) {
        mOnEntryClickedListener = eventListener;
    }

    public interface OnEntryActiveStateChanged {
        void onEvent(StoredAlarm storedAlarm, boolean isActive);
    }

    public void setOnEntryActiveStateChangedListener(OnEntryActiveStateChanged eventListener) {
        mEntryActiveStateChangedListener = eventListener;
    }

    public interface OnSelectionModeStateChanged {
        void onSelectionModeEntered();
        void onSelectionModeLeft();
    }

    public void setOnSelectionModeStateChangedListener(OnSelectionModeStateChanged eventListener) {
        if(selectionModeEnabled) {
            mSelectionModeStateChangedListener = eventListener;
        }
        else {
            throw new IllegalStateException("Setting the listener of the selection state change event is not possible if the selection mode is disabled!");
        }
    }
}
