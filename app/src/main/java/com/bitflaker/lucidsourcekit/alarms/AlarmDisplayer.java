package com.bitflaker.lucidsourcekit.alarms;

import android.animation.ValueAnimator;
import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.general.JournalTypes;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.bitflaker.lucidsourcekit.main.dreamjournal.DreamJournalEntryEditor;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class AlarmDisplayer extends AppCompatActivity {
    private AlarmManager alarmManager;
    private PendingIntent alarmIntent;
    private OutsideSlider alarmSlider;
    private RecyclerView quickAccessActionsView;
    private ImageButton closeDisplayer;
    private TextView currentTimeView, alarmName;
    private boolean isSnoozing = false;
    private AlarmItem alarmItem;
    private MediaPlayer mediaPlayer;
    private Timer volumeIncreaser;
    private Timer vibrationTimer;
    private Timer flashlightTimer;
    private Vibrator vib;
    private String cameraId;
    private CameraManager camManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_displayer);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            keyguardManager.requestDismissKeyguard(this, null);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        Tools.makeStatusBarTransparent(this);
        alarmSlider = findViewById(R.id.oss_alarm_slider);
        quickAccessActionsView = findViewById(R.id.rcv_quick_access_actions);
        closeDisplayer = findViewById(R.id.btn_close_displayer);
        currentTimeView = findViewById(R.id.txt_current_time);
        alarmName = findViewById(R.id.txt_alarm_name);
        alarmSlider.setData(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_check_24, getTheme()), ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_snooze_24, getTheme()));
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        mediaPlayer = new MediaPlayer();
        vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        if(!getIntent().hasExtra("ALARM_ID")) { System.out.println("NONE"); finish(); }
        AlarmStorage.getInstance(this).setOnAlarmsLoadedListener(() -> {
            int id = getIntent().getIntExtra("ALARM_ID", -1);
            alarmItem = AlarmStorage.getInstance(this).getAlarmItemWithId(id);
            alarmName.setText(alarmItem.getTitle());
        });
        if (AlarmStorage.getInstance(this).isLoaded() && alarmItem == null) {
            int id = getIntent().getIntExtra("ALARM_ID", -1);
            alarmItem = AlarmStorage.getInstance(this).getAlarmItemWithId(id);
            alarmName.setText(alarmItem.getTitle());
        }

        if(alarmItem.getAlarmRepeatWeekdays().size() > 0) {
            AlarmTools.scheduleAlarm(getApplicationContext(), alarmItem);
        }
        else {
            alarmItem.setActive(false);
            AlarmStorage.getInstance(this).modifyAlarm(alarmItem);
        }

        quickAccessActionsView.setVisibility(View.GONE);
        closeDisplayer.setVisibility(View.GONE);
        quickAccessActionsView.setAlpha(0);
        closeDisplayer.setAlpha(0.0f);

        alarmSlider.setOnLeftSideSelectedListener(() -> {
            isSnoozing = false;
            disableAlarm();
        });

        alarmSlider.setOnRightSideSelectedListener(() -> {
            isSnoozing = true;
            disableAlarm();
            Intent intent = new Intent(this, AlarmReceiverManager.class);
            intent.putExtra("ALARM_ID", alarmItem.getAlarmId());
            alarmIntent = PendingIntent.getBroadcast(getApplicationContext(), Tools.getBroadcastReqCodeSnoozeFromID(alarmItem.getAlarmId()), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (5*60*1000), alarmIntent); // TODO make snooze delay changable
            finishAndRemoveTask();
        });

        alarmSlider.setOnFadedAwayListener(() -> {
            alarmSlider.setVisibility(View.GONE);
            getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            if(!isSnoozing){
                quickAccessActionsView.setVisibility(View.VISIBLE);
                closeDisplayer.setVisibility(View.VISIBLE);
                ValueAnimator opacityAnim = ValueAnimator.ofFloat(0, 1);
                opacityAnim.setDuration(300);
                opacityAnim.setInterpolator(new LinearInterpolator());
                opacityAnim.addUpdateListener((valueAnimator) -> {
                    quickAccessActionsView.setAlpha((float)valueAnimator.getAnimatedValue());
                    closeDisplayer.setAlpha((float)valueAnimator.getAnimatedValue());
                });
                opacityAnim.start();
            }
            else {
                finish();
            }
        });

        List<QuickAccessAction> quickAccessActions = new ArrayList<>();
        quickAccessActions.add(new QuickAccessAction("Add text journal entry", "Write down your dreams now so you do not forget them", ResourcesCompat.getDrawable(getResources(), R.drawable.ic_round_book_24, getTheme()), ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_text_fields_24, getTheme()), () -> {
            showJournalCreator(JournalTypes.Text);
        }));
        quickAccessActions.add(new QuickAccessAction("Add forms journal entry", "Write down your dreams into the set template for writing them down quickly and easily", ResourcesCompat.getDrawable(getResources(), R.drawable.ic_round_book_24, getTheme()), ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_ballot_24, getTheme()), () -> {
            showJournalCreator(JournalTypes.Forms);
        }));
        quickAccessActions.add(new QuickAccessAction("Listen to binaural beats", "Listening to binaural beats while going back to sleep might help to induce lucid dreams", ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_bedtime_24, getTheme()), null, () -> {
            Intent intent = new Intent(this, IsolatedBinauralBeatsView.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }));
        RecyclerViewAdapterQuickAccessActions rcvAQAA = new RecyclerViewAdapterQuickAccessActions(this, quickAccessActions);
        rcvAQAA.setOnEntryClickedListener(quickAccessAction -> quickAccessAction.getOnSelectedListener().onEvent());
        quickAccessActionsView.setLayoutManager(new LinearLayoutManager(this));
        quickAccessActionsView.setAdapter(rcvAQAA);
        closeDisplayer.setOnClickListener(e -> finishAndRemoveTask());

        Calendar cal = Calendar.getInstance();
        currentTimeView.setText(String.format(Locale.ENGLISH, "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)));
        cal.set(Calendar.SECOND, cal.get(Calendar.SECOND) + 1);
        cal.set(Calendar.MILLISECOND, 0);

        Timer clockTimer = new Timer();
        clockTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Calendar cal = Calendar.getInstance();
                runOnUiThread(() -> currentTimeView.setText(String.format(Locale.ENGLISH, "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))));
            }
        }, cal.getTimeInMillis() - Calendar.getInstance().getTimeInMillis(), 1000);

        // fixed the bug that you cannot open the Alarms-Manager because of the deprecated HARDWARE_CONTROLS permission.
        //fixed the bug that the dream description in the dream journal entries were displayed with slightly more than two lines

        try {
            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .build()
            );
            mediaPlayer.setLooping(true);
            if(alarmItem.getAlarmToneType() == AlarmItem.AlarmToneType.CUSTOM_FILE){
                mediaPlayer.setDataSource(this, alarmItem.getAlarmUri());
            }
            else if(alarmItem.getAlarmToneType() == AlarmItem.AlarmToneType.RINGTONE) {
                mediaPlayer.setDataSource(getApplicationContext(), alarmItem.getAlarmUri());
            }
            mediaPlayer.prepare();
            float alarmVolume = alarmItem.getAlarmVolume()/100.0f;
            if(alarmItem.isIncreaseVolume()){
                mediaPlayer.setVolume(0, 0);
                float incrementVolume = alarmVolume / alarmItem.getVolumeIncreaseTotalSeconds();
                final int[] currentSeconds = {0};
                volumeIncreaser = new Timer();
                volumeIncreaser.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        mediaPlayer.setVolume(incrementVolume * currentSeconds[0], incrementVolume * currentSeconds[0]);
                        currentSeconds[0]++;
                        if(currentSeconds[0] > alarmItem.getVolumeIncreaseTotalSeconds()) {
                            volumeIncreaser.cancel();
                        }
                    }
                }, 0, 1000);
            }
            else {
                mediaPlayer.setVolume(alarmVolume, alarmVolume);
            }
            mediaPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(alarmItem.isVibrate()){
            vibrationTimer = new Timer();
            vibrationTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    AsyncTask.execute(() -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vib.vibrate(VibrationEffect.createOneShot(1200, VibrationEffect.DEFAULT_AMPLITUDE));
                        } else {
                            vib.vibrate(1200);
                        }
                    });
                }
            }, 0, 1500);    // TODO: make duration and pause changeable
        }

        if(alarmItem.isUseFlashlight() && getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)){
            camManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            try {
                cameraId = camManager.getCameraIdList()[0];
                camManager.setTorchMode(cameraId, true);
                flashlightTimer = new Timer();
                final boolean[] flashlightOn = {true};
                flashlightTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        AsyncTask.execute(() -> {
                            try {
                                camManager.setTorchMode(cameraId, flashlightOn[0]);
                                flashlightOn[0] = !flashlightOn[0];
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        });
                    }
                }, 0, 500);     // TODO: make duration changeable
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void showJournalCreator(JournalTypes type) {
        Intent intent = new Intent(this, DreamJournalEntryEditor.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("type", type.ordinal());
        startActivity(intent);
    }

    private void disableAlarm() {
        mediaPlayer.stop();
        if(volumeIncreaser != null){
            volumeIncreaser.cancel();
        }
        if(vibrationTimer != null){
            vibrationTimer.cancel();
        }
        if(flashlightTimer != null){
            flashlightTimer.cancel();
        }
        if(camManager != null){
            try {
                camManager.setTorchMode(cameraId, false);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        disableAlarm();
        finishAndRemoveTask();
    }
}