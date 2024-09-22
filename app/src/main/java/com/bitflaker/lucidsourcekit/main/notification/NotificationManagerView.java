package com.bitflaker.lucidsourcekit.main.notification;

import android.Manifest;
import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.PopupMenu;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.data.datastore.DataStoreKeys;
import com.bitflaker.lucidsourcekit.data.datastore.DataStoreManager;
import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.notifications.entities.NotificationCategory;
import com.bitflaker.lucidsourcekit.databinding.ActivityNotificationManagerBinding;
import com.bitflaker.lucidsourcekit.databinding.SheetNotificationSettingsBinding;
import com.bitflaker.lucidsourcekit.main.alarms.AlarmHandler;
import com.bitflaker.lucidsourcekit.utils.Tools;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class NotificationManagerView extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 772;
    private int customDailyNotificationsCount;
    private int currentDeliveryProgress;
    private Calendar notificationsTimeFrom, notificationsTimeTo;
    private RecyclerViewAdapterNotificationCategories rcvaNotificationCategories;
    private MainDatabase db;
    private ActivityNotificationManagerBinding binding;
    private TextView compliantNotificationCountSettings, totalNotificationCountSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityNotificationManagerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Tools.makeStatusBarTransparent(NotificationManagerView.this);
        ConstraintLayout.LayoutParams lParamsHeading = Tools.getConstraintLayoutParamsTopStatusbar((ConstraintLayout.LayoutParams) binding.txtManageNotificationHeading.getLayoutParams(), NotificationManagerView.this);
        binding.txtManageNotificationHeading.setLayoutParams(lParamsHeading);

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
        binding.rcvNotificationCategories.setAdapter(rcvaNotificationCategories);
        binding.rcvNotificationCategories.setLayoutManager(new LinearLayoutManager(this));
        rcvaNotificationCategories.setNotificationCategoryChangedListener(this::updateNotificationStats);

        if(getIntent().hasExtra("AUTO_OPEN_ID")){
            String autoOpenId = getIntent().getStringExtra("AUTO_OPEN_ID");
            rcvaNotificationCategories.openSettingsForCategoryId(autoOpenId);
        }

        currentDeliveryProgress = (int) getNotificationDeliveryProgress();
        binding.spdoNotificationsDelivered.setData(25f, currentDeliveryProgress, 100);
        binding.spdoNotificationsDelivered.setPercentageData(true);
        binding.spdoNotificationsDelivered.setDecimalPlaces(0);
        binding.spdoNotificationsDelivered.setDescription("notifications\nalready delivered today");

        Calendar curr = Calendar.getInstance();
        long delay = (60 - curr.get(Calendar.SECOND) - 1) * 1000 + 1000 - curr.get(Calendar.MILLISECOND);
        new Handler().postDelayed(deliveryStatusUpdated, delay);

        DataStoreManager dsManager = DataStoreManager.getInstance();
        binding.txtNotificationsDisabledInfo.setVisibility(dsManager.getSetting(DataStoreKeys.NOTIFICATION_PAUSED_ALL).blockingFirst() ? View.VISIBLE : View.GONE);
        binding.btnMoreNotificationOptions.setOnClickListener(e -> {
            PopupMenu popup = new PopupMenu(new ContextThemeWrapper(this, R.style.Theme_LucidSourceKit_PopupMenu), binding.btnMoreNotificationOptions);
            popup.getMenuInflater().inflate(R.menu.more_notification_options, popup.getMenu());
            popup.getMenu().findItem(R.id.itm_pause_notifications).setTitle(dsManager.getSetting(DataStoreKeys.NOTIFICATION_PAUSED_ALL).blockingFirst() ? "Resume notifications" : "Pause notifications");
            popup.setOnMenuItemClickListener(item -> {
                if(item.getItemId() == R.id.itm_pause_notifications) {
                    boolean allNotificationsPaused = dsManager.getSetting(DataStoreKeys.NOTIFICATION_PAUSED_ALL).blockingFirst();
                    new MaterialAlertDialogBuilder(this, R.style.Theme_LucidSourceKit_ThemedDialog)
                            .setTitle(allNotificationsPaused ? "Resume notifications" : "Pause notifications")
                            .setMessage(allNotificationsPaused ? "Do you really want to resume all notifications?" : "Do you really want to pause all notifications for the time being? You can re-enable all notifications any time later.")
                            .setPositiveButton(getResources().getString(R.string.yes), (dialog, which) -> {
                                dsManager.updateSetting(DataStoreKeys.NOTIFICATION_PAUSED_ALL, !allNotificationsPaused).blockingSubscribe();
                                popup.getMenu().findItem(R.id.itm_pause_notifications).setTitle(!allNotificationsPaused ? "Resume notifications" : "Pause notifications");
                                binding.txtNotificationsDisabledInfo.setVisibility(!allNotificationsPaused ? View.VISIBLE : View.GONE);
                            })
                            .setNegativeButton(getResources().getString(R.string.no), null)
                            .show();
                }
                else if (item.getItemId() == R.id.itm_disable_notifications) {
                    new MaterialAlertDialogBuilder(this, R.style.Theme_LucidSourceKit_ThemedDialog)
                            .setTitle("Disable notifications")
                            .setMessage("Do you really want to disable all notifications?")
                            .setPositiveButton(getResources().getString(R.string.yes), (dialog, which) -> {
                                for (NotificationCategory category : categories) {
                                    category.setEnabled(false);
                                    db.getNotificationCategoryDao().update(category).blockingAwait();
                                    rcvaNotificationCategories.notifyCategoryChanged(category);
                                }
                            })
                            .setNegativeButton(getResources().getString(R.string.no), null)
                            .show();
                }
                return true;
            });
            popup.show();
        });

        requestPermissionIfRequired();
        updateNotificationStats();
    }

    private final Runnable deliveryStatusUpdated = new Runnable() {
        @Override
        public void run() {
            calculateAndApplyNewStatus();
            new Handler().postDelayed(deliveryStatusUpdated, 60 * 1000);
        }
    };

    private void calculateAndApplyNewStatus() {
        int newDeliveryProgress = (int) getNotificationDeliveryProgress();
        if(currentDeliveryProgress != newDeliveryProgress) {
            currentDeliveryProgress = newDeliveryProgress;
            binding.spdoNotificationsDelivered.updateValue(currentDeliveryProgress);
        }
    }

    private double getNotificationDeliveryProgress() {
        long notificationTimeframeFrom = rcvaNotificationCategories.getNotificationTimeframeFrom();
        long notificationTimeframeTo = rcvaNotificationCategories.getNotificationTimeframeTo();
        long currentTimeInMillis = Tools.getTimeOfDayMillis(Calendar.getInstance());

        if(currentTimeInMillis <= notificationTimeframeFrom) {
            return 0;
        }
        else if (currentTimeInMillis >= notificationTimeframeTo) {
            return 100;
        }

        long redTo = notificationTimeframeTo - notificationTimeframeFrom;
        long redCurrent = currentTimeInMillis - notificationTimeframeFrom;

        return 100.0 * redCurrent / redTo;
    }

    private void updateNotificationStats() {
        int totalDailyNotificationCount = rcvaNotificationCategories.getDailyNotificationCount();
        long enabledCategoriesCount = rcvaNotificationCategories.getEnabledCategoriesCount();
        int totalCategoriesCount = rcvaNotificationCategories.getItemCount();
        long notificationTimeframeFrom = rcvaNotificationCategories.getNotificationTimeframeFrom();
        long notificationTimeframeTo = rcvaNotificationCategories.getNotificationTimeframeTo();
        int obfuscationPercentage = rcvaNotificationCategories.getObfuscationPercentage();

        binding.txtDailyNotificationsVal.setText(String.format(Locale.ENGLISH, "%d", totalDailyNotificationCount));
        binding.txtCategoriesEnabledVal.setText(String.format(Locale.ENGLISH, "%d/%d", enabledCategoriesCount, totalCategoriesCount));
        long nTimeframeFromHours = TimeUnit.MILLISECONDS.toHours(notificationTimeframeFrom);
        long nTimeframeFromMinutes = TimeUnit.MILLISECONDS.toMinutes(notificationTimeframeFrom) - TimeUnit.HOURS.toMinutes(nTimeframeFromHours);
        long nTimeframeToHours = TimeUnit.MILLISECONDS.toHours(notificationTimeframeTo);
        long nTimeframeToMinutes = TimeUnit.MILLISECONDS.toMinutes(notificationTimeframeTo) - TimeUnit.HOURS.toMinutes(nTimeframeToHours);
        binding.txtNotificationTimespanVal.setText(String.format(Locale.ENGLISH, "%02d:%02d - %02d:%02d", nTimeframeFromHours, nTimeframeFromMinutes, nTimeframeToHours, nTimeframeToMinutes));
        binding.txtObfuscationLevelVal.setText(String.format(Locale.ENGLISH, "%d%%", obfuscationPercentage));
        calculateAndApplyNewStatus();
    }

    private void requestPermissionIfRequired() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.POST_NOTIFICATIONS }, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                finish();
            }
        }
    }

    ActivityResultLauncher<Intent> notificationMessageEditorLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    String categoryId = data.getStringExtra("CATEGORY_ID");
                    int obfuscationTypeId = data.getIntExtra("OBFUSCATION_TYPE_ID", 0);
                    compliantNotificationCountSettings.setText(String.format(Locale.ENGLISH, "%d", db.getNotificationMessageDao().getCountOfMessagesForCategoryAndObfuscationType(categoryId, obfuscationTypeId).blockingGet()));
                    totalNotificationCountSettings.setText("/ " + db.getNotificationMessageDao().getCountOfMessagesForCategory(categoryId).blockingGet());
                }
            });

    private void createAndShowBottomSheetConfigurator(NotificationCategory category) {
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        SheetNotificationSettingsBinding sBinding = SheetNotificationSettingsBinding.inflate(getLayoutInflater());
        bottomSheetDialog.setContentView(sBinding.getRoot());

        Chip[] presetNotificationCounts = new Chip[] {
                sBinding.chpNotification1,
                sBinding.chpNotification2,
                sBinding.chpNotification3,
                sBinding.chpNotification5
        };

        DateFormat tf = DateFormat.getTimeInstance(DateFormat.SHORT);

        // Setting saved values
        notificationsTimeFrom = Calendar.getInstance();
        notificationsTimeFrom.setTimeInMillis(Tools.getTimeFromMidnight(category.getTimeFrom()));
        notificationsTimeTo = Calendar.getInstance();
        notificationsTimeTo.setTimeInMillis(Tools.getTimeFromMidnight(category.getTimeTo()));
        customDailyNotificationsCount = category.getDailyNotificationCount();

        if (!category.isEnabled()) {
            sBinding.chkEnableNotifications.setChecked(false);
            sBinding.llDailyNotificationCount.setVisibility(View.GONE);
        }

        sBinding.txtNotificationTimeFrom.setText(tf.format(notificationsTimeFrom.getTime()));
        sBinding.txtNotificationTimeTo.setText(tf.format(notificationsTimeTo.getTime()));

        sBinding.txtCompliantNotificationMessageCount.setText(String.format(Locale.ENGLISH, "%d", db.getNotificationMessageDao().getCountOfMessagesForCategoryAndObfuscationType(category.getId(), category.getObfuscationTypeId()).blockingGet()));
        sBinding.txtTotalNotificationMessageCount.setText("/ " + db.getNotificationMessageDao().getCountOfMessagesForCategory(category.getId()).blockingGet());

        sBinding.imgNotificationSettingsIcon.setImageDrawable(ResourcesCompat.getDrawable(getResources(), category.getDrawable(), getTheme()));
        sBinding.txtNotificationSettingsHeading.setText(category.getItemHeading());
        sBinding.txtNotificationSettingsDescription.setText(category.getItemDescription());

        Chip[] obfuscationChips = new Chip[] { sBinding.chpObfuscateTransparent, sBinding.chpObfuscateNeutral, sBinding.chpObfuscateMax };
        for (Chip obfuscationChip : obfuscationChips) {
            obfuscationChip.setOnClickListener(e -> {
                category.setObfuscationTypeId(obfuscationChip == sBinding.chpObfuscateTransparent ? 0 : (obfuscationChip == sBinding.chpObfuscateNeutral ? 1 : 2));
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
            sBinding.chkEnableNotifications.setOnCheckedChangeListener((compoundButton, checked) -> {
            category.setEnabled(checked);
            sBinding.llDailyNotificationCount.setVisibility(checked ? View.VISIBLE : View.GONE);
        });
        sBinding.crdNotificationTimeFrom.setOnClickListener(e -> {
            new TimePickerDialog(this, (timePickerFrom, hourFrom, minuteFrom) -> {
                notificationsTimeFrom.set(Calendar.HOUR_OF_DAY, hourFrom);
                notificationsTimeFrom.set(Calendar.MINUTE, minuteFrom);
                sBinding.txtNotificationTimeFrom.setText(tf.format(notificationsTimeFrom.getTime()));
                category.setTimeFrom(Tools.getTimeOfDayMillis(notificationsTimeFrom));
            }, notificationsTimeFrom.get(Calendar.HOUR_OF_DAY), notificationsTimeFrom.get(Calendar.MINUTE), true).show();
        });
        sBinding.crdNotificationTimeTo.setOnClickListener(e -> {
            new TimePickerDialog(this, (timePickerFrom, hourFrom, minuteFrom) -> {
                notificationsTimeTo.set(Calendar.HOUR_OF_DAY, hourFrom);
                notificationsTimeTo.set(Calendar.MINUTE, minuteFrom);
                sBinding.txtNotificationTimeTo.setText(tf.format(notificationsTimeTo.getTime()));
                category.setTimeTo(Tools.getTimeOfDayMillis(notificationsTimeTo));
            }, notificationsTimeTo.get(Calendar.HOUR_OF_DAY), notificationsTimeTo.get(Calendar.MINUTE), true).show();
        });
        sBinding.chpNotificationCustom.setOnClickListener(e -> {
            final NumberPicker numberPicker = new NumberPicker(this);
            numberPicker.setMaxValue(99);
            numberPicker.setMinValue(1);
            numberPicker.setValue(customDailyNotificationsCount);

            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
            builder.setView(numberPicker);
            builder.setTitle("Daily notifications count");
            builder.setMessage("Choose an amount");
            builder.setPositiveButton(getResources().getString(R.string.ok), (dialog, which) -> {
                customDailyNotificationsCount = numberPicker.getValue();
                category.setDailyNotificationCount(customDailyNotificationsCount);
                sBinding.chpNotificationCustom.setText("Custom (" + customDailyNotificationsCount + ")");
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
            sBinding.chpNotificationCustom.setChecked(true);
            sBinding.chpNotificationCustom.setText("Custom (" + customDailyNotificationsCount + ")");
        }

        sBinding.btnEditNotificationMessages.setOnClickListener(e -> {
            Intent intent = new Intent(this, NotificationManagerEditorView.class);
            intent.putExtra("CATEGORY_ID", category.getId());
            intent.putExtra("OBFUSCATION_TYPE_ID", category.getObfuscationTypeId());
            compliantNotificationCountSettings = sBinding.txtCompliantNotificationMessageCount;
            totalNotificationCountSettings = sBinding.txtTotalNotificationMessageCount;
            notificationMessageEditorLauncher.launch(intent);
        });
        sBinding.btnCancel.setOnClickListener(e -> bottomSheetDialog.cancel());
        sBinding.btnSave.setOnClickListener(e -> {
            // TODO save changes
            int compliantMessageCount = db.getNotificationMessageDao().getCountOfMessagesForCategoryAndObfuscationType(category.getId(), category.getObfuscationTypeId()).blockingGet();
            if(compliantMessageCount == 0 && category.isEnabled()) {
                Toast.makeText(this, "No messages comply with current settings, disabling category", Toast.LENGTH_LONG).show();
                category.setEnabled(false);
            }
            db.getNotificationCategoryDao().update(category).blockingAwait();
            bottomSheetDialog.dismiss();
            rcvaNotificationCategories.notifyCategoryChanged(category);
            AlarmHandler.scheduleNextNotification(this).blockingSubscribe();
        });
        bottomSheetDialog.show();
    }
}