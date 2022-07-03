package com.bitflaker.lucidsourcekit;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.bitflaker.lucidsourcekit.clock.SleepClock;

public class AlarmCreator extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_creator);

        SleepClock sleepClock = findViewById(R.id.slp_clk_set_time);

        sleepClock.setDrawHours(true);
        sleepClock.setDrawTimeSetterButtons(true);
    }
}