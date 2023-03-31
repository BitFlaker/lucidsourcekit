package com.bitflaker.lucidsourcekit.notification;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class NotificationManager extends AppCompatActivity {

    private Calendar notificationsTimeFrom, notificationsTimeTo;
    private int customDailyNotificationsCount;

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
            createAndShowBottomSheetConfigurator(ncDreamJournal);
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
            createAndShowBottomSheetConfigurator(ncRealityCheck);
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
            createAndShowBottomSheetConfigurator(ncGoals);
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
            createAndShowBottomSheetConfigurator(ncCustom);
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
            createAndShowBottomSheetConfigurator(ncPermanentNotification);
        });
        categories.add(ncPermanentNotification);

        RecyclerViewAdapterNotificationCategories rcvaNotificationCategories = new RecyclerViewAdapterNotificationCategories(this, categories);
        RecyclerView rcvNotificationCategories = findViewById(R.id.rcv_notification_categories);
        rcvNotificationCategories.setAdapter(rcvaNotificationCategories);
        rcvNotificationCategories.setLayoutManager(new LinearLayoutManager(this));
    }

    private void createAndShowBottomSheetConfigurator(NotificationCategory category) {
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(R.layout.sheet_notification_settings);

        ImageView notificationSettingsIcon = bottomSheetDialog.findViewById(R.id.img_notification_settings_icon);
        TextView notificationSettingsHeading = bottomSheetDialog.findViewById(R.id.txt_notification_settings_heading);
        TextView notificationSettingsDescription = bottomSheetDialog.findViewById(R.id.txt_notification_settings_description);
        Chip obfuscationMin = bottomSheetDialog.findViewById(R.id.chp_obfuscate_transparent);
        Chip obfuscationMed = bottomSheetDialog.findViewById(R.id.chp_obfuscate_neutral);
        Chip obfuscationMax = bottomSheetDialog.findViewById(R.id.chp_obfuscate_max);
        Chip customNotificationCount = bottomSheetDialog.findViewById(R.id.chp_notification_custom);
        ChipGroup dailyNotificationChipGroup = bottomSheetDialog.findViewById(R.id.ll_daily_notification_count);
        SwitchMaterial notificationEnabledSwitch = bottomSheetDialog.findViewById(R.id.chk_enable_notifications);
        MaterialCardView cardNotificationTimeFrom = bottomSheetDialog.findViewById(R.id.crd_notification_time_from);
        MaterialCardView cardNotificationTimeTo = bottomSheetDialog.findViewById(R.id.crd_notification_time_to);
        TextView labelNotificationTimeFrom = bottomSheetDialog.findViewById(R.id.txt_notification_time_from);
        TextView labelNotificationTimeTo = bottomSheetDialog.findViewById(R.id.txt_notification_time_to);
        MaterialButton cancelButton = bottomSheetDialog.findViewById(R.id.btn_cancel);
        MaterialButton saveButton = bottomSheetDialog.findViewById(R.id.btn_save);
        ImageButton editNotificationMessages = bottomSheetDialog.findViewById(R.id.btn_edit_notification_messages);

        DateFormat tf = DateFormat.getTimeInstance(DateFormat.SHORT);

        // Setting default values
        customDailyNotificationsCount = 15;

        notificationsTimeFrom = Calendar.getInstance();
        notificationsTimeFrom.set(Calendar.HOUR_OF_DAY, 8);
        notificationsTimeFrom.set(Calendar.MINUTE, 0);
        notificationsTimeFrom.set(Calendar.SECOND, 0);
        notificationsTimeFrom.set(Calendar.MILLISECOND, 0);

        notificationsTimeTo = Calendar.getInstance();
        notificationsTimeTo.set(Calendar.HOUR_OF_DAY, 18);
        notificationsTimeTo.set(Calendar.MINUTE, 0);
        notificationsTimeTo.set(Calendar.SECOND, 0);
        notificationsTimeTo.set(Calendar.MILLISECOND, 0);

        labelNotificationTimeFrom.setText(tf.format(notificationsTimeFrom.getTime()));
        labelNotificationTimeTo.setText(tf.format(notificationsTimeTo.getTime()));

        notificationSettingsIcon.setImageDrawable(ResourcesCompat.getDrawable(getResources(), category.getCategoryDrawable(), getTheme()));
        notificationSettingsHeading.setText(category.getCategoryHeading());
        notificationSettingsDescription.setText(category.getCategoryDescription());

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
        notificationEnabledSwitch.setOnCheckedChangeListener((compoundButton, checked) -> {
            dailyNotificationChipGroup.setVisibility(checked ? View.VISIBLE : View.GONE);
        });
        cardNotificationTimeFrom.setOnClickListener(e -> {
            new TimePickerDialog(this, (timePickerFrom, hourFrom, minuteFrom) -> {
                notificationsTimeFrom.set(Calendar.HOUR_OF_DAY, hourFrom);
                notificationsTimeFrom.set(Calendar.MINUTE, minuteFrom);
                labelNotificationTimeFrom.setText(tf.format(notificationsTimeFrom.getTime()));
            }, notificationsTimeFrom.get(Calendar.HOUR_OF_DAY), notificationsTimeFrom.get(Calendar.MINUTE), true).show();
        });
        cardNotificationTimeTo.setOnClickListener(e -> {
            new TimePickerDialog(this, (timePickerFrom, hourFrom, minuteFrom) -> {
                notificationsTimeTo.set(Calendar.HOUR_OF_DAY, hourFrom);
                notificationsTimeTo.set(Calendar.MINUTE, minuteFrom);
                labelNotificationTimeTo.setText(tf.format(notificationsTimeTo.getTime()));
            }, notificationsTimeTo.get(Calendar.HOUR_OF_DAY), notificationsTimeTo.get(Calendar.MINUTE), true).show();
        });
        customNotificationCount.setOnClickListener(e -> {
            final NumberPicker numberPicker = new NumberPicker(this);
            numberPicker.setMaxValue(99);
            numberPicker.setMinValue(1);
            numberPicker.setValue(customDailyNotificationsCount);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(numberPicker);
            builder.setTitle("Daily notifications count");
            builder.setMessage("Choose an amount");
            builder.setPositiveButton(getResources().getString(R.string.ok), (dialog, which) -> {
                customDailyNotificationsCount = numberPicker.getValue();
                customNotificationCount.setText("Custom (" + customDailyNotificationsCount + ")");
            });
            builder.setNegativeButton(getResources().getString(R.string.cancel), null);
            builder.create();
            builder.show();
        });
        editNotificationMessages.setOnClickListener(e -> {
            startActivity(new Intent(this, NotificationManagerEditor.class));
        });
        cancelButton.setOnClickListener(e -> bottomSheetDialog.cancel());
        saveButton.setOnClickListener(e -> {
            // TODO save changes
            bottomSheetDialog.dismiss();
        });
        bottomSheetDialog.show();
    }
}