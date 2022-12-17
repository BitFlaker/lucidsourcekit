package com.bitflaker.lucidsourcekit.alarms;

import android.animation.ValueAnimator;
import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.alarms.updated.AlarmHandler;
import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.alarms.updated.entities.StoredAlarm;
import com.bitflaker.lucidsourcekit.general.JournalTypes;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.bitflaker.lucidsourcekit.main.dreamjournal.DreamJournalEntryEditor;
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
    private ImageButton closeViewer;
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
    private LinearLayout buttonContainer;
    private MaterialCardView sliderContainer;
    private final AlarmStopByMode alarmStopByMode = AlarmStopByMode.BUTTON;
    private final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.FULL);
    private final DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_viewer_alternative);

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
//        quickAccessActionsView = findViewById(R.id.rcv_quick_access_actions);
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

        alarmSlider.setData(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_check_24, getTheme()), ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_snooze_24, getTheme()));

        // The alarm id is missing, which should not be possible when the alarm was triggered from the alarm receiver
        if(!getIntent().hasExtra("STORED_ALARM_ID")) { finish(); }

        int id = getIntent().getIntExtra("STORED_ALARM_ID", -1);
        MainDatabase.getInstance(this).getStoredAlarmDao().getById(id).subscribe(alarm -> {
            storedAlarm = alarm;
            alarmName.setText(alarm.title);
        }).dispose();

//        quickAccessActionsView.setVisibility(View.GONE);
        closeViewer.setVisibility(View.GONE);
//        quickAccessActionsView.setAlpha(0);
        closeViewer.setAlpha(0.0f);

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

//        List<QuickAccessAction> quickAccessActions = new ArrayList<>();
//        quickAccessActions.add(new QuickAccessAction("Add text journal entry", "Write down your dreams now so you do not forget them", ResourcesCompat.getDrawable(getResources(), R.drawable.ic_round_book_24, getTheme()), ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_text_fields_24, getTheme()), () -> {
//            showJournalCreator(JournalTypes.Text);
//        }));
//        quickAccessActions.add(new QuickAccessAction("Add forms journal entry", "Write down your dreams into the set template for writing them down quickly and easily", ResourcesCompat.getDrawable(getResources(), R.drawable.ic_round_book_24, getTheme()), ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_ballot_24, getTheme()), () -> {
//            showJournalCreator(JournalTypes.Forms);
//        }));
//        quickAccessActions.add(new QuickAccessAction("Listen to binaural beats", "Listening to binaural beats while going back to sleep might help to induce lucid dreams", ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_bedtime_24, getTheme()), null, () -> {
//            Intent intent = new Intent(this, IsolatedBinauralBeatsView.class);
//            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//            startActivity(intent);
//        }));
//        RecyclerViewAdapterQuickAccessActions rvQuickAccessActions = new RecyclerViewAdapterQuickAccessActions(this, quickAccessActions);
//        rvQuickAccessActions.setOnEntryClickedListener(quickAccessAction -> quickAccessAction.getOnSelectedListener().onEvent());
//        quickAccessActionsView.setLayoutManager(new LinearLayoutManager(this));
//        quickAccessActionsView.setAdapter(rvQuickAccessActions);
        closeViewer.setOnClickListener(e -> finishAndRemoveTask());

        Calendar cal = Calendar.getInstance();
        currentTimeView.setText(timeFormat.format(cal.getTime()));
        currentDateView.setText(dateFormat.format(cal.getTime()));
//        currentTimeView.setText(String.format(Locale.ENGLISH, "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)));
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
//                runOnUiThread(() -> currentTimeView.setText(String.format(Locale.ENGLISH, "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))));
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
            closeViewer.setVisibility(View.VISIBLE);
            ValueAnimator opacityAnim = ValueAnimator.ofFloat(0, 1);
            opacityAnim.setDuration(300);
            opacityAnim.setInterpolator(new LinearInterpolator());
            opacityAnim.addUpdateListener((valueAnimator) -> {
//                quickAccessActionsView.setAlpha((float)valueAnimator.getAnimatedValue());
                closeViewer.setAlpha((float)valueAnimator.getAnimatedValue());
            });
            opacityAnim.start();
        }
        else {
            finishAndRemoveTask();
        }
    }

    private void snoozeAlarmSelected() {
        isSnoozing = true;
        resetAlarmActivity();
        Intent intent = new Intent(this, AlarmReceiverManager.class);
        intent.putExtra("SNOOZING_STORED_ALARM_ID", storedAlarm.alarmId);
        alarmIntent = PendingIntent.getBroadcast(getApplicationContext(), AlarmHandler.SNOOZING_ALARM_REQUEST_CODE_START_VALUE + storedAlarm.alarmId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (5*60*1000), alarmIntent); // TODO make snooze delay changeable
        finishAndRemoveTask();
    }

    private void stopAlarmSelected() {
        isSnoozing = false;
        resetAlarmActivity();
    }

    private void setCameraIdWithFlashlight() throws CameraAccessException {
        cameraId = null;
        String[] cameraIds = camManager.getCameraIdList();
        for (String camId : cameraIds) {
            boolean flashAvailable = camManager.getCameraCharacteristics(camId).get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            if(flashAvailable){
                cameraId = camId;
                break;
            }
        }
    }

    private void showJournalCreator(JournalTypes type) {
        Intent intent = new Intent(this, DreamJournalEntryEditor.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("type", type.ordinal());
        startActivity(intent);
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