package com.bitflaker.lucidsourcekit;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.DreamClarity;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.DreamMood;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.DreamType;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.SleepQuality;
import com.bitflaker.lucidsourcekit.database.goals.entities.defaults.DefaultGoals;
import com.bitflaker.lucidsourcekit.general.Crypt;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.bitflaker.lucidsourcekit.main.MainViewer;
import com.bitflaker.lucidsourcekit.setup.SetupViewer;
import com.google.android.material.button.MaterialButton;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private AlphaAnimation buttonClick = new AlphaAnimation(1F, 0.5F);
    private String enteredPin = "";
    private String storedHash = "";
    private byte[] storedSalt;

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
        
        /*
        String filename = getFilesDir().getAbsolutePath() + "/Recordings";
        File directory = new File(filename);
        File[] files = directory.listFiles();
        for (int i = 0; i < files.length; i++) {
            System.out.println(files[i].getName());
        }
         */

        // TODO: only insert when necessary and add loading
        // TODO: add loading indicator
        MainDatabase db = MainDatabase.getInstance(MainActivity.this);
        db.getDreamTypeDao().insertAll(DreamType.populateData()).subscribe(() -> {
            db.getDreamMoodDao().insertAll(DreamMood.populateData()).subscribe(() -> {
                db.getDreamClarityDao().insertAll(DreamClarity.populateData()).subscribe(() -> {
                    db.getSleepQualityDao().insertAll(SleepQuality.populateData()).subscribe(() -> {
                        db.getGoalDao().getGoalCount().subscribe((count) -> {
                            if(count == 0) {
                                DefaultGoals defaultGoals = new DefaultGoals(this);
                                db.getGoalDao().insertAll(defaultGoals.getGoalsList()).subscribe(() -> {
                                    // TODO: hide loading indicator
                                });
                            }
                            else {
                                // TODO: hide loading indicator
                            }
                        });
                    });
                });
            });
        });

        /*
        JournalDatabase db = JournalDatabase.getInstance(MainActivity.this);
        System.out.println("==== start of reading database ====");

        List<SleepQuality> sqs = db.sleepQualityDao().getAll();
        for (SleepQuality sq : sqs) {
            System.out.println(sq.qualityId + " | " + sq.description);
        }

        System.out.println("==== end  of reading database ====");
         */

        // TODO: set language of controls

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

    private void setupPasswordAuthentication() {
        ((TextView)findViewById(R.id.txt_password)).setVisibility(View.VISIBLE);
        ((TextView)findViewById(R.id.btn_unlock)).setVisibility(View.VISIBLE);
        ((LinearLayout)findViewById(R.id.ll_pinLayout)).setVisibility(View.GONE);
        MaterialButton unlockButton = findViewById(R.id.btn_unlock);
        unlockButton.setOnClickListener(e -> {
            try {
                String hash = Crypt.encryptString(String.valueOf(((TextView) findViewById(R.id.txt_password)).getText()), storedSalt);
                if(hash.equals(storedHash)){
                    Intent intent = new Intent(MainActivity.this, MainViewer.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                }
                else {
                    Toast.makeText(MainActivity.this, "invalid password!", Toast.LENGTH_SHORT).show();
                }
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
        ((TextView)findViewById(R.id.txt_password)).setVisibility(View.GONE);
        ((TextView)findViewById(R.id.btn_unlock)).setVisibility(View.GONE);
        ((LinearLayout)findViewById(R.id.ll_pinLayout)).setVisibility(View.VISIBLE);
        buttonClick.setDuration(100);

        MaterialButton btn_pin0 = (MaterialButton)findViewById(R.id.btn_pin0);
        MaterialButton btn_pin1 = (MaterialButton)findViewById(R.id.btn_pin1);
        MaterialButton btn_pin2 = (MaterialButton)findViewById(R.id.btn_pin2);
        MaterialButton btn_pin3 = (MaterialButton)findViewById(R.id.btn_pin3);
        MaterialButton btn_pin4 = (MaterialButton)findViewById(R.id.btn_pin4);
        MaterialButton btn_pin5 = (MaterialButton)findViewById(R.id.btn_pin5);
        MaterialButton btn_pin6 = (MaterialButton)findViewById(R.id.btn_pin6);
        MaterialButton btn_pin7 = (MaterialButton)findViewById(R.id.btn_pin7);
        MaterialButton btn_pin8 = (MaterialButton)findViewById(R.id.btn_pin8);
        MaterialButton btn_pin9 = (MaterialButton)findViewById(R.id.btn_pin9);
        MaterialButton[] pinButtons = { btn_pin0, btn_pin1, btn_pin2, btn_pin3, btn_pin4, btn_pin5, btn_pin6, btn_pin7, btn_pin8, btn_pin9};
        MaterialButton btn_delete = (MaterialButton)findViewById(R.id.btn_delete);
        TextView txtEnteredPin = (TextView)findViewById(R.id.txt_enteredPin);
        txtEnteredPin.setTextColor(getResources().getColor(R.color.white, getTheme()));

        for (MaterialButton pinButton : pinButtons) {
            pinButton.setTextColor(getResources().getColor(R.color.white, getTheme()));
            pinButton.setOnClickListener(e -> {
                pinButton.startAnimation(buttonClick);
                txtEnteredPin.setText(txtEnteredPin.getText() + "\u2022");
                enteredPin += pinButton.getText();

                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        boolean success = false;
                        try {
                            String hash = Crypt.encryptStringBlowfish(enteredPin, storedSalt);
                            if(hash.equals(storedHash)){
                                success = true;
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }

                        boolean finalSuccess = success;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(finalSuccess){
                                    Intent intent = new Intent(MainActivity.this, MainViewer.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    startActivity(intent);
                                    finish();
                                }
                                else{
                                    if(enteredPin.length() > 7) {
                                        enteredPin = "";
                                        txtEnteredPin.setText("");
                                        Toast.makeText(MainActivity.this, "Invalid PIN entered!", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        });
                    }
                };
                thread.start();
            });
        }

        btn_delete.setTextColor(getResources().getColor(R.color.white, getTheme()));
        btn_delete.setOnClickListener(e -> {
            btn_delete.startAnimation(buttonClick);
            if(enteredPin.length() > 0) {
                enteredPin = enteredPin.substring(0, enteredPin.length() - 1);
                StringBuilder pinText = new StringBuilder();
                for(int i = 0; i < enteredPin.length(); i++) { pinText.append("\u2022"); }
                txtEnteredPin.setText(pinText.toString());
            }
        });
    }
}