package com.bitflaker.lucidsourcekit;

import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.bitflaker.lucidsourcekit.alarms.AlarmReceiverManager;

public class AlarmDisplayer extends AppCompatActivity {
    private AlarmManager alarmManager;
    private PendingIntent alarmIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_receiver);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            keyguardManager.requestDismissKeyguard(this, null);
        }
        else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        ImageButton stopAlarm = findViewById(R.id.btn_stop_alarm);
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        stopAlarm.setOnClickListener(e -> {
            Intent intent = new Intent(this, AlarmReceiverManager.class);
            alarmIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
            if (alarmManager!= null) {
                alarmManager.cancel(alarmIntent);
                System.out.println("Alarm cancelled");
            }
        });
    }
}