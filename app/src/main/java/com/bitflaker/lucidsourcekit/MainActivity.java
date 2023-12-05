package com.bitflaker.lucidsourcekit;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.bitflaker.lucidsourcekit.alarms.updated.AlarmHandler;
import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.alarms.entities.AlarmToneTypes;
import com.bitflaker.lucidsourcekit.database.alarms.entities.Weekdays;
import com.bitflaker.lucidsourcekit.database.alarms.updated.entities.ActiveAlarm;
import com.bitflaker.lucidsourcekit.database.alarms.updated.entities.ActiveAlarmDetails;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.DreamClarity;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.DreamMood;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.DreamType;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.SleepQuality;
import com.bitflaker.lucidsourcekit.database.goals.entities.Goal;
import com.bitflaker.lucidsourcekit.database.goals.entities.Shuffle;
import com.bitflaker.lucidsourcekit.database.goals.entities.ShuffleHasGoal;
import com.bitflaker.lucidsourcekit.database.goals.entities.defaults.DefaultGoals;
import com.bitflaker.lucidsourcekit.database.notifications.entities.NotificationCategory;
import com.bitflaker.lucidsourcekit.database.notifications.entities.NotificationObfuscations;
import com.bitflaker.lucidsourcekit.general.Crypt;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.bitflaker.lucidsourcekit.general.datastore.DataStoreKeys;
import com.bitflaker.lucidsourcekit.general.datastore.DataStoreManager;
import com.bitflaker.lucidsourcekit.general.datastore.DataStoreMigrator;
import com.bitflaker.lucidsourcekit.main.MainViewer;
import com.bitflaker.lucidsourcekit.setup.SetupViewer;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.flexbox.JustifyContent;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private final char[] pinLayout = new char[] { '1', '2', '3', '4', '5', '6', '7', '8', '9', ' ', '0', '<' };
    private final StringBuilder enteredPin = new StringBuilder();
    private String storedHash = "";
    private byte[] storedSalt;
    private int pinButtonSize = 68;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO: remove enforce dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        switch (currentNightMode) {
            case Configuration.UI_MODE_NIGHT_NO:
                Tools.setThemeColors(R.style.Theme_LucidSourceKit_Light);
                break;
            case Configuration.UI_MODE_NIGHT_YES:
                Tools.setThemeColors(R.style.Theme_LucidSourceKit_Dark);
                break;
        }
        setTheme(Tools.getTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DataStoreManager.initialize(this);
        DataStoreMigrator.migrateSharedPreferencesToDataStore(this);
        DataStoreMigrator.migrateSetupFinishedToDataStore(this);

        Tools.loadLanguage(MainActivity.this);
        Tools.makeStatusBarTransparent(MainActivity.this);

        findViewById(R.id.btn_fingerprint_unlock).setOnClickListener(e -> startBiometricAuthentication());
        pinButtonSize = getResources().getBoolean(R.bool.is_h720dp) ? 68 : 54;

        // TODO: set language of controls

        Calendar cldrStored = new GregorianCalendar(TimeZone.getDefault());
        Calendar cldrNow = new GregorianCalendar(TimeZone.getDefault());
        cldrNow.setTime(Calendar.getInstance().getTime());
        cldrNow.set(Calendar.HOUR_OF_DAY, 0);
        cldrNow.set(Calendar.MINUTE, 0);
        cldrNow.set(Calendar.SECOND, 0);
        cldrNow.set(Calendar.MILLISECOND, 0);

        DataStoreManager dsManager = DataStoreManager.getInstance();
        long storedTime = dsManager.getSetting(DataStoreKeys.FIRST_OPEN_LATEST_DAY).blockingFirst();
        if(storedTime == 0) {
            dsManager.updateSetting(DataStoreKeys.FIRST_OPEN_LATEST_DAY, cldrNow.getTimeInMillis()).blockingSubscribe();
            dsManager.updateSetting(DataStoreKeys.APP_OPEN_STREAK, 0L).blockingSubscribe();
            dsManager.updateSetting(DataStoreKeys.APP_OPEN_STREAK_LONGEST, 0L).blockingSubscribe();
        }
        else {
            // TODO: take daylight saving time and different country times in consideration
            cldrStored.setTimeInMillis(storedTime);
            long diff = cldrNow.getTimeInMillis() - cldrStored.getTimeInMillis();
            long dayLength = TimeUnit.DAYS.toMillis(1);
            if(diff == dayLength) {
                long appOpenStreak = dsManager.getSetting(DataStoreKeys.APP_OPEN_STREAK).blockingFirst();
                long currentAppOpenStreak = appOpenStreak + 1;
                dsManager.updateSetting(DataStoreKeys.FIRST_OPEN_LATEST_DAY, cldrNow.getTimeInMillis()).blockingSubscribe();
                dsManager.updateSetting(DataStoreKeys.APP_OPEN_STREAK, currentAppOpenStreak).blockingSubscribe();
                long appOpenStreakLongest = dsManager.getSetting(DataStoreKeys.APP_OPEN_STREAK_LONGEST).blockingFirst();
                if(currentAppOpenStreak > appOpenStreakLongest){
                    dsManager.updateSetting(DataStoreKeys.APP_OPEN_STREAK_LONGEST, currentAppOpenStreak).blockingSubscribe();
                }
            }
            else if (diff > dayLength || diff < 0) {
                dsManager.updateSetting(DataStoreKeys.FIRST_OPEN_LATEST_DAY, cldrNow.getTimeInMillis()).blockingSubscribe();
                dsManager.updateSetting(DataStoreKeys.APP_OPEN_STREAK, 0L).blockingSubscribe();
            }
        }

        // TODO: only insert when necessary
        // TODO: add loading indicator
        new Thread(() -> {
            MainDatabase db = MainDatabase.getInstance(MainActivity.this);

            db.getActiveAlarmDao().insert(ActiveAlarm.createUnreferencedAlarm()).blockingSubscribe();
            List<ActiveAlarmDetails> activeAlarms = db.getActiveAlarmDao().getAllDetails().blockingGet();
            AlarmHandler.reEnableAlarmsIfNotRunning(getApplicationContext(), activeAlarms);

            // Populate all default data in database
            db.getDreamTypeDao().insertAll(DreamType.populateData()).blockingSubscribe();
            db.getDreamMoodDao().insertAll(DreamMood.populateData()).blockingSubscribe();
            db.getDreamClarityDao().insertAll(DreamClarity.populateData()).blockingSubscribe();
            db.getSleepQualityDao().insertAll(SleepQuality.populateData()).blockingSubscribe();
            db.getWeekdaysDao().insertAll(Weekdays.populateData()).blockingSubscribe();
            db.getAlarmToneTypesDao().insertAll(AlarmToneTypes.populateData()).blockingSubscribe();
            db.getNotificationObfuscationDao().insertAll(NotificationObfuscations.populateData()).blockingSubscribe();
            db.getNotificationCategoryDao().insertAll(NotificationCategory.populateData()).blockingSubscribe();
            if (db.getGoalDao().getGoalCount().blockingGet() == 0) {    // Only add default goals in case no goals are present in the database
                db.getGoalDao().insertAll(new DefaultGoals(this).getGoalsList()).blockingSubscribe();
            }

            AlarmHandler.createNotificationChannels(this).blockingSubscribe();
            AlarmHandler.reEnableNotificationsIfNotRunning(getApplicationContext());

            String path = getFilesDir().getAbsolutePath() + "/Recordings";
            File recFolder = new File(path);
            if(!recFolder.exists() && !recFolder.mkdir()) {
                runOnUiThread(() -> {
                    Log.e("MainActivity", "Unable to create recordings directory!");
                    setResult(RESULT_CANCELED);
                    finish();
                });
                return;
            }
            shuffleGoalsForToday(db);
        }).start();
    }

    private void applicationLogin() {
        DataStoreManager dsManager = DataStoreManager.getInstance();
        if(!dsManager.getSetting(DataStoreKeys.APP_SETUP_FINISHED).blockingFirst()) {
            Intent intent = new Intent(MainActivity.this, SetupViewer.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
            return;
        }

        boolean useBiometrics = dsManager.getSetting(DataStoreKeys.AUTHENTICATION_USE_BIOMETRICS).blockingFirst();
        String hash = dsManager.getSetting(DataStoreKeys.AUTHENTICATION_HASH).blockingFirst();
        String salt = dsManager.getSetting(DataStoreKeys.AUTHENTICATION_SALT).blockingFirst();
        switch (dsManager.getSetting(DataStoreKeys.AUTHENTICATION_TYPE).blockingFirst()) {
            case "password":
                setupPasswordAuthentication();
                if(useBiometrics) {
                    setupBiometricAuthentication();
                }
                storedHash = hash;
                storedSalt = Base64.decode(salt, Base64.DEFAULT);
                break;
            case "pin":
                setupPinAuthentication();
                if(useBiometrics) {
                    setupBiometricAuthentication();
                }
                storedHash = hash;
                storedSalt = Base64.decode(salt, Base64.DEFAULT);
                break;
            default:
                startLoadingAnimation();
                startMainApp();
                break;
        }
    }

    private void startMainApp() {
        Intent intent = new Intent(MainActivity.this, MainViewer.class);
        if(getIntent().hasExtra("INITIAL_PAGE")){
            intent.putExtra("INITIAL_PAGE", getIntent().getStringExtra("INITIAL_PAGE"));
        }
        if(getIntent().hasExtra("type")){
            intent.putExtra("type", getIntent().getIntExtra("type", -1));
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void shuffleGoalsForToday(MainDatabase db) {
        Pair<Long, Long> dayTimeSpans = Tools.getTimeSpanFrom(0, true);
        db.getShuffleDao().getLastShuffleInDay(dayTimeSpans.first, dayTimeSpans.second).subscribe((shuffle, e) -> {
            if(shuffle == null) {
                List<Goal> goals = db.getGoalDao().getAllSingle().blockingGet();
                List<Goal> goalsResult = Tools.getSuitableGoals(goals);
                Long newShuffleId = db.getShuffleDao().insert(new Shuffle(dayTimeSpans.first, dayTimeSpans.second)).blockingGet();
                List<ShuffleHasGoal> hasGoals = new ArrayList<>();
                for (Goal goal : goalsResult) {
                    hasGoals.add(new ShuffleHasGoal(newShuffleId.intValue(), goal.goalId));
                }
                db.getShuffleHasGoalDao().insertAll(hasGoals).blockingSubscribe();
            }
            runOnUiThread(this::applicationLogin);
        }).dispose();
    }

    private void setupPasswordAuthentication() {
        findViewById(R.id.ll_pw_auth_container).setVisibility(View.VISIBLE);
        findViewById(R.id.ll_pin_auth_container).setVisibility(View.GONE);
        MaterialButton unlockButton = findViewById(R.id.btn_unlock);
        unlockButton.setOnClickListener(e -> {
            try {
                String hash = Crypt.encryptString(String.valueOf(((TextView) findViewById(R.id.txt_password)).getText()), storedSalt);
                runOnUiThread(() -> {
                    if(hash.equals(storedHash)) {
                        startLoadingAnimation();
                        startMainApp();
                    }
                    else {
                        Toast.makeText(MainActivity.this, "invalid password!", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    private void setupBiometricAuthentication() {
        BiometricManager biometricManager = BiometricManager.from(this);
        switch (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                findViewById(R.id.btn_fingerprint_unlock).setVisibility(View.VISIBLE);
                startBiometricAuthentication();
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                Toast.makeText(this, "Biometric failed: No hardware available", Toast.LENGTH_LONG).show();
                break;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                Toast.makeText(this, "Biometric failed: Hardware unavailable", Toast.LENGTH_LONG).show();
                break;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                Toast.makeText(this, "Biometric failed: No biometric/device credentials enrolled", Toast.LENGTH_LONG).show();
                break;
            case BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED:
                Toast.makeText(this, "Biometric failed: Security update required", Toast.LENGTH_LONG).show();
                break;
            case BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED:
                Toast.makeText(this, "Biometric failed: Not supported", Toast.LENGTH_LONG).show();
                break;
            case BiometricManager.BIOMETRIC_STATUS_UNKNOWN:
                Toast.makeText(this, "Biometric failed: Biometric status unknown", Toast.LENGTH_LONG).show();
                break;
        }
    }

    private void startBiometricAuthentication() {
        Executor executor = ContextCompat.getMainExecutor(this);
        final BiometricPrompt biometricPrompt = new BiometricPrompt(MainActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                startLoadingAnimation();
                startMainApp();
            }
        });
        final BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Login")
                .setDescription("Log in using your biometric credentials")
                .setNegativeButtonText("Cancel")
                .build();
        biometricPrompt.authenticate(promptInfo);
    }

    private void setupPinAuthentication() {
        findViewById(R.id.ll_pw_auth_container).setVisibility(View.GONE);
        findViewById(R.id.ll_pin_auth_container).setVisibility(View.VISIBLE);
        LinearLayout pinLayout = findViewById(R.id.ll_pinLayout);
        for (int y = 0; y < 4; y++) {
            FlexboxLayout hFlxBx = new FlexboxLayout(this);
            hFlxBx.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            hFlxBx.setJustifyContent(JustifyContent.SPACE_AROUND);
            for (int x = 0; x < 3; x++) {
                hFlxBx.addView(generatePinButton(y*3+x));
            }
            pinLayout.addView(hFlxBx);
        }
    }

    private View generatePinButton(int pos) {
        MaterialButton pinButton = new MaterialButton(this);
        char currentType = pinLayout[pos];
        LinearLayout.LayoutParams lParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pinButton.setLayoutParams(lParams);
        pinButton.setMinimumHeight(Tools.dpToPx(this, pinButtonSize));
        pinButton.setMinimumWidth(Tools.dpToPx(this, pinButtonSize));
        pinButton.setMinHeight(Tools.dpToPx(this, pinButtonSize));
        pinButton.setMinWidth(Tools.dpToPx(this, pinButtonSize));
        pinButton.setHeight(Tools.dpToPx(this, pinButtonSize));
        pinButton.setWidth(Tools.dpToPx(this, pinButtonSize));
        pinButton.setPadding(0,0,0,0);
        pinButton.setTextColor(Tools.getAttrColor(R.attr.primaryTextColor, getTheme()));
        pinButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        pinButton.setBackgroundResource(R.drawable.ripple_round_clear);
        TextView txtEnteredPin = findViewById(R.id.txt_enteredPin);
        if (Character.isDigit(currentType)) {
            pinButton.setText(Character.toString(currentType));
            int buttonVal = Integer.parseInt(Character.toString(currentType));
            pinButton.setOnClickListener(e -> {
                txtEnteredPin.setText(txtEnteredPin.getText() + "\u2022");
                enteredPin.append(buttonVal);
                new Thread(() -> {
                    boolean success = isPinSuccess();
                    if(success || enteredPin.length() > 7){
                        runOnUiThread(() -> {
                            if(success){
                                startLoadingAnimation();
                                startMainApp();
                            }
                            else {
                                enteredPin.delete(0, enteredPin.length());
                                txtEnteredPin.setText("");
                                Toast.makeText(MainActivity.this, "Invalid PIN entered!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }).start();
            });
            return pinButton;
        }
        else if (currentType == '<') {
            pinButton.setText("\u232B");
            pinButton.setOnClickListener(e -> {
                if(enteredPin.length() > 0) {
                    enteredPin.deleteCharAt(enteredPin.length()-1);
                    StringBuilder pinText = new StringBuilder();
                    for(int i = 0; i < enteredPin.length(); i++) { pinText.append("\u2022"); }
                    txtEnteredPin.setText(pinText.toString());
                }
            });
            return pinButton;
        }
        return generateSpace();
    }

    private void startLoadingAnimation() {
        findViewById(R.id.ll_pw_auth_container).setVisibility(View.GONE);
        findViewById(R.id.ll_pin_auth_container).setVisibility(View.GONE);
        findViewById(R.id.btn_fingerprint_unlock).setVisibility(View.GONE);
        findViewById(R.id.clpb_start_app).setVisibility(View.VISIBLE);
    }

    private Space generateSpace() {
        Space space = new Space(this);
        space.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        space.setMinimumHeight(Tools.dpToPx(this, pinButtonSize));
        space.setMinimumWidth(Tools.dpToPx(this, pinButtonSize));
        return space;
    }

    private boolean isPinSuccess() {
        boolean success = false;
        try {
            String hash = Crypt.encryptStringBlowfish(enteredPin.toString(), storedSalt);
            if(hash != null && hash.equals(storedHash)){ success = true; }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return success;
    }
}