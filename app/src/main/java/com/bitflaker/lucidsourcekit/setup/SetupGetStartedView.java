package com.bitflaker.lucidsourcekit.setup;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.bitflaker.lucidsourcekit.MainActivity;
import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.utils.Tools;
import com.google.android.material.button.MaterialButton;

public class SetupGetStartedView extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_get_started);
        Tools.makeStatusBarTransparent(this);

        MaterialButton btnGetStarted = findViewById(R.id.btn_get_started);
        btnGetStarted.setOnClickListener(e -> {
            Intent intent = new Intent(SetupGetStartedView.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }
}