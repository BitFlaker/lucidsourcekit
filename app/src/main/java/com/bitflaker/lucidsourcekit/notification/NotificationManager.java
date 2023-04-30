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
import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.notifications.entities.NotificationCategory;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;

public class NotificationManager extends AppCompatActivity {
    private Calendar notificationsTimeFrom, notificationsTimeTo;
    private int customDailyNotificationsCount;
    private MainDatabase db;
    private RecyclerViewAdapterNotificationCategories rcvaNotificationCategories;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Tools.getTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_manager);
        Tools.makeStatusBarTransparent(NotificationManager.this);
        ConstraintLayout.LayoutParams lParamsHeading = Tools.getConstraintLayoutParamsTopStatusbar((ConstraintLayout.LayoutParams) findViewById(R.id.txt_manage_notification_heading).getLayoutParams(), NotificationManager.this);
        findViewById(R.id.txt_manage_notification_heading).setLayoutParams(lParamsHeading);

        db = MainDatabase.getInstance(this);
        List<NotificationCategory> categories = db.getNotificationCategoryDao().getAll().blockingGet();

        for (NotificationCategory category : categories) {
            String heading;
            String description;
            int drawable;

            switch (category.getId()){
                case "DJR":
                    heading = "Dream journal reminder";
                    description = "Reminder for writing down your dream to the dream journal in order to improve dream recall";
                    drawable = R.drawable.ic_baseline_text_fields_24;
                    break;
                case "RCR":
                    heading = "Reality check reminder";
                    description = "Reminder for performing a reality check to train performing reality checks in your dreams";
                    drawable = R.drawable.round_model_training_24;
                    break;
                case "DGR":
                    heading = "Daily goals reminder";
                    description = "Reminder for taking a look at your daily goals and to look out for them throughout the day";
                    drawable = R.drawable.ic_baseline_bookmark_added_24;
                    break;
                case "CR":
                    heading = "Custom reminder";
                    description = "Reminder for everything you want to be reminded about. You can set your own messages";
                    drawable = R.drawable.round_lightbulb_24;
                    break;
                case "PN":
                    heading = "Permanent notification";
                    description = "Permanent notification for LucidSourceKit with some general information at a glance";
                    drawable = R.drawable.ic_outline_info_24;
                    break;
                default:
                    heading = "";
                    description = "";
                    drawable = -1;
                    break;
            }

            category.setItemHeading(heading);
            category.setItemDescription(description);
            category.setDrawable(drawable);
            category.setCategoryClickedListener(() -> createAndShowBottomSheetConfigurator(category));
        }

        rcvaNotificationCategories = new RecyclerViewAdapterNotificationCategories(this, categories);
        RecyclerView rcvNotificationCategories = findViewById(R.id.rcv_notification_categories);
        rcvNotificationCategories.setAdapter(rcvaNotificationCategories);
        rcvNotificationCategories.setLayoutManager(new LinearLayoutManager(this));

        if(getIntent().hasExtra("AUTO_OPEN_ID")){
            String autoOpenId = getIntent().getStringExtra("AUTO_OPEN_ID");
            rcvaNotificationCategories.openSettingsForCategoryId(autoOpenId);
        }
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
        TextView labelCompliantNotificationCount = bottomSheetDialog.findViewById(R.id.txt_compliant_notification_message_count);
        TextView labelTotalNotificationCount = bottomSheetDialog.findViewById(R.id.txt_total_notification_message_count);
        MaterialButton cancelButton = bottomSheetDialog.findViewById(R.id.btn_cancel);
        MaterialButton saveButton = bottomSheetDialog.findViewById(R.id.btn_save);
        ImageButton editNotificationMessages = bottomSheetDialog.findViewById(R.id.btn_edit_notification_messages);
        Chip[] presetNotificationCounts = new Chip[] {
                bottomSheetDialog.findViewById(R.id.chp_notification_1),
                bottomSheetDialog.findViewById(R.id.chp_notification_2),
                bottomSheetDialog.findViewById(R.id.chp_notification_3),
                bottomSheetDialog.findViewById(R.id.chp_notification_5),
                bottomSheetDialog.findViewById(R.id.chp_notification_10)
        };

        DateFormat tf = DateFormat.getTimeInstance(DateFormat.SHORT);

        // Setting saved values
        notificationsTimeFrom = Calendar.getInstance();
        notificationsTimeFrom.setTimeInMillis(Tools.getTimeFromMidnight(category.getTimeFrom()));
        notificationsTimeTo = Calendar.getInstance();
        notificationsTimeTo.setTimeInMillis(Tools.getTimeFromMidnight(category.getTimeTo()));
        customDailyNotificationsCount = category.getDailyNotificationCount();

        if(!category.isEnabled()){
            notificationEnabledSwitch.setChecked(false);
            dailyNotificationChipGroup.setVisibility(View.GONE);
        }

        labelNotificationTimeFrom.setText(tf.format(notificationsTimeFrom.getTime()));
        labelNotificationTimeTo.setText(tf.format(notificationsTimeTo.getTime()));

        labelCompliantNotificationCount.setText(db.getNotificationMessageDao().getCountOfMessagesForCategoryAndObfuscationType(category.getId(), category.getObfuscationTypeId()).blockingGet().toString());
        labelTotalNotificationCount.setText("/ " + db.getNotificationMessageDao().getCountOfMessagesForCategory(category.getId()).blockingGet());

        notificationSettingsIcon.setImageDrawable(ResourcesCompat.getDrawable(getResources(), category.getDrawable(), getTheme()));
        notificationSettingsHeading.setText(category.getItemHeading());
        notificationSettingsDescription.setText(category.getItemDescription());

        Chip[] obfuscationChips = new Chip[] { obfuscationMin, obfuscationMed, obfuscationMax };
        for (Chip obfuscationChip : obfuscationChips) {
            obfuscationChip.setOnClickListener(e -> {
                category.setObfuscationTypeId(obfuscationChip == obfuscationMin ? 0 : (obfuscationChip == obfuscationMed ? 1 : 2));
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
        obfuscationChips[category.getObfuscationTypeId()].setChecked(true);
        notificationEnabledSwitch.setOnCheckedChangeListener((compoundButton, checked) -> {
            category.setEnabled(checked);
            dailyNotificationChipGroup.setVisibility(checked ? View.VISIBLE : View.GONE);
        });
        cardNotificationTimeFrom.setOnClickListener(e -> {
            new TimePickerDialog(this, (timePickerFrom, hourFrom, minuteFrom) -> {
                notificationsTimeFrom.set(Calendar.HOUR_OF_DAY, hourFrom);
                notificationsTimeFrom.set(Calendar.MINUTE, minuteFrom);
                labelNotificationTimeFrom.setText(tf.format(notificationsTimeFrom.getTime()));
                category.setTimeFrom(Tools.getTimeOfDayMillis(notificationsTimeFrom));
            }, notificationsTimeFrom.get(Calendar.HOUR_OF_DAY), notificationsTimeFrom.get(Calendar.MINUTE), true).show();
        });
        cardNotificationTimeTo.setOnClickListener(e -> {
            new TimePickerDialog(this, (timePickerFrom, hourFrom, minuteFrom) -> {
                notificationsTimeTo.set(Calendar.HOUR_OF_DAY, hourFrom);
                notificationsTimeTo.set(Calendar.MINUTE, minuteFrom);
                labelNotificationTimeTo.setText(tf.format(notificationsTimeTo.getTime()));
                category.setTimeTo(Tools.getTimeOfDayMillis(notificationsTimeTo));
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
                category.setDailyNotificationCount(customDailyNotificationsCount);
                customNotificationCount.setText("Custom (" + customDailyNotificationsCount + ")");
            });
            builder.setNegativeButton(getResources().getString(R.string.cancel), null);
            builder.create();
            builder.show();
        });
        boolean setPresetCount = false;
        for (Chip presetNotificationCount : presetNotificationCounts) {
            int chipInt = Integer.parseInt(presetNotificationCount.getText().toString());
            if(chipInt == category.getDailyNotificationCount()){
                presetNotificationCount.setChecked(true);
                setPresetCount = true;
            }
            presetNotificationCount.setOnClickListener(e -> {
                category.setDailyNotificationCount(chipInt);
            });
        }
        if(!setPresetCount){
            customNotificationCount.setChecked(true);
            customNotificationCount.setText("Custom (" + customDailyNotificationsCount + ")");
        }
        editNotificationMessages.setOnClickListener(e -> {
            Intent intent = new Intent(this, NotificationManagerEditor.class);
            intent.putExtra("notificationCategoryId", category.getId());
            startActivity(intent);
        });
        cancelButton.setOnClickListener(e -> bottomSheetDialog.cancel());
        saveButton.setOnClickListener(e -> {
            // TODO save changes
            db.getNotificationCategoryDao().update(category).blockingAwait();
            bottomSheetDialog.dismiss();
            rcvaNotificationCategories.notifyCategoryChanged(category);
        });
        bottomSheetDialog.show();
    }
}