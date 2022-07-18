package com.bitflaker.lucidsourcekit.alarms;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.clock.SleepClock;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarms_manager);

        Tools.makeStatusBarTransparent(AlarmsManager.this);
        LinearLayout topContainer = findViewById(R.id.ll_top_heading);
        SleepClock clock = findViewById(R.id.slp_clock);
        FloatingActionButton addAlarm = findViewById(R.id.fab_add_alarm);
        topContainer.setLayoutParams(Tools.addRelativeLayoutParamsTopStatusbarSpacing(AlarmsManager.this, ((RelativeLayout.LayoutParams) topContainer.getLayoutParams())));

        clock.startClock();
        addAlarm.setOnClickListener(e -> startActivity(new Intent(this, AlarmCreator.class)));

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

    private void showAllStoredAlarms() {
        List<AlarmData> alarms = new ArrayList<>();
        for (int i = 0; i < AlarmStorage.getInstance(this).size(); i++) {
            alarms.add(getDataFromItem(AlarmStorage.getInstance(this).getAlarmAt(i)));
        }
//        alarms.add(new AlarmData("Random alarm", Calendar.getInstance(), Arrays.asList(AlarmData.ActiveDays.MONDAY, AlarmData.ActiveDays.TUESDAY, AlarmData.ActiveDays.SUNDAY), true));
//        alarms.add(new AlarmData("Second alarm", Calendar.getInstance(), Arrays.asList(AlarmData.ActiveDays.FRIDAY, AlarmData.ActiveDays.SATURDAY, AlarmData.ActiveDays.SUNDAY), false));
//        alarms.add(new AlarmData("Mid REM sleep", Calendar.getInstance(), Arrays.asList(AlarmData.ActiveDays.WEDNESDAY, AlarmData.ActiveDays.THURSDAY), false));
//        alarms.add(new AlarmData("After party waker", Calendar.getInstance(), Arrays.asList(AlarmData.ActiveDays.SATURDAY, AlarmData.ActiveDays.SUNDAY), true));
        adapterAlarms.setData(alarms);
    }

    private AlarmData getDataFromItem(AlarmItem alarmAt) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, alarmAt.getAlarmHour());
        cal.set(Calendar.MINUTE, alarmAt.getAlarmMinute());

        List<AlarmData.ActiveDays> activeDays = new ArrayList<>();
        List<Integer> days = alarmAt.getAlarmRepeatWeekdays();

        for (int day : days) {
            activeDays.add(AlarmData.ActiveDays.values()[day-2 < 0 ? AlarmData.ActiveDays.SUNDAY.ordinal() : day-2]);
        }

        return new AlarmData(alarmAt.getTitle(), cal, activeDays, alarmAt.isActive());
    }
}