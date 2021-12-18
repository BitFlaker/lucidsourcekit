package com.bitflaker.lucidsourcekit;

import android.content.Intent;
import android.os.Bundle;
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

import com.bitflaker.lucidsourcekit.general.Crypt;
import com.bitflaker.lucidsourcekit.general.DatabaseWrapper;
import com.bitflaker.lucidsourcekit.general.StoredSettings;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.bitflaker.lucidsourcekit.main.MainViewer;
import com.bitflaker.lucidsourcekit.setup.SetupViewer;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {

    private AlphaAnimation buttonClick = new AlphaAnimation(1F, 0.5F);
    private String enteredPin = "";
    private DatabaseWrapper dbWrapper;
    private String storedHash = "";
    private byte[] storedSalt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dbWrapper = new DatabaseWrapper(this);
        Tools.loadLanguage(this);
        Tools.makeStatusBarTransparent(this);

        // TODO: set language of controls

        String path = getFilesDir().getAbsolutePath() + "/.app_setup_done";
        File file = new File(path);
        if(file.exists()) {

            StoredSettings authType = dbWrapper.GetProperty("auth_type");

            switch (authType.getValue()) {
                case "password":
                    setupPasswordAuthentication();
                    if(dbWrapper.GetProperty("auth_use_biometrics").getValue().equals("true")) {
                        setupBiometricAuthentication();     // TODO: should be callable again if cancelled
                    }
                    storedHash = dbWrapper.GetProperty("auth_hash").getValue();
                    storedSalt = Base64.decode(dbWrapper.GetProperty("auth_salt").getValue(), Base64.DEFAULT);
                    break;
                case "pin":
                    setupPinAuthentication();
                    if(dbWrapper.GetProperty("auth_use_biometrics").getValue().equals("true")) {
                        setupBiometricAuthentication();     // TODO: should be callable again if cancelled
                    }
                    storedHash = dbWrapper.GetProperty("auth_cipher").getValue();
                    storedSalt = Base64.decode(dbWrapper.GetProperty("auth_key").getValue(), Base64.DEFAULT);
                    break;
                case "none":
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

        for (MaterialButton pinButton : pinButtons) {
            pinButton.setOnClickListener(e -> {
                pinButton.startAnimation(buttonClick);
                TextView txtEnteredPin = (TextView)findViewById(R.id.txt_enteredPin);
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

        btn_delete.setOnClickListener(e -> {
            btn_delete.startAnimation(buttonClick);
            if(enteredPin.length() > 0) {
                enteredPin = enteredPin.substring(0, enteredPin.length() - 1);
                TextView txtEnteredPin = (TextView)findViewById(R.id.txt_enteredPin);
                StringBuilder pinText = new StringBuilder();
                for(int i = 0; i < enteredPin.length(); i++) { pinText.append("\u2022"); }
                txtEnteredPin.setText(pinText.toString());
            }
        });
    }
}