package com.bitflaker.lucidsourcekit.setup;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.bitflaker.lucidsourcekit.MainActivity;
import com.bitflaker.lucidsourcekit.R;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.IOException;

public class SetupGetStarted extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_started);
        ((MaterialButton) findViewById(R.id.btn_get_started)).setOnClickListener(e -> {
            String path = getFilesDir().getAbsolutePath() + "/.app_setup_done";
            File file = new File(path);
            try {
                file.createNewFile();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }

            path = getFilesDir().getAbsolutePath() + "/Recordings";
            File recFolder = new File(path);
            if(recFolder.mkdir()){
                Intent intent = new Intent(SetupGetStarted.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });
    }
}