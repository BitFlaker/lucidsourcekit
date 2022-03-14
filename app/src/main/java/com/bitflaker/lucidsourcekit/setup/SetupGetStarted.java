package com.bitflaker.lucidsourcekit.setup;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.bitflaker.lucidsourcekit.MainActivity;
import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.database.JournalDatabase;
import com.bitflaker.lucidsourcekit.database.entities.DreamClarity;
import com.bitflaker.lucidsourcekit.database.entities.DreamMood;
import com.bitflaker.lucidsourcekit.database.entities.DreamType;
import com.bitflaker.lucidsourcekit.database.entities.SleepQuality;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.IOException;

public class SetupGetStarted extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_started);

        // TODO: only insert when necessary and add loading
        // TODO: add loading indicator
        JournalDatabase db = JournalDatabase.getInstance(SetupGetStarted.this);
        db.dreamTypeDao().insertAll(DreamType.populateData()).subscribe(() -> {
            db.dreamMoodDao().insertAll(DreamMood.populateData()).subscribe(() -> {
                db.dreamClarityDao().insertAll(DreamClarity.populateData()).subscribe(() -> {
                    db.sleepQualityDao().insertAll(SleepQuality.populateData()).subscribe(() -> {
                        // TODO: hide loading indicator
                        /*

                        System.out.println("++++++++++");
                        db.dreamTypeDao().getAll().subscribe((dreamTypes, throwable) -> {
                            for (DreamType dt : dreamTypes) {
                                System.out.println(dt.typeId);
                            }
                        });
                        System.out.println("++++++++++");
                        db.dreamMoodDao().getAll().subscribe((dreamTypes, throwable) -> {
                            for (DreamMood dt : dreamTypes) {
                                System.out.println(dt.moodId);
                            }
                        });
                        System.out.println("++++++++++");
                        db.dreamClarityDao().getAll().subscribe((dreamTypes, throwable) -> {
                            for (DreamClarity dt : dreamTypes) {
                                System.out.println(dt.clarityId);
                            }
                        });
                        System.out.println("++++++++++");
                        db.sleepQualityDao().getAll().subscribe((dreamTypes, throwable) -> {
                            for (SleepQuality dt : dreamTypes) {
                                System.out.println(dt.qualityId);
                            }
                        });
                        System.out.println("++++++++++");

                         */
                    });
                });
            });
        });
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