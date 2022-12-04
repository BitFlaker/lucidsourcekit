package com.bitflaker.lucidsourcekit.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.alarms.AlarmsManager;

public class MainOverview extends Fragment {
    private TextView totalEntries, totalLucidEntries, totalGoalsReached, streak;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main_overview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setData();
        fillStats();
    }

    private void setData() {
        totalEntries = getView().findViewById(R.id.txt_total_journal_entries);
        totalLucidEntries = getView().findViewById(R.id.txt_lucid_journal_entries);
        totalGoalsReached = getView().findViewById(R.id.txt_total_goals_reached);
        streak = getView().findViewById(R.id.txt_days_streak);

        getView().findViewById(R.id.crd_alarm1).setOnClickListener(e -> { });
        getView().findViewById(R.id.crd_alarm2).setOnClickListener(e -> { });
        getView().findViewById(R.id.btn_manage_alarms).setOnClickListener(e -> {
//            AlarmHandler.clickAction(getActivity().getApplicationContext());
            startActivity(new Intent(getContext(), AlarmsManager.class));


//            AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
//            PendingIntent alarmIntent;
//            Intent intent = new Intent(getContext(), AlarmReceiverManager.class);
//            alarmIntent = PendingIntent.getBroadcast(getContext(), 0, intent, 0);
//            alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (6 * 1000L), alarmIntent);
//            startActivity(new Intent(getContext(), AlarmDisplayer.class));
        });
    }

    private void fillStats() {
        // TODO: fill with real data
        //totalEntries
        //totalLucidEntries
        //totalGoalsReached
        //streak
//        ((TextView) getView().findViewById(R.id.txt_alarm1)).setText(Html.fromHtml("<span><big><big><strong>03:12</strong></big></big></span><br /><span>first night timer</span>", Html.FROM_HTML_MODE_COMPACT));
//        ((TextView) getView().findViewById(R.id.txt_alarm2)).setText(Html.fromHtml("<span><big><big><strong>06:24</strong></big></big></span><br /><span>last night timer</span>", Html.FROM_HTML_MODE_COMPACT));
    }
}