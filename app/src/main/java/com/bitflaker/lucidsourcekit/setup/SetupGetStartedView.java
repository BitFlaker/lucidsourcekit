package com.bitflaker.lucidsourcekit.setup;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.bitflaker.lucidsourcekit.MainActivity;
import com.bitflaker.lucidsourcekit.databinding.ActivitySetupGetStartedBinding;
import com.bitflaker.lucidsourcekit.utils.Tools;

public class SetupGetStartedView extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivitySetupGetStartedBinding binding = ActivitySetupGetStartedBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Tools.makeStatusBarTransparent(this);
        binding.btnGetStarted.setOnClickListener(e -> {
            Intent intent = new Intent(SetupGetStartedView.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }
}