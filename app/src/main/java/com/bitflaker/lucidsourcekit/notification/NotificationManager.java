package com.bitflaker.lucidsourcekit.notification;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.general.Tools;

public class NotificationManager extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Tools.getTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_manager);
        Tools.makeStatusBarTransparent(NotificationManager.this);
        ConstraintLayout.LayoutParams lParamsHeading = Tools.getConstraintLayoutParamsTopStatusbar((ConstraintLayout.LayoutParams) findViewById(R.id.txt_manage_notification_heading).getLayoutParams(), NotificationManager.this);
        findViewById(R.id.txt_manage_notification_heading).setLayoutParams(lParamsHeading);
    }
}