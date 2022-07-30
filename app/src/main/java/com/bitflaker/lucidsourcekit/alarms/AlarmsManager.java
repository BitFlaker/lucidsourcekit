package com.bitflaker.lucidsourcekit.alarms;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

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
import java.util.Timer;
import java.util.TimerTask;

public class AlarmsManager extends AppCompatActivity {
    private TextView time, date;
    private RecyclerViewAdapterAlarms adapterAlarms;
    private boolean isInSelectionMode = false;

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
                startActivity(new Intent(this, AlarmCreator.class));
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
        recyclerView.setAdapter(adapterAlarms);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        new Thread(() -> {
            while(!AlarmStorage.getInstance(this).isFinishedLoading()){
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            runOnUiThread(this::showAllStoredAlarms);
        }).start();
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
//        alarms.add(new AlarmData("Random alarm", Calendar.getInstance(), Arrays.asList(AlarmData.ActiveDays.MONDAY, AlarmData.ActiveDays.TUESDAY, AlarmData.ActiveDays.SUNDAY), true));
//        alarms.add(new AlarmData("Second alarm", Calendar.getInstance(), Arrays.asList(AlarmData.ActiveDays.FRIDAY, AlarmData.ActiveDays.SATURDAY, AlarmData.ActiveDays.SUNDAY), false));
//        alarms.add(new AlarmData("Mid REM sleep", Calendar.getInstance(), Arrays.asList(AlarmData.ActiveDays.WEDNESDAY, AlarmData.ActiveDays.THURSDAY), false));
//        alarms.add(new AlarmData("After party waker", Calendar.getInstance(), Arrays.asList(AlarmData.ActiveDays.SATURDAY, AlarmData.ActiveDays.SUNDAY), true));
        adapterAlarms.setData(alarms);
    }
}