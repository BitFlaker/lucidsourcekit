package com.bitflaker.lucidsourcekit;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PointF;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Pair;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.alarms.entities.AlarmToneTypes;
import com.bitflaker.lucidsourcekit.database.alarms.entities.Weekdays;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.DreamClarity;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.DreamMood;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.DreamType;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.SleepQuality;
import com.bitflaker.lucidsourcekit.database.goals.entities.Goal;
import com.bitflaker.lucidsourcekit.database.goals.entities.Shuffle;
import com.bitflaker.lucidsourcekit.database.goals.entities.ShuffleHasGoal;
import com.bitflaker.lucidsourcekit.database.goals.entities.defaults.DefaultGoals;
import com.bitflaker.lucidsourcekit.general.Crypt;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.bitflaker.lucidsourcekit.main.MainViewer;
import com.bitflaker.lucidsourcekit.setup.SetupViewer;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.flexbox.JustifyContent;
import com.google.android.material.button.MaterialButton;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private AlphaAnimation buttonClick = new AlphaAnimation(1F, 0.5F);
    private char[] pinLayout = new char[] { '1', '2', '3', '4', '5', '6', '7', '8', '9', ' ', '0', '<' };
    private StringBuilder enteredPin = new StringBuilder();
    private String storedHash = "";
    private byte[] storedSalt;
    private int pinButtonSize = 76;

    public static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Tools.setThemeColors(R.style.Theme_LucidSourceKit_Dark);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Tools.loadLanguage(MainActivity.this);
        Tools.makeStatusBarTransparent(MainActivity.this);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();

        // TODO: set language of controls

        Calendar cldrStored = new GregorianCalendar(TimeZone.getDefault());
        Calendar cldrNow = new GregorianCalendar(TimeZone.getDefault());
        cldrNow.setTime(Calendar.getInstance().getTime());
        cldrNow.set(Calendar.HOUR_OF_DAY, 0);
        cldrNow.set(Calendar.MINUTE, 0);
        cldrNow.set(Calendar.SECOND, 0);
        cldrNow.set(Calendar.MILLISECOND, 0);

        long storedTime = preferences.getLong("latest_day_first_open", 0);
        System.out.println("CURRENT STREAK: " + preferences.getLong("app_open_streak", 0));
        if(storedTime == 0) {
            editor.putLong("latest_day_first_open", cldrNow.getTimeInMillis());
            editor.putLong("app_open_streak", 0);
            editor.putLong("longest_app_open_streak", 0);
            System.out.println("TIME WAS NOT YET SET");
        }
        else {
            // TODO: take daylight saving time and different country times in consideration
            cldrStored.setTimeInMillis(storedTime);
            long diff = cldrNow.getTimeInMillis() - cldrStored.getTimeInMillis();
            long dayLength = TimeUnit.DAYS.toMillis(1);
            if(diff == dayLength) {
                long currentAppOpenStreak = preferences.getLong("app_open_streak", 0) + 1;
                editor.putLong("latest_day_first_open", cldrNow.getTimeInMillis());
                editor.putLong("app_open_streak", currentAppOpenStreak);
                if(currentAppOpenStreak > preferences.getLong("longest_app_open_streak", 0)){
                    editor.putLong("longest_app_open_streak", currentAppOpenStreak);
                }
            }
            else if (diff > dayLength || diff < 0) {
                editor.putLong("latest_day_first_open", cldrNow.getTimeInMillis());
                editor.putLong("app_open_streak", 0);
            }
        }
        editor.apply();
        
        // TODO: only insert when necessary
        // TODO: add loading indicator
        new Thread(() -> {
            MainDatabase db = MainDatabase.getInstance(MainActivity.this);
            //db.getShuffleHasGoalDao().deleteAll().subscribe(() -> db.getShuffleDao().deleteAll().subscribe(() -> {
                db.getDreamTypeDao().insertAll(DreamType.populateData()).subscribe(() -> {
                    db.getDreamMoodDao().insertAll(DreamMood.populateData()).subscribe(() -> {
                        db.getDreamClarityDao().insertAll(DreamClarity.populateData()).subscribe(() -> {
                            db.getSleepQualityDao().insertAll(SleepQuality.populateData()).subscribe(() -> {
                                db.getWeekdaysDao().insertAll(Weekdays.populateData()).subscribe(() -> {
                                    db.getAlarmToneTypesDao().insertAll(AlarmToneTypes.populateData()).subscribe(() -> {
                                        db.getGoalDao().getGoalCount().subscribe((count) -> {
                                            if (count == 0) {
                                                DefaultGoals defaultGoals = new DefaultGoals(this);
                                                db.getGoalDao().insertAll(defaultGoals.getGoalsList()).subscribe(() -> {
                                                    dataSetupHandler(db, preferences, count);
                                                });
                                            } else {
                                                dataSetupHandler(db, preferences, count);
                                            }
                                        });
                                    });
                                });
                            });
                        });
                    });
                });
            //}));
        }).start();
    }

    private void applicationLogin(SharedPreferences preferences) {
        String path = getFilesDir().getAbsolutePath() + "/.app_setup_done";
        File file = new File(path);
        if(file.exists()) {
            switch (preferences.getString("auth_type", "")) {
                case "password":
                    setupPasswordAuthentication();
                    if(preferences.getString("auth_use_biometrics", "").equals("true")) {
                        setupBiometricAuthentication();     // TODO: should be callable again if cancelled
                    }
                    storedHash = preferences.getString("auth_hash", "");
                    storedSalt = Base64.decode(preferences.getString("auth_salt", ""), Base64.DEFAULT);
                    break;
                case "pin":
                    setupPinAuthentication();
                    if(preferences.getString("auth_use_biometrics", "").equals("true")) {
                        setupBiometricAuthentication();     // TODO: should be callable again if cancelled
                    }
                    storedHash = preferences.getString("auth_cipher", "");
                    storedSalt = Base64.decode(preferences.getString("auth_key", ""), Base64.DEFAULT);
                    break;
                default:
                    Intent intent = new Intent(MainActivity.this, MainViewer.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                    break;
            }
        }
        else {
            Intent intent = new Intent(MainActivity.this, SetupViewer.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }
    }

    private void dataSetupHandler(MainDatabase db, SharedPreferences preferences, int goalsCount) {
        SharedPreferences.Editor editor = preferences.edit();
        db.getGoalDao().getCountUntilDifficulty(2.0f).subscribe(amount -> {
            if(anyGoalSettingMissing(preferences)) {
                float easyValue = 100.0f;
                float normalValue = 100.0f;
                float hardValue = 100.0f;
                editor.putBoolean("goal_difficulty_auto_adjust", true);
                editor.putFloat("goal_difficulty_easy_value", easyValue);
                editor.putFloat("goal_difficulty_normal_value", normalValue);
                editor.putFloat("goal_difficulty_hard_value", hardValue);
                editor.putFloat("goal_difficulty_tendency", 1.8f);
                editor.putFloat("goal_difficulty_variance", 0.15f);
                editor.putInt("goal_difficulty_count", 3);
                editor.putFloat("goal_difficulty_value_variance", 10.0f);
                editor.putInt("goal_difficulty_accuracy", 100);
                PointF weight1 = new PointF(0f, easyValue);
                PointF weight2 = new PointF(amount-1, normalValue);
                PointF weight3 = new PointF(goalsCount-1, hardValue);
                double[] points = Tools.calculateQuadraticFunction(weight1, weight2, weight3);
                editor.putFloat("goal_function_value_a", (float)points[0]);
                editor.putFloat("goal_function_value_b", (float)points[1]);
                editor.putFloat("goal_function_value_c", (float)points[2]);
            }
            editor.apply();
            Pair<Long, Long> dayTimeSpans = Tools.getTimeSpanFrom(0, true);
            db.getShuffleDao().getLastShuffleInDay(dayTimeSpans.first, dayTimeSpans.second).subscribe((shuffle, throwable) -> {
                if(shuffle == null) {
                    db.getGoalDao().getAllSingle().subscribe((goals, throwable2) -> {
                        List<Goal> goalsResult = Tools.getSuitableGoals(this, goals, preferences.getFloat("goal_difficulty_tendency", 1.8f), preferences.getFloat("goal_difficulty_variance", 0.15f), preferences.getInt("goal_difficulty_accuracy", 100), preferences.getFloat("goal_difficulty_value_variance", 3.0f), preferences.getInt("goal_difficulty_count", 3));
                        db.getShuffleDao().insert(new Shuffle(dayTimeSpans.first, dayTimeSpans.second)).subscribe(newShuffleId -> {
                            int id = newShuffleId.intValue();
                            List<ShuffleHasGoal> hasGoals = new ArrayList<>();
                            for (Goal goal : goalsResult) {
                                hasGoals.add(new ShuffleHasGoal(id, goal.goalId));
                            }
                            db.getShuffleHasGoalDao().insertAll(hasGoals).blockingSubscribe(() -> {
                                // TODO: hide loading indicator
                                runOnUiThread(() -> applicationLogin(preferences));
                            });
                        });
                    });
                }
                else {
                    System.out.println("SHUFFLE " + shuffle.shuffleId + " already present");
                    // TODO: hide loading indicator
                    runOnUiThread(() -> applicationLogin(preferences) );
                }
            });
        });
    }

    private boolean anyGoalSettingMissing(SharedPreferences preferences) {
        return !preferences.contains("goal_difficulty_easy_value") ||
                !preferences.contains("goal_difficulty_normal_value") ||
                !preferences.contains("goal_difficulty_hard_value") ||
                !preferences.contains("goal_difficulty_tendency") ||
                !preferences.contains("goal_difficulty_count") ||
                !preferences.contains("goal_difficulty_accuracy") ||
                !preferences.contains("goal_difficulty_value_variance") ||
                !preferences.contains("goal_difficulty_variance") ||
                !preferences.contains("goal_function_value_a") ||
                !preferences.contains("goal_function_value_b") ||
                !preferences.contains("goal_difficulty_auto_adjust") ||
                !preferences.contains("goal_function_value_c");
    }

    private void setupPasswordAuthentication() {
        findViewById(R.id.txt_password).setVisibility(View.VISIBLE);
        findViewById(R.id.btn_unlock).setVisibility(View.VISIBLE);
        findViewById(R.id.ll_pinLayout).setVisibility(View.GONE);
        MaterialButton unlockButton = findViewById(R.id.btn_unlock);
        unlockButton.setOnClickListener(e -> {
            try {
                String hash = Crypt.encryptString(String.valueOf(((TextView) findViewById(R.id.txt_password)).getText()), storedSalt);
                runOnUiThread(() -> {
                    if(hash.equals(storedHash)) {
                        startLoadingAnimation();
                        Intent intent = new Intent(MainActivity.this, MainViewer.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
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
        // TODO: check if phone even supports biometrics (also do so in first time setup and settings)
        switch (biometricManager.canAuthenticate()) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                // allow login
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                // show error no biometric hardware
                break;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                // show error currently unavailable
                break;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                // show error biometrics not enrolled
                break;
        }

        Executor executor = ContextCompat.getMainExecutor(this);
        final BiometricPrompt biometricPrompt = new BiometricPrompt(MainActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Intent intent = new Intent(MainActivity.this, MainViewer.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });
        final BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder().setTitle("GFG").setDescription("Use your fingerprint to login ").setNegativeButtonText("Cancel").build();
        biometricPrompt.authenticate(promptInfo);
    }

    private void setupPinAuthentication() {
        findViewById(R.id.txt_password).setVisibility(View.GONE);
        findViewById(R.id.btn_unlock).setVisibility(View.GONE);
        LinearLayout pinLayout = findViewById(R.id.ll_pinLayout);
        pinLayout.setVisibility(View.VISIBLE);
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
                                Intent intent = new Intent(MainActivity.this, MainViewer.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);
                                finish();
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
        findViewById(R.id.txt_password).setVisibility(View.GONE);
        findViewById(R.id.btn_unlock).setVisibility(View.GONE);
        findViewById(R.id.ll_pinLayout).setVisibility(View.GONE);
        ProgressBar loadingCircle = findViewById(R.id.clpb_start_app);
        loadingCircle.setVisibility(View.VISIBLE);
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