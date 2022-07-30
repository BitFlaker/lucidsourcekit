package com.bitflaker.lucidsourcekit.alarms;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiverManager extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent alarmDisplayer = new Intent(context, AlarmDisplayer.class);
        alarmDisplayer.setFlags(FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(alarmDisplayer);
    }
}
