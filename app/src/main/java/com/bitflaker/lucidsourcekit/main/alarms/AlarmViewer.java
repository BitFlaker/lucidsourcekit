package com.bitflaker.lucidsourcekit.main.alarms;

import android.animation.ValueAnimator;
import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.bitflaker.lucidsourcekit.MainActivity;
import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.alarms.updated.entities.StoredAlarm;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.resulttables.DreamJournalEntry;
import com.bitflaker.lucidsourcekit.utils.Tools;
import com.bitflaker.lucidsourcekit.views.OutsideSlider;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

public class AlarmViewer extends AppCompatActivity {
    private AlarmManager alarmManager;
    private PendingIntent alarmIntent;
    private OutsideSlider alarmSlider;
//    private RecyclerView quickAccessActionsView;
    private ImageButton closeViewer, openJournal, openBinauralBeatsPlayer, openApp;
    private TextView currentTimeView, currentDateView, alarmName;
    private boolean isSnoozing = false;
    private StoredAlarm storedAlarm;
    private MediaPlayer mediaPlayer;
    private Timer vibrationTimer;
    private Timer flashlightTimer;
    private Vibrator vib;
    private String cameraId;
    private CameraManager camManager;
    private ValueAnimator volumeIncreaseAnimation;
    private MaterialButton snoozeAlarmButton, stopAlarmButton;
    private LinearLayout buttonContainer, quickAccessActionsContainer;
    private MaterialCardView sliderContainer;
    private final AlarmStopByMode alarmStopByMode = AlarmStopByMode.SWIPE;
    private final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.FULL);
    private final DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        setTheme(Tools.getTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_viewer);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            keyguardManager.requestDismissKeyguard(this, null);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        Tools.makeStatusBarTransparent(this);
        alarmSlider = findViewById(R.id.oss_alarm_slider);
        closeViewer = findViewById(R.id.btn_close_viewer);
        currentTimeView = findViewById(R.id.txt_current_time);
        currentDateView = findViewById(R.id.txt_current_date);
        alarmName = findViewById(R.id.txt_alarm_name);
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        mediaPlayer = new MediaPlayer();
        vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        snoozeAlarmButton = findViewById(R.id.btn_snooze);
        stopAlarmButton = findViewById(R.id.btn_stop_alarm);
        buttonContainer = findViewById(R.id.ll_alarm_stop_button_container);
        sliderContainer = findViewById(R.id.crd_alarm_slider_container);
        quickAccessActionsContainer = findViewById(R.id.ll_quick_access_actions);
        openJournal = findViewById(R.id.btn_open_journal);
        openBinauralBeatsPlayer = findViewById(R.id.btn_open_binaural_player);
        openApp = findViewById(R.id.btn_open_app);

        alarmSlider.setData(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_check_24, getTheme()), ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_snooze_24, getTheme()));

        // The alarm id is missing, which should not be possible when the alarm was triggered from the alarm receiver
        if(!getIntent().hasExtra("STORED_ALARM_ID")) { finish(); }

        long id = getIntent().getLongExtra("STORED_ALARM_ID", -1);
        // TODO: ERROR: returned empty result!!
        MainDatabase.getInstance(this).getStoredAlarmDao().getById(id).subscribe(alarm -> {
            storedAlarm = alarm;
            alarmName.setText(alarm.title);
        }).dispose();

        quickAccessActionsContainer.setVisibility(View.GONE);
        quickAccessActionsContainer.setAlpha(0.0f);

        if(alarmStopByMode == AlarmStopByMode.BUTTON){
            buttonContainer.setVisibility(View.VISIBLE);
            sliderContainer.setVisibility(View.GONE);
            stopAlarmButton.setOnClickListener(e -> {
                stopAlarmSelected();
                buttonContainer.setVisibility(View.GONE);
                showAfterAlarmStopControls();
            });
            snoozeAlarmButton.setOnClickListener(e -> snoozeAlarmSelected());
        }
        else if (alarmStopByMode == AlarmStopByMode.SWIPE){
            sliderContainer.setVisibility(View.VISIBLE);
            buttonContainer.setVisibility(View.GONE);
            alarmSlider.setOnLeftSideSelectedListener(this::stopAlarmSelected);
            alarmSlider.setOnRightSideSelectedListener(this::snoozeAlarmSelected);
        }

        alarmSlider.setOnFadedAwayListener(() -> {
            sliderContainer.setVisibility(View.GONE);
            showAfterAlarmStopControls();
        });

        openBinauralBeatsPlayer.setOnClickListener(e -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("INITIAL_PAGE", "binaural");
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finishAffinity();
        });
        openJournal.setOnClickListener(e -> {
            DreamJournalEntryTypeDialog dialog = new DreamJournalEntryTypeDialog(this);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.setOnEntryTypeSelected(this::showJournalCreator);
            dialog.show();
            dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        });
        openApp.setOnClickListener(e -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finishAffinity();
        });

        closeViewer.setOnClickListener(e -> finishAndRemoveTask());

        Calendar cal = Calendar.getInstance();
        currentTimeView.setText(timeFormat.format(cal.getTime()));
        currentDateView.setText(dateFormat.format(cal.getTime()));
        cal.set(Calendar.MINUTE, cal.get(Calendar.MINUTE) + 1);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        Timer clockTimer = new Timer();
        clockTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Calendar cal = Calendar.getInstance();
                runOnUiThread(() -> {
                    currentTimeView.setText(timeFormat.format(cal.getTime()));
                    currentDateView.setText(dateFormat.format(cal.getTime()));
                });
            }
        }, cal.getTimeInMillis() - Calendar.getInstance().getTimeInMillis(), 60*1000);

        try {
            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .build()
            );
            mediaPlayer.setLooping(true);
            mediaPlayer.setDataSource(getApplicationContext(), Uri.parse(storedAlarm.alarmUri));
            mediaPlayer.prepare();
            if(storedAlarm.alarmVolumeIncreaseTimestamp != 0){
                volumeIncreaseAnimation = ValueAnimator.ofFloat(0, storedAlarm.alarmVolume);
                volumeIncreaseAnimation.setDuration(storedAlarm.alarmVolumeIncreaseTimestamp);
                volumeIncreaseAnimation.setInterpolator(new LinearInterpolator());
                volumeIncreaseAnimation.addUpdateListener((valueAnimator) -> {
                    float currentVolume = (float) valueAnimator.getAnimatedValue();
                    mediaPlayer.setVolume(currentVolume, currentVolume);
                });
                volumeIncreaseAnimation.start();
            }
            else {
                mediaPlayer.setVolume(storedAlarm.alarmVolume, storedAlarm.alarmVolume);
            }
            mediaPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(storedAlarm.isVibrationActive){
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

        if(storedAlarm.isFlashlightActive && getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)){
            camManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            try {
                setCameraIdWithFlashlight();
                if(cameraId != null){
                    camManager.setTorchMode(cameraId, true);
                    flashlightTimer = new Timer();
                    final boolean[] flashlightOn = { true };
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
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void showAfterAlarmStopControls() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if(!isSnoozing){
//            quickAccessActionsView.setVisibility(View.VISIBLE);
            quickAccessActionsContainer.setVisibility(View.VISIBLE);
            ValueAnimator opacityAnim = ValueAnimator.ofFloat(0, 1);
            opacityAnim.setDuration(300);
            opacityAnim.setInterpolator(new LinearInterpolator());
            opacityAnim.addUpdateListener((valueAnimator) -> {
//                quickAccessActionsView.setAlpha((float)valueAnimator.getAnimatedValue());
                quickAccessActionsContainer.setAlpha((float)valueAnimator.getAnimatedValue());
            });
            opacityAnim.start();
        }
        else {
            finish();
        }
    }

    private void snoozeAlarmSelected() {
        isSnoozing = true;
        resetAlarmActivity();
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("SNOOZING_STORED_ALARM_ID", storedAlarm.alarmId);
        alarmIntent = PendingIntent.getBroadcast(getApplicationContext(), AlarmHandler.SNOOZING_ALARM_REQUEST_CODE_START_VALUE + (int)storedAlarm.alarmId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (5*60*1000), alarmIntent); // TODO make snooze delay changeable
        finish();
    }

    private void stopAlarmSelected() {
        isSnoozing = false;
        resetAlarmActivity();
    }

    private void setCameraIdWithFlashlight() throws CameraAccessException {
        cameraId = null;
        String[] cameraIds = camManager.getCameraIdList();
        for (String camId : cameraIds) {
            boolean flashAvailable = Boolean.TRUE.equals(camManager.getCameraCharacteristics(camId).get(CameraCharacteristics.FLASH_INFO_AVAILABLE));
            if(flashAvailable){
                cameraId = camId;
                break;
            }
        }
    }

    private void showJournalCreator(DreamJournalEntry.EntryType type) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("DREAM_JOURNAL_TYPE", type.ordinal());
        intent.putExtra("INITIAL_PAGE", "journal");
        startActivity(intent);
        finishAffinity();
    }

    private void resetAlarmActivity() {
        mediaPlayer.stop();
        if(vibrationTimer != null){
            vibrationTimer.cancel();
        }
        if(flashlightTimer != null){
            flashlightTimer.cancel();
        }
        if(volumeIncreaseAnimation != null && volumeIncreaseAnimation.isRunning()){
            volumeIncreaseAnimation.cancel();
        }
        if(camManager != null){
            try {
                if(cameraId != null){
                    camManager.setTorchMode(cameraId, false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        resetAlarmActivity();
        finishAndRemoveTask();
    }

    public enum AlarmStopByMode {
        SWIPE,
        BUTTON
    }
}