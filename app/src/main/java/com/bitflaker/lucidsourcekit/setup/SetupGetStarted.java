package com.bitflaker.lucidsourcekit.setup;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.bitflaker.lucidsourcekit.MainActivity;
import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.google.android.material.button.MaterialButton;

public class SetupGetStarted extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_started);
        Tools.makeStatusBarTransparent(this);

        MaterialButton btnGetStarted = findViewById(R.id.btn_get_started);
        btnGetStarted.setOnClickListener(e -> {
            Intent intent = new Intent(SetupGetStarted.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }
}