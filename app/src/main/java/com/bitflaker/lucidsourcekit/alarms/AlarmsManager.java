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
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class AlarmsManager extends AppCompatActivity {
    private TextView time, date;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarms_manager);

        Tools.makeStatusBarTransparent(AlarmsManager.this);
        LinearLayout topContainer = findViewById(R.id.ll_top_heading);
        SleepClock clock = findViewById(R.id.slp_clock);
        FloatingActionButton addAlarm = findViewById(R.id.fab_add_alarm);
        topContainer.setLayoutParams(Tools.addRelativeLayoutParamsTopStatusbarSpacing(AlarmsManager.this, ((RelativeLayout.LayoutParams) topContainer.getLayoutParams())));

        AlarmStorage.getInstance(this);
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

        List<AlarmData> alarms = new ArrayList<>();
        alarms.add(new AlarmData("Random alarm", Calendar.getInstance(), Arrays.asList(AlarmData.ActiveDays.MONDAY, AlarmData.ActiveDays.TUESDAY, AlarmData.ActiveDays.SUNDAY), true));
        alarms.add(new AlarmData("Second alarm", Calendar.getInstance(), Arrays.asList(AlarmData.ActiveDays.FRIDAY, AlarmData.ActiveDays.SATURDAY, AlarmData.ActiveDays.SUNDAY), false));
        alarms.add(new AlarmData("Mid REM sleep", Calendar.getInstance(), Arrays.asList(AlarmData.ActiveDays.WEDNESDAY, AlarmData.ActiveDays.THURSDAY), false));
        alarms.add(new AlarmData("After party waker", Calendar.getInstance(), Arrays.asList(AlarmData.ActiveDays.SATURDAY, AlarmData.ActiveDays.SUNDAY), true));
        RecyclerViewAdapterAlarms adapterAlarms = new RecyclerViewAdapterAlarms(this, alarms);
        recyclerView.setAdapter(adapterAlarms);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }
}