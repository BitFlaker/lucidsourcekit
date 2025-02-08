package com.bitflaker.lucidsourcekit.main.overview;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.resulttables.DreamJournalEntry;
import com.bitflaker.lucidsourcekit.databinding.EntryJournalBinding;
import com.bitflaker.lucidsourcekit.databinding.FragmentMainOverviewBinding;
import com.bitflaker.lucidsourcekit.main.alarms.AlarmEditorView;
import com.bitflaker.lucidsourcekit.main.alarms.AlarmManagerView;
import com.bitflaker.lucidsourcekit.main.alarms.RecyclerViewAdapterAlarms;
import com.bitflaker.lucidsourcekit.main.dreamjournal.RecyclerViewAdapterDreamJournal;
import com.bitflaker.lucidsourcekit.main.notification.NotificationManagerView;
import com.bitflaker.lucidsourcekit.main.notification.VisualNotificationActivity;

import java.util.ArrayList;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class MainOverviewView extends Fragment {
    private RecyclerViewAdapterAlarms adapterAlarms;
    private OnRememberJournalEntryClicked mRememberJournalEntryClickedListener;
    private final ActivityResultCallback<ActivityResult> alarmCreationOrModificationCallback = result -> {
        if(result.getResultCode() == RESULT_OK) {
            Intent data = result.getData();
            if (data != null && data.hasExtra("CREATED_NEW_ALARM") && data.hasExtra("ALARM_ID")) {
                if(!data.getBooleanExtra("CREATED_NEW_ALARM", false)){
                    adapterAlarms.reloadModifiedAlarmWithId(data.getLongExtra("ALARM_ID", -1));
                }
            }
        }
    };
    private final ActivityResultCallback<ActivityResult> alarmManagerCallback = result -> {
        MainDatabase.getInstance(getContext()).getStoredAlarmDao().getAllActive().subscribe(storedAlarms -> {
            adapterAlarms.setData(storedAlarms);
            setNoActiveAlarmsMessageVisible(storedAlarms.isEmpty());
        }).dispose();
    };
    private final ActivityResultLauncher<Intent> alarmInteractionLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), alarmCreationOrModificationCallback);
    private final ActivityResultLauncher<Intent> alarmManagerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), alarmManagerCallback);
    public CompositeDisposable disposables = new CompositeDisposable();
    private MainDatabase db;
    private FragmentMainOverviewBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMainOverviewBinding.inflate(getLayoutInflater());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setData();
    }

    private void setData() {
        db = MainDatabase.getInstance(getContext());
        disposables.add(db.getJournalEntryDao().getRandomEntry().subscribe(entries -> {
            if (entries.isEmpty()) {
                binding.djeRememberDream.llJournalEntry.setVisibility(View.GONE);
                binding.crdNoRememberEntry.setVisibility(View.VISIBLE);
                return;
            }
            DreamJournalEntry entry = entries.get(0);
            generateRememberEntry(binding.djeRememberDream, entry);
            binding.djeRememberDream.crdJournalEntryCard.setOnClickListener(e -> {
                if (mRememberJournalEntryClickedListener != null) {
                    mRememberJournalEntryClickedListener.onEvent(entry);
                }
                // TODO: add setting not to smooth scroll to the entry and to just open it
            });
        }));

        adapterAlarms = new RecyclerViewAdapterAlarms(getContext(), new ArrayList<>());
        adapterAlarms.setSelectionModeEnabled(false);
        adapterAlarms.setControlsVisible(false);
        adapterAlarms.setOnEntryClickedListener(storedAlarm -> {
            Intent editor = new Intent(getContext(), AlarmEditorView.class);
            editor.putExtra("ALARM_ID", storedAlarm.alarmId);
            alarmInteractionLauncher.launch(editor);
        });
        binding.rcvActiveAlarms.setAdapter(adapterAlarms);
        binding.rcvActiveAlarms.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rcvActiveAlarms.setItemAnimator(null);
        db.getStoredAlarmDao().getAllActive().subscribe(storedAlarms -> {
            adapterAlarms.setData(storedAlarms);
            setNoActiveAlarmsMessageVisible(storedAlarms.isEmpty());
        }).dispose();

        binding.btnManageAlarms.setOnClickListener(e -> {
            alarmManagerLauncher.launch(new Intent(getContext(), AlarmManagerView.class));
        });

        // Quick action button click
        binding.btnQaNotifications.setOnClickListener(e -> startActivity(new Intent(getContext(), NotificationManagerView.class)));

        // Quick action container click forwarding
        binding.llQaNotifications.setOnClickListener(e -> binding.btnQaNotifications.performClick());

        // TODO: This is just here for testing purpose, move this into alarm receiver for reality check reminder
        //       and add a check for locked screen, as it should only be launched when the user is not
        //       actively using the phone in order to not interfere with user interactions
        binding.btnQaMore.setOnClickListener(e -> startActivity(new Intent(getContext(), VisualNotificationActivity.class)));
    }

    private void generateRememberEntry(EntryJournalBinding binding, DreamJournalEntry entry) {
        RecyclerViewAdapterDreamJournal.MainViewHolder rememberDreamHolder = new RecyclerViewAdapterDreamJournal.MainViewHolder(binding, getContext());
        rememberDreamHolder.resetEntry();
        rememberDreamHolder.setSpecialDreamIcons(entry.getDreamTypes());
        rememberDreamHolder.setTitleAndTextContent(entry.journalEntry.title, entry.journalEntry.description);
        rememberDreamHolder.setRecordingsCount(entry.audioLocations.size());
        RecyclerViewAdapterDreamJournal.MainViewHolder.setTagList(binding.llTagsHolder, rememberDreamHolder.calculateAdditionalContainerPadding(), 1, entry.getStringTags(), getActivity());
    }

    private void setNoActiveAlarmsMessageVisible(boolean visible) {
        if(visible){
            binding.txtNoAlarmsSet.setVisibility(View.VISIBLE);
            binding.rcvActiveAlarms.setVisibility(View.GONE);
        }
        else {
            binding.txtNoAlarmsSet.setVisibility(View.GONE);
            binding.rcvActiveAlarms.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        disposables.clear();
        super.onDestroyView();
    }

    public interface OnRememberJournalEntryClicked {
        void onEvent(DreamJournalEntry entry);
    }

    public void setRememberJournalEntryClickedListener(OnRememberJournalEntryClicked listener) {
        mRememberJournalEntryClickedListener = listener;
    }
}