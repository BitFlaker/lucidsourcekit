package com.bitflaker.lucidsourcekit.main;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.alarms.AlarmCreator;
import com.bitflaker.lucidsourcekit.alarms.AlarmsManager;
import com.bitflaker.lucidsourcekit.database.MainDatabase;

import java.util.ArrayList;

public class MainOverview extends Fragment {
    private TextView totalEntries, totalLucidEntries, totalGoalsReached, streak, noAlarmsSet;
    private RecyclerViewAdapterAlarms adapterAlarms;
    private final ActivityResultCallback<ActivityResult> alarmCreationOrModificationCallback = result -> {
        if(result.getResultCode() == RESULT_OK){
            Intent data = result.getData();
            if (data != null && data.hasExtra("CREATED_NEW_ALARM") && data.hasExtra("ALARM_ID")) {
                if(!data.getBooleanExtra("CREATED_NEW_ALARM", false)){
                    adapterAlarms.reloadModifiedAlarmWithId(data.getIntExtra("ALARM_ID", -1));
                }
            }
        }
    };
    private final ActivityResultCallback<ActivityResult> alarmManagerCallback = result -> {
        MainDatabase.getInstance(getContext()).getStoredAlarmDao().getAllActive().subscribe(storedAlarms -> {
            adapterAlarms.setData(storedAlarms);
            setNoActiveAlarmsMessageVisible(storedAlarms.size() == 0);
        }).dispose();
    };
    private final ActivityResultLauncher<Intent> alarmInteractionLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), alarmCreationOrModificationCallback);
    private final ActivityResultLauncher<Intent> alarmManagerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), alarmManagerCallback);
    private RecyclerView recyclerView;

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
        noAlarmsSet = getView().findViewById(R.id.txt_no_alarms_set);

        recyclerView = getView().findViewById(R.id.rcv_active_alarms);
        adapterAlarms = new RecyclerViewAdapterAlarms(getContext(), new ArrayList<>());
        adapterAlarms.setSelectionModeEnabled(false);
        adapterAlarms.setControlsVisible(false);
        adapterAlarms.setElevatedBackground(true);
        adapterAlarms.setOnEntryClickedListener(storedAlarm -> {
            Intent editor = new Intent(getContext(), AlarmCreator.class);
            editor.putExtra("ALARM_ID", storedAlarm.alarmId);
            alarmInteractionLauncher.launch(editor);
        });
        recyclerView.setAdapter(adapterAlarms);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        MainDatabase db = MainDatabase.getInstance(getContext());
        db.getStoredAlarmDao().getAllActive().subscribe(storedAlarms -> {
            adapterAlarms.setData(storedAlarms);
            setNoActiveAlarmsMessageVisible(storedAlarms.size() == 0);
        }).dispose();

//        getView().findViewById(R.id.crd_alarm1).setOnClickListener(e -> { });
//        getView().findViewById(R.id.crd_alarm2).setOnClickListener(e -> { });
        getView().findViewById(R.id.btn_manage_alarms).setOnClickListener(e -> {
//            AlarmHandler.clickAction(getActivity().getApplicationContext());
            alarmManagerLauncher.launch(new Intent(getContext(), AlarmsManager.class));


//            AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
//            PendingIntent alarmIntent;
//            Intent intent = new Intent(getContext(), AlarmReceiverManager.class);
//            alarmIntent = PendingIntent.getBroadcast(getContext(), 0, intent, 0);
//            alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (6 * 1000L), alarmIntent);
//            startActivity(new Intent(getContext(), AlarmDisplayer.class));
        });
    }

    private void setNoActiveAlarmsMessageVisible(boolean visible) {
        if(visible){
            noAlarmsSet.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }
        else {
            noAlarmsSet.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
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