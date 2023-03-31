package com.bitflaker.lucidsourcekit.notification;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;

public class NotificationManagerEditor extends AppCompatActivity {

    private int customNotificationWeightValue;
    private RecyclerViewAdapterNotificationEditor rcvaNotificationEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_manager_editor);
        Tools.makeStatusBarTransparent(NotificationManagerEditor.this);
        ConstraintLayout.LayoutParams lParamsHeading = Tools.getConstraintLayoutParamsTopStatusbar((ConstraintLayout.LayoutParams) findViewById(R.id.txt_notification_editor_heading).getLayoutParams(), NotificationManagerEditor.this);
        findViewById(R.id.txt_notification_editor_heading).setLayoutParams(lParamsHeading);

        List<NotificationMessage> notificationMessages = new ArrayList<>();
        notificationMessages.add(new NotificationMessage(1, "This is a very long example notification message which should not fit within one line or even two lines of this TextView \uD83D\uDE2F", 0, 9));
        notificationMessages.add(new NotificationMessage(2, "Some random test message", 0, 2));
        notificationMessages.add(new NotificationMessage(3, "Some random test message", 1, 1));
        notificationMessages.add(new NotificationMessage(4, "Some random test message", 1, 2));
        notificationMessages.add(new NotificationMessage(5, "Some random test message", 1, 2));
        notificationMessages.add(new NotificationMessage(6, "Some random test message", 1, 6));
        notificationMessages.add(new NotificationMessage(7, "Some random test message", 2, 1));
        notificationMessages.add(new NotificationMessage(8, "Some random test message", 2, 3));
        notificationMessages.add(new NotificationMessage(9, "Some random test message", 2, 3));
        notificationMessages.add(new NotificationMessage(10, "Some random test message", 2, 15));

        RecyclerView rcv = findViewById(R.id.rcv_notification_messages);
        FloatingActionButton addNotificationMessageButton = findViewById(R.id.btn_add_notification_message);

        rcvaNotificationEditor = new RecyclerViewAdapterNotificationEditor(this, notificationMessages);
        rcvaNotificationEditor.setOnMessageClickedListener(this::createAndShowBottomSheetConfigurator);
        rcv.setAdapter(rcvaNotificationEditor);
        rcv.setLayoutManager(new LinearLayoutManager(this));

        addNotificationMessageButton.setOnClickListener(e -> createAndShowBottomSheetConfigurator(null));
    }

    private void createAndShowBottomSheetConfigurator(NotificationMessage message) {
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(R.layout.sheet_notification_message);

        EditText editNotificationMessage = bottomSheetDialog.findViewById(R.id.txt_custom_notification_message);
        Chip obfuscationMin = bottomSheetDialog.findViewById(R.id.chp_obfuscate_transparent);
        Chip obfuscationMed = bottomSheetDialog.findViewById(R.id.chp_obfuscate_neutral);
        Chip obfuscationMax = bottomSheetDialog.findViewById(R.id.chp_obfuscate_max);
        MaterialButton cancelButton = bottomSheetDialog.findViewById(R.id.btn_cancel);
        MaterialButton saveButton = bottomSheetDialog.findViewById(R.id.btn_save);

        Chip customNotificationWeight = bottomSheetDialog.findViewById(R.id.chp_notification_weight_custom);
        Chip customNotificationWeight2 = bottomSheetDialog.findViewById(R.id.chp_notification_weight_1);
        Chip customNotificationWeight3 = bottomSheetDialog.findViewById(R.id.chp_notification_weight_2);
        Chip customNotificationWeight4 = bottomSheetDialog.findViewById(R.id.chp_notification_weight_3);
        Chip customNotificationWeight5 = bottomSheetDialog.findViewById(R.id.chp_notification_weight_4);
        Chip customNotificationWeight6 = bottomSheetDialog.findViewById(R.id.chp_notification_weight_5);
        Chip[] customNotificationWeightChips = new Chip[] {
                customNotificationWeight2,
                customNotificationWeight3,
                customNotificationWeight4,
                customNotificationWeight5,
                customNotificationWeight6
        };
        ChipGroup notificationWeightChipGroup = bottomSheetDialog.findViewById(R.id.chp_grp_notification_weight);
        SwitchMaterial customNotificationWeightSwitch = bottomSheetDialog.findViewById(R.id.chk_custom_notification_weight);

        // Setting default values
        customNotificationWeightValue = 6;

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
        customNotificationWeightSwitch.setOnCheckedChangeListener((compoundButton, checked) -> {
            notificationWeightChipGroup.setVisibility(checked ? View.VISIBLE : View.GONE);
        });
        customNotificationWeight.setOnClickListener(e -> {
            final NumberPicker numberPicker = new NumberPicker(this);
            numberPicker.setMaxValue(99);
            numberPicker.setMinValue(1);
            numberPicker.setValue(customNotificationWeightValue);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(numberPicker);
            builder.setTitle("Daily notifications count");
            builder.setMessage("Choose an amount");
            builder.setPositiveButton(getResources().getString(R.string.ok), (dialog, which) -> {
                customNotificationWeightValue = numberPicker.getValue();
                customNotificationWeight.setText("Custom (" + customNotificationWeightValue + ")");
            });
            builder.setNegativeButton(getResources().getString(R.string.cancel), null);
            builder.create();
            builder.show();
        });
        cancelButton.setOnClickListener(e -> bottomSheetDialog.cancel());
        saveButton.setOnClickListener(e -> {
            if(message != null){
                int origObfuscationTypeId = message.getObfuscationTypeId();
                message.setMessage(editNotificationMessage.getText().toString());
                int weight = customNotificationWeight.isChecked() ? customNotificationWeightValue : getSelectedWeight(customNotificationWeightChips);
                if(weight != -1){ message.setWeight(weight); }
                if(obfuscationMin.isChecked()) { message.setObfuscationTypeId(0); }
                else if(obfuscationMed.isChecked()){ message.setObfuscationTypeId(1); }
                else if(obfuscationMax.isChecked()){ message.setObfuscationTypeId(2); }
                rcvaNotificationEditor.notifyMessageChanged(message, origObfuscationTypeId != message.getObfuscationTypeId());
            }
            else {
                NotificationMessage newMessage = new NotificationMessage(
                        999,
                        editNotificationMessage.getText().toString(),
                        obfuscationMin.isChecked() ? 0 : (obfuscationMed.isChecked() ? 1 : 2),
                        customNotificationWeight.isChecked() ? customNotificationWeightValue : getSelectedWeight(customNotificationWeightChips)
                );
                rcvaNotificationEditor.notifyMessageAdded(newMessage);
            }

            // TODO save changes / create new notification message
            bottomSheetDialog.dismiss();
        });

        if(message != null) {
            editNotificationMessage.setText(message.getMessage());
            obfuscationMin.setChecked(false);
            obfuscationMed.setChecked(false);
            obfuscationMax.setChecked(false);
            switch (message.getObfuscationTypeId()){
                case 0:
                    obfuscationMin.setChecked(true);
                    break;
                case 1:
                    obfuscationMed.setChecked(true);
                    break;
                case 2:
                    obfuscationMax.setChecked(true);
                    break;
            }
            switch(message.getWeight()) {
                case 1:
                    customNotificationWeightSwitch.setChecked(false);
                    break;
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                    customNotificationWeightSwitch.setChecked(true);
                    customNotificationWeightChips[message.getWeight() - 2].setChecked(true);
                    break;
                default:
                    customNotificationWeightSwitch.setChecked(true);
                    customNotificationWeight.setChecked(true);
                    customNotificationWeightValue = message.getWeight();
                    customNotificationWeight.setText("Custom (" + customNotificationWeightValue + ")");
            }
        }
        else {
            editNotificationMessage.requestFocus();
        }
        bottomSheetDialog.show();
    }

    private int getSelectedWeight(Chip[] customNotificationWeightChips) {
        for (Chip customNotificationWeightChip : customNotificationWeightChips) {
            if (customNotificationWeightChip.isChecked()) {
                return Integer.parseInt(customNotificationWeightChip.getText().toString());
            }
        }
        return -1;
    }
}