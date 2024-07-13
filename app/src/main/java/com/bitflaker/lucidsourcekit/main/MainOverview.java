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
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.alarms.AlarmCreator;
import com.bitflaker.lucidsourcekit.alarms.AlarmsManager;
import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.AudioLocation;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntry;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntryHasType;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.resulttables.AssignedTags;
import com.bitflaker.lucidsourcekit.database.notifications.entities.NotificationCategory;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.bitflaker.lucidsourcekit.general.database.values.DreamJournalEntry;
import com.bitflaker.lucidsourcekit.general.database.values.DreamTypes;
import com.bitflaker.lucidsourcekit.notification.NotificationManager;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class MainOverview extends Fragment {
    private TextView noAlarmsSet;
    private RecyclerViewAdapterAlarms adapterAlarms;
    private MaterialCardView realityCheckReminderCard, permanentNotificationCard, lockscreenWriterCard, taskReminderCard;
    private TextView realityCheckReminderText, permanentNotificationText, lockscreenWriterText, taskReminderText;
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
    private final ActivityResultLauncher<Intent> activeEventsReloadLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> reloadActiveEventsStates());
    private RecyclerView recyclerView;
    private MainDatabase db;
    public CompositeDisposable disposables = new CompositeDisposable();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main_overview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setData();
    }

    private void setData() {
        db = MainDatabase.getInstance(getContext());

        noAlarmsSet = getView().findViewById(R.id.txt_no_alarms_set);

        realityCheckReminderCard = getView().findViewById(R.id.crd_reality_check_reminder_state);
        permanentNotificationCard = getView().findViewById(R.id.crd_permanent_notification_state);
        lockscreenWriterCard = getView().findViewById(R.id.crd_lockscreen_writer_state);
        taskReminderCard = getView().findViewById(R.id.crd_task_reminder_state);
        realityCheckReminderText = getView().findViewById(R.id.txt_reality_check_reminder_state);
        permanentNotificationText = getView().findViewById(R.id.txt_permanent_notification_state);
        lockscreenWriterText = getView().findViewById(R.id.txt_lockscreen_writer_state);
        taskReminderText = getView().findViewById(R.id.txt_task_reminder_state);

        View rememberDream = getView().findViewById(R.id.dje_remember_dream);
        MaterialCardView noRememberEntries = getView().findViewById(R.id.crd_no_remember_entry);
        disposables.add(db.getJournalEntryDao().getRandomEntry().subscribe(entries -> {
            if (entries.isEmpty()) {
                rememberDream.setVisibility(View.GONE);
                noRememberEntries.setVisibility(View.VISIBLE);
                return;
            }
            JournalEntry entry = entries.get(0);
            List<AssignedTags> assignedTags = db.getJournalEntryHasTagDao().getAllFromEntryId(entry.entryId).blockingGet();
            List<JournalEntryHasType> journalEntryHasTypes = db.getJournalEntryIsTypeDao().getAllFromEntryId(entry.entryId).blockingGet();
            List<AudioLocation> audioLocations = db.getAudioLocationDao().getAllFromEntryId(entry.entryId).blockingGet();
            DreamJournalEntry djEntry = new DreamJournalEntry(entry, assignedTags, journalEntryHasTypes, audioLocations);
            generateRememberEntry(rememberDream, djEntry);
            rememberDream.setOnClickListener(e -> {
                // TODO: switch over to dream journal entries, scroll to this entry and click it to open the dream journal entry viewer
                //       maybe allow as option not to scroll all the way to the entry but to just simply open it
            });
        }));

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

        reloadActiveEventsStates();
        realityCheckReminderCard.setOnClickListener(e -> openNotificationSettingsWithId("RCR"));
        taskReminderCard.setOnClickListener(e -> openNotificationSettingsWithId("DGR"));
        permanentNotificationCard.setOnClickListener(e -> openNotificationSettingsWithId("PN"));
    }

    private RecyclerViewAdapterDreamJournal.MainViewHolder generateRememberEntry(View rememberDream, DreamJournalEntry entry) {
        RecyclerViewAdapterDreamJournal.MainViewHolder rememberDreamHolder = new RecyclerViewAdapterDreamJournal.MainViewHolder(rememberDream, getContext());
        rememberDreamHolder.resetEntry();
        List<DreamTypes> dreamTypes = entry.getTypes().stream()
                .map(e -> DreamTypes.getEnum(e.typeId))
                .collect(Collectors.toList());
        rememberDreamHolder.setSpecialDreamIcons(dreamTypes);
        rememberDreamHolder.setTitleAndTextContent(entry.getEntry().title, entry.getEntry().description);
        rememberDreamHolder.setRecordingsCount(entry.getAudioLocations().size());
        rememberDreamHolder.setTagList(entry.getTagStrings(), getActivity());
        return rememberDreamHolder;
    }

    private void reloadActiveEventsStates() {
        List<NotificationCategory> categories = db.getNotificationCategoryDao().getAll().blockingGet();
        for (NotificationCategory category : categories) {
            switch(category.getId()){
                case "RCR":
                    realityCheckReminderText.setCompoundDrawablesWithIntrinsicBounds(category.isEnabled() ? R.drawable.rounded_check_24 : R.drawable.rounded_close_24, 0, 0, 0);
                    TextViewCompat.setCompoundDrawableTintList(realityCheckReminderText, Tools.getAttrColorStateList(category.isEnabled() ? R.attr.secondaryTextColor : R.attr.tertiaryTextColor, getContext().getTheme()));
                    break;
                case "DGR":
                    taskReminderText.setCompoundDrawablesWithIntrinsicBounds(category.isEnabled() ? R.drawable.rounded_check_24 : R.drawable.rounded_close_24, 0, 0, 0);
                    TextViewCompat.setCompoundDrawableTintList(taskReminderText, Tools.getAttrColorStateList(category.isEnabled() ? R.attr.secondaryTextColor : R.attr.tertiaryTextColor, getContext().getTheme()));
                    break;
                case "PN":
                    permanentNotificationText.setCompoundDrawablesWithIntrinsicBounds(category.isEnabled() ? R.drawable.rounded_check_24 : R.drawable.rounded_close_24, 0, 0, 0);
                    TextViewCompat.setCompoundDrawableTintList(permanentNotificationText, Tools.getAttrColorStateList(category.isEnabled() ? R.attr.secondaryTextColor : R.attr.tertiaryTextColor, getContext().getTheme()));
                    break;
            }
        }
    }

    private void openNotificationSettingsWithId(String RCR) {
        Intent intent = new Intent(getContext(), NotificationManager.class);
        intent.putExtra("AUTO_OPEN_ID", RCR);
        activeEventsReloadLauncher.launch(intent);
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

    @Override
    public void onDestroyView() {
        disposables.clear();
        super.onDestroyView();
    }
}