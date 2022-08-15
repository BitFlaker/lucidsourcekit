package com.bitflaker.lucidsourcekit.alarms;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.clock.SleepClock;
import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.bitflaker.lucidsourcekit.main.AlarmData;
import com.bitflaker.lucidsourcekit.main.RecyclerViewAdapterAlarms;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class AlarmsManager extends AppCompatActivity {
    private TextView time, date, nAlarmTime, nAlarmTimeTo;
    private RecyclerViewAdapterAlarms adapterAlarms;
    private boolean isInSelectionMode = false;
    private long nextAlarmTimeStamp;
    ActivityResultLauncher<Intent> alarmInteractionLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        setNextAlarmData(false);
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarms_manager);

        Tools.makeStatusBarTransparent(AlarmsManager.this);
        LinearLayout topContainer = findViewById(R.id.ll_top_heading);
        SleepClock clock = findViewById(R.id.slp_clock);
        FloatingActionButton addAlarm = findViewById(R.id.fab_add_alarm);
        addAlarm.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.colorPrimary, getTheme()));
        topContainer.setLayoutParams(Tools.addRelativeLayoutParamsTopStatusbarSpacing(AlarmsManager.this, ((RelativeLayout.LayoutParams) topContainer.getLayoutParams())));

        clock.startClock();
        addAlarm.setOnClickListener(e -> {
            if(!isInSelectionMode){
                alarmInteractionLauncher.launch(new Intent(this, AlarmCreator.class));
            }
            else {
                new AlertDialog.Builder(this, Tools.getThemeDialog()).setTitle("Delete Alarms").setMessage("Do you really want to delete the selected alarms?")
                        .setPositiveButton(getResources().getString(R.string.yes), (dialog, which) -> {
                            List<Integer> alarmsToDelete = adapterAlarms.getSelectedEntryIds();
                            List<Integer> selectedEntryPositions = adapterAlarms.getSelectedEntryPositions();
                            MainDatabase.getInstance(this).getAlarmDao().deleteAllById(alarmsToDelete).subscribe(() -> {
                                AlarmStorage.getInstance(this).removedAlarmIds(alarmsToDelete);
                                adapterAlarms.removedEntryPositions(selectedEntryPositions);
                            });
                        })
                        .setNegativeButton(getResources().getString(R.string.no), null)
                        .show();
            }
        });

        RecyclerView recyclerView = findViewById(R.id.rcv_list_alarms);
        time = findViewById(R.id.txt_current_time);
        date = findViewById(R.id.txt_current_date);
        nAlarmTime = findViewById(R.id.txt_next_alarm_time);
        nAlarmTimeTo = findViewById(R.id.txt_next_alarm_time_to);

        DateFormat tf = DateFormat.getTimeInstance(DateFormat.MEDIUM);
        DateFormat df = DateFormat.getDateInstance(DateFormat.LONG);
        time.setText(tf.format(Calendar.getInstance().getTime()));
        date.setText(df.format(Calendar.getInstance().getTime()));

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.SECOND, cal.get(Calendar.SECOND) + 1);
        cal.set(Calendar.MILLISECOND, 0);

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Calendar curr = Calendar.getInstance();
                String timeS = tf.format(curr.getTime());
                String dateS = df.format(curr.getTime());

                runOnUiThread(() -> {
                    time.setText(timeS);
                    date.setText(dateS);
                });
            }
        }, cal.getTimeInMillis() - Calendar.getInstance().getTimeInMillis(), 1000);

        adapterAlarms = new RecyclerViewAdapterAlarms(this, new ArrayList<>());
        adapterAlarms.setOnSelectionModeStateChangedListener(new RecyclerViewAdapterAlarms.OnSelectionModeStateChanged() {
            @Override
            public void onSelectionModeEntered() {
                addAlarm.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.colorError, getTheme()));
                addAlarm.setImageResource(R.drawable.ic_baseline_delete_24);
                isInSelectionMode = true;
            }

            @Override
            public void onSelectionModeLeft() {
                addAlarm.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.colorPrimary, getTheme()));
                addAlarm.setImageResource(R.drawable.ic_round_add_24);
                isInSelectionMode = false;
            }
        });
        adapterAlarms.setOnEntryClickedListener(alarmData -> {
            Intent editor = new Intent(this, AlarmCreator.class);
            editor.putExtra("ALARM_ID", alarmData.getAlarmId());
            alarmInteractionLauncher.launch(editor);
        });
        adapterAlarms.setOnEntryActiveStateChangedListener((alarmData, checked) -> setNextAlarmData(false));
        recyclerView.setAdapter(adapterAlarms);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        AlarmStorage.getInstance(this).setOnAlarmsLoadedListener(this::showAllStoredAlarms);
        if(AlarmStorage.getInstance(this).isLoaded() && adapterAlarms.getItemCount() == 0) {
            showAllStoredAlarms();
        }
    }

    private void setNextAlarmData(boolean startCycle) {
        // TODO: support AM/PM
        nextAlarmTimeStamp = getNextAlarmTimeStamp();
        if(nextAlarmTimeStamp != -1){
            Calendar calAlarm = Calendar.getInstance();
            calAlarm.setTimeInMillis(nextAlarmTimeStamp);
            // TODO: also say which weekday as otherwise it seems as if it would go off today at that time
            nAlarmTime.setText(String.format(Locale.ENGLISH, "%02d:%02d", calAlarm.get(Calendar.HOUR_OF_DAY), calAlarm.get(Calendar.MINUTE)));
            setNextTimeTo(nextAlarmTimeStamp);
        }
        else {
            nAlarmTime.setText("--");
            nAlarmTimeTo.setText("--");
        }

        if(startCycle){
            Calendar calDelay = Calendar.getInstance();
            calDelay.set(Calendar.MINUTE, calDelay.get(Calendar.MINUTE) + 1);
            calDelay.set(Calendar.SECOND, 0);
            calDelay.set(Calendar.MILLISECOND, 0);

            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    setNextTimeTo(nextAlarmTimeStamp);
                }
            }, calDelay.getTimeInMillis() - Calendar.getInstance().getTimeInMillis(), 60*1000);
        }
    }

    @Override
    public void onBackPressed() {
        if(isInSelectionMode){
            adapterAlarms.leaveSelectionMode();
        }
        else {
            super.onBackPressed();
        }
    }

    private void showAllStoredAlarms() {
        List<AlarmData> alarms = new ArrayList<>();
        for (int i = 0; i < AlarmStorage.getInstance(this).size(); i++) {
            alarms.add(AlarmTools.getAlarmDataFromItem(AlarmStorage.getInstance(this).getAlarmAt(i)));
        }
        adapterAlarms.setData(alarms);
        setNextAlarmData(true);
    }

    private long getNextAlarmTimeStamp() {
        List<Long> nextTimeStamps = new ArrayList<>();
        for (int i = 0; i < AlarmStorage.getInstance(this).size(); i++) {
            AlarmItem ai = AlarmStorage.getInstance(this).getAlarmAt(i);
            if(ai.isActive()){
                nextTimeStamps.add(ai.getTimesTo().get(0).getMillisTimeStamp());
            }
        }
        nextTimeStamps.sort((nts1, nts2) -> {
            if(nts1 < nts2) { return -1; }
            else if(nts1 > nts2) { return 1; }
            return 0;
        });
        return nextTimeStamps.size() == 0 ? -1 : nextTimeStamps.get(0);
    }

    private void setNextTimeTo(long nextAlarmTimeStamp) {
        if(nextAlarmTimeStamp == -1){
//            runOnUiThread(() -> nAlarmTimeTo.setText("--"));
            return;
        }
        long millisDiff = nextAlarmTimeStamp - Calendar.getInstance().getTimeInMillis() + (60 * 1000); // added 1 minute because seconds are hidden and then there would be alarm in 0 minutes for 1 minute
        double days = (millisDiff / 1000.0 / 60.0 / 60.0 / 24.0);
        double hours = (days - (int)days) * 24.0;
        double minutes = (hours - (int)hours) * 60.0;
//        double seconds = (minutes - (int)minutes) * 60.0;

        runOnUiThread(() -> {
            nAlarmTimeTo.setText(String.format(Locale.ENGLISH, "%02d:%02d:%02d", (int)days, (int)hours, (int)minutes));
            if((int)days == 0 && (int)hours == 0 && (int)minutes == 0){
                setNextAlarmData(false);
            }
        });
    }
}