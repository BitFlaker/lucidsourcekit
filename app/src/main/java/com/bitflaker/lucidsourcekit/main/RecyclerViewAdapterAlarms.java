package com.bitflaker.lucidsourcekit.main;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.alarms.AlarmCreator;
import com.bitflaker.lucidsourcekit.alarms.AlarmStorage;
import com.bitflaker.lucidsourcekit.alarms.AlarmTools;
import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RecyclerViewAdapterAlarms extends RecyclerView.Adapter<RecyclerViewAdapterAlarms.MainViewHolderAlarms> {
    private OnEntryClicked mListener;
    private OnSelectionModeStateChanged mSelectionModeStateChangedListener;
    private Context context;
    private List<AlarmData> alarmData;
    private final static String[] weekdayShorts = new String[] { "Mo", "Tu", "We", "Th", "Fr", "Sa", "Su" };
    private boolean isInSelectionMode = false;
    private RecyclerView mRecyclerView;
    private int selectionCounter = 0;
    private List<MainViewHolderAlarms> loadedItems = new ArrayList<>();
    private List<Integer> loadedPositions = new ArrayList<>();

    public RecyclerViewAdapterAlarms(Context context, List<AlarmData> alarmData) {
        this.context = context;
        this.alarmData = alarmData;
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
        loadedItems.add(holder);
        loadedPositions.add(position);
        holder.title.setText(alarmData.get(position).getTitle());
        DateFormat tf = DateFormat.getTimeInstance(DateFormat.SHORT);
        String tString = tf.format(alarmData.get(position).getTime().getTime());
        boolean isPMAM = tString.toUpperCase().contains("PM") || tString.toUpperCase().contains("AM");
        holder.timePrimary.setText(isPMAM ? tString.replace("PM", "").replace("AM", "") : tString);
        holder.timeSecondary.setText(isPMAM ? (tString.toUpperCase().contains("PM") ? "PM" : "AM") : "");
        holder.active.setOnCheckedChangeListener((e, checked) -> {
            alarmData.get(position).setActive(checked);
            MainDatabase.getInstance(context).getAlarmDao().setActiveState(alarmData.get(position).getAlarmId(), checked).subscribe(() -> {
                AlarmStorage.getInstance(context).setAlarmActive(alarmData.get(position).getAlarmId(), checked);
            });
            // TODO: cancel alarm schedule if is being disabled
        });
        holder.active.setChecked(alarmData.get(position).isActive());
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
        List<AlarmData.ActiveDays> ad = alarmData.get(position).getActiveDays();
        List<String> names = new ArrayList<>();
        for (int i = 0; i < ad.size(); i++) {
            names.add(weekdayShorts[ad.get(i).ordinal()]);
        }
        if(names.size() > 0){
            holder.activeDays.setText(String.join(", ", names));
        }
        else {
            holder.activeDays.setText("No repeat");
        }

        holder.setSelectionModeActive(isInSelectionMode);
        holder.chkChecked.setOnTouchListener((v, event) -> {
            View par = ((View) v.getParent().getParent());
            event.setLocation(par.getWidth(), 0);
            par.onTouchEvent(event);
            return true;
        });
        holder.chkChecked.setChecked(alarmData.get(position).isSelected());
        holder.card.setOnClickListener(e -> {
            if(isInSelectionMode){
                holder.chkChecked.setChecked(!holder.chkChecked.isChecked());
                alarmData.get(position).setSelected(holder.chkChecked.isChecked());
                selectionCounter += holder.chkChecked.isChecked() ? 1 : -1;
                if(selectionCounter == 0){
                    setSelectionModeInItems(false);
                    isInSelectionMode = false;
                    if(mSelectionModeStateChangedListener != null){
                        mSelectionModeStateChangedListener.onSelectionModeLeft();
                    }
                }
            }
            else {
                Intent editor = new Intent(context, AlarmCreator.class);
                editor.putExtra("ALARM_ID", alarmData.get(position).getAlarmId());
                context.startActivity(editor);
                // TODO open editor
            }
        });
        holder.card.setOnLongClickListener(e -> {
            if(!isInSelectionMode){
                holder.chkChecked.setChecked(!holder.chkChecked.isChecked());
                alarmData.get(position).setSelected(holder.chkChecked.isChecked());
                selectionCounter += holder.chkChecked.isChecked() ? 1 : -1;
                isInSelectionMode = true;
                setSelectionModeInItems(true);
                if(mSelectionModeStateChangedListener != null){
                    mSelectionModeStateChangedListener.onSelectionModeEntered();
                }
            }
            return true;
        });
    }

    @Override
    public void onViewRecycled(@NonNull MainViewHolderAlarms holder) {
        super.onViewRecycled(holder);
        loadedPositions.add(holder.getAdapterPosition());
        loadedItems.remove(holder);
    }

    private void setSelectionModeInItems(boolean selMode) {
        for (MainViewHolderAlarms al : loadedItems) {
            al.setSelectionModeActive(selMode);
        }
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mRecyclerView = recyclerView;
    }

    public void setData(List<AlarmData> alarmData) {
        this.alarmData = alarmData;
        notifyDataSetChanged();
        AlarmStorage.getInstance(context).setOnAlarmAddedListener(alarm -> {
            alarmData.add(AlarmTools.getAlarmDataFromItem(alarm));
            notifyItemInserted(getItemCount() - 1);
        });
        AlarmStorage.getInstance(context).setOnAlarmUpdatedListener(alarmId -> {
            for (int i = 0; i < alarmData.size(); i++) {
                if(alarmData.get(i).getAlarmId() == alarmId) {
                    alarmData.set(i, AlarmTools.getAlarmDataFromItem(AlarmStorage.getInstance(context).getAlarmItemWithId(alarmId)));
                    notifyItemChanged(i);
                    break;
                }
            }
        });
    }

    public List<Integer> getSelectedEntryIds() {
        List<Integer> selected = new ArrayList<>();
        for (AlarmData alarm : alarmData){
            if(alarm.isSelected()) {
                int alarmId = alarm.getAlarmId();
                if(alarmId != -1){
                    selected.add(alarmId);
                }
            }
        }
        return selected;
    }

    public List<Integer> getSelectedEntryPositions() {
        List<Integer> selected = new ArrayList<>();
        for (int i = 0; i < alarmData.size(); i++){
            if(alarmData.get(i).isSelected()) {
                int alarmId = alarmData.get(i).getAlarmId();
                if(alarmId != -1){
                    selected.add(i);
                }
            }
        }
        return selected;
    }

    public void removedEntryPositions(List<Integer> selectedEntryPositions) {
        for (int i = 0; i < selectedEntryPositions.size(); i++){
            alarmData.remove(selectedEntryPositions.get(i) - i);
            notifyItemRemoved(selectedEntryPositions.get(i) - i);
            selectionCounter--;
        }
        setSelectionModeInItems(false);
        isInSelectionMode = false;
        if(mSelectionModeStateChangedListener != null){
            mSelectionModeStateChangedListener.onSelectionModeLeft();
        }
        loadedPositions.forEach(this::notifyItemChanged);
    }

    @Override
    public int getItemCount() {
        return alarmData.size();
    }

    public void leaveSelectionMode() {
        selectionCounter = 0;
        for (AlarmData alarm : alarmData) {
            alarm.setSelected(false);
        }
        setSelectionModeInItems(false);
        isInSelectionMode = false;
        if(mSelectionModeStateChangedListener != null){
            mSelectionModeStateChangedListener.onSelectionModeLeft();
        }
    }

    public class MainViewHolderAlarms extends RecyclerView.ViewHolder {
        TextView title, timePrimary, timeSecondary, activeDays;
        TextView dayMo, dayTu, dayWe, dayTh, dayFr, daySa, daySu;
        MaterialCardView card;
        SwitchMaterial active;
        List<TextView> weekdays;
        CheckBox chkChecked;

        public MainViewHolderAlarms(@NonNull View itemView) {
            super(itemView);
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

        public void setSelectionModeActive(boolean isInSelectionMode) {
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

    public interface OnEntryClicked {
        void onEvent(AlarmData binauralBeat, int position);
    }

    public void setOnEntryClickedListener(OnEntryClicked eventListener) {
        mListener = eventListener;
    }

    public interface OnSelectionModeStateChanged {
        void onSelectionModeEntered();
        void onSelectionModeLeft();
    }

    public void setOnSelectionModeStateChangedListener(OnSelectionModeStateChanged eventListener) {
        mSelectionModeStateChangedListener = eventListener;
    }
}
