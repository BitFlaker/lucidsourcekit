package com.bitflaker.lucidsourcekit.notification;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class NotificationManager extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Tools.getTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_manager);
        Tools.makeStatusBarTransparent(NotificationManager.this);
        ConstraintLayout.LayoutParams lParamsHeading = Tools.getConstraintLayoutParamsTopStatusbar((ConstraintLayout.LayoutParams) findViewById(R.id.txt_manage_notification_heading).getLayoutParams(), NotificationManager.this);
        findViewById(R.id.txt_manage_notification_heading).setLayoutParams(lParamsHeading);

        List<NotificationCategory> categories = new ArrayList<>();

        NotificationCategory ncDreamJournal = new NotificationCategory();
        ncDreamJournal.setActive(false);
        ncDreamJournal.setCategoryHeading("Dream journal reminder");
        ncDreamJournal.setCategoryDescription("Reminder for writing down your dream to the dream journal in order to improve dream recall");
        ncDreamJournal.setNotificationCount(5);
        ncDreamJournal.setCategoryDrawable(R.drawable.ic_baseline_text_fields_24);
        ncDreamJournal.setCategoryClickedListener(() -> {
            // TODO: open up bottom sheet with appropriate settings
            final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
            bottomSheetDialog.setContentView(R.layout.sheet_notification_settings);
            Chip obfuscationMin = bottomSheetDialog.findViewById(R.id.chp_obfuscate_transparent);
            Chip obfuscationMed = bottomSheetDialog.findViewById(R.id.chp_obfuscate_neutral);
            Chip obfuscationMax = bottomSheetDialog.findViewById(R.id.chp_obfuscate_max);
            Chip[] obfuscationChips = new Chip[] { obfuscationMin, obfuscationMed, obfuscationMax };
            for (Chip obfuscationChip : obfuscationChips) {
                obfuscationChip.setOnClickListener(e -> {
                    if(!obfuscationChip.isChecked()) {
                        obfuscationChip.setChecked(true);
                        return;
                    }
                    for (Chip c : obfuscationChips) {
                        if(c != obfuscationChip){
                            c.setChecked(false);
                        }
                    }
                });
            }
            obfuscationMin.setChecked(true);
            bottomSheetDialog.show();
        });
        categories.add(ncDreamJournal);

        NotificationCategory ncRealityCheck = new NotificationCategory();
        ncRealityCheck.setActive(true);
        ncRealityCheck.setCategoryHeading("Reality check reminder");
        ncRealityCheck.setCategoryDescription("Reminder for performing a reality check to train performing reality checks in your dreams");
        ncRealityCheck.setNotificationCount(3);
        ncRealityCheck.setCategoryDrawable(R.drawable.round_model_training_24);
        ncRealityCheck.setCategoryClickedListener(() -> {
            // TODO: open up bottom sheet with appropriate settings
        });
        categories.add(ncRealityCheck);

        NotificationCategory ncGoals = new NotificationCategory();
        ncGoals.setActive(true);
        ncGoals.setCategoryHeading("Daily goals reminder");
        ncGoals.setCategoryDescription("Reminder for taking a look at your daily goals and to look out for them throughout the day");
        ncGoals.setNotificationCount(9);
        ncGoals.setCategoryDrawable(R.drawable.ic_baseline_bookmark_added_24);
        ncGoals.setCategoryClickedListener(() -> {
            // TODO: open up bottom sheet with appropriate settings
        });
        categories.add(ncGoals);

        NotificationCategory ncCustom = new NotificationCategory();
        ncCustom.setActive(true);
        ncCustom.setCategoryHeading("Custom reminder");
        ncCustom.setCategoryDescription("Reminder for everything you want to be reminded about. You can set your own messages.");
        ncCustom.setNotificationCount(2);
        ncCustom.setCategoryDrawable(R.drawable.round_lightbulb_24);
        ncCustom.setCategoryClickedListener(() -> {
            // TODO: open up bottom sheet with appropriate settings
        });
        categories.add(ncCustom);

        NotificationCategory ncPermanentNotification = new NotificationCategory();
        ncPermanentNotification.setActive(true);
        ncPermanentNotification.setCategoryHeading("Permanent notification");
        ncPermanentNotification.setCategoryDescription("Permanent notification for LucidSourceKit with some general information at a glance.");
        ncPermanentNotification.setNotificationCount(1);
        ncPermanentNotification.setCategoryDrawable(R.drawable.ic_outline_info_24);
        ncPermanentNotification.setCategoryClickedListener(() -> {
            // TODO: open up bottom sheet with appropriate settings
        });
        categories.add(ncPermanentNotification);

        RecyclerViewAdapterNotificationCategories rcvaNotificationCategories = new RecyclerViewAdapterNotificationCategories(this, categories);
        RecyclerView rcvNotificationCategories = findViewById(R.id.rcv_notification_categories);
        rcvNotificationCategories.setAdapter(rcvaNotificationCategories);
        rcvNotificationCategories.setLayoutManager(new LinearLayoutManager(this));
    }
}