package com.bitflaker.lucidsourcekit.main.notification;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.NumberPicker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.notifications.entities.NotificationMessage;
import com.bitflaker.lucidsourcekit.databinding.ActivityNotificationMessageEditorBinding;
import com.bitflaker.lucidsourcekit.databinding.SheetNotificationMessageBinding;
import com.bitflaker.lucidsourcekit.utils.Tools;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class NotificationManagerEditorView extends AppCompatActivity {
    private int customNotificationWeightValue;
    private RecyclerViewAdapterNotificationEditor rcvaNotificationEditor;
    private String notificationCategoryId;
    private MainDatabase db;
    private ActivityNotificationMessageEditorBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityNotificationMessageEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Tools.makeStatusBarTransparent(NotificationManagerEditorView.this);
        ConstraintLayout.LayoutParams lParamsHeading = Tools.getConstraintLayoutParamsTopStatusbar((ConstraintLayout.LayoutParams) binding.txtNotificationEditorHeading.getLayoutParams(), NotificationManagerEditorView.this);
        binding.txtNotificationEditorHeading.setLayoutParams(lParamsHeading);

        db = MainDatabase.getInstance(this);

        if (!getIntent().hasExtra("CATEGORY_ID")) {
            Intent data = new Intent();
            data.putExtra("CATEGORY_ID", "");
            data.putExtra("OBFUSCATION_TYPE_ID", getIntent().getIntExtra("OBFUSCATION_TYPE_ID", 0));
            setResult(RESULT_CANCELED, data);
            finish();
        }

        Intent data = new Intent();
        data.putExtra("CATEGORY_ID", getIntent().getStringExtra("CATEGORY_ID"));
        data.putExtra("OBFUSCATION_TYPE_ID", getIntent().getIntExtra("OBFUSCATION_TYPE_ID", 0));
        setResult(RESULT_OK, data);

        notificationCategoryId = getIntent().getStringExtra("CATEGORY_ID");

        List<NotificationMessage> notificationMessages = db.getNotificationMessageDao().getAllOfCategory(notificationCategoryId).blockingGet();

        rcvaNotificationEditor = new RecyclerViewAdapterNotificationEditor(this, notificationMessages);
        rcvaNotificationEditor.setOnMessageClickedListener(this::createAndShowBottomSheetConfigurator);

        // The following code is because of the issue of moving the first object causes a weird scroll
        // This workaround makes it look a little better but is not perfect still
        rcvaNotificationEditor.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                super.onItemRangeMoved(fromPosition, toPosition, itemCount);
                if (fromPosition == 0 || toPosition == 0) {
                    binding.rcvNotificationMessages.scrollToPosition(0);
                }
            }
        });
        binding.rcvNotificationMessages.setAdapter(rcvaNotificationEditor);
        binding.rcvNotificationMessages.setLayoutManager(new LinearLayoutManager(this));
        binding.btnAddNotificationMessage.setOnClickListener(e -> createAndShowBottomSheetConfigurator(null));
    }

    private void createAndShowBottomSheetConfigurator(NotificationMessage message) {
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogStyle);
        SheetNotificationMessageBinding sBinding = SheetNotificationMessageBinding.inflate(getLayoutInflater());
        bottomSheetDialog.setContentView(sBinding.getRoot());

        Chip[] customNotificationWeightChips = new Chip[] {
                sBinding.chpNotificationWeight1,
                sBinding.chpNotificationWeight2,
                sBinding.chpNotificationWeight3,
                sBinding.chpNotificationWeight4
        };

        // Setting default values
        customNotificationWeightValue = 6;

        sBinding.chkCustomNotificationWeight.setOnCheckedChangeListener((compoundButton, checked) -> {
            sBinding.chpGrpNotificationWeight.setVisibility(checked ? View.VISIBLE : View.GONE);
        });
        sBinding.chpNotificationWeightCustom.setOnClickListener(e -> {
            final NumberPicker numberPicker = new NumberPicker(this);
            numberPicker.setMaxValue(99);
            numberPicker.setMinValue(1);
            numberPicker.setValue(customNotificationWeightValue);

            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
            builder.setView(numberPicker);
            builder.setTitle("Daily notifications count");
            builder.setMessage("Choose an amount");
            builder.setPositiveButton(getResources().getString(R.string.ok), (dialog, which) -> {
                customNotificationWeightValue = numberPicker.getValue();
                sBinding.chpNotificationWeightCustom.setText("Custom (" + customNotificationWeightValue + ")");
            });
            builder.setNegativeButton(getResources().getString(R.string.cancel), null);
            builder.create();
            builder.show();
        });
        sBinding.btnCancel.setOnClickListener(e -> bottomSheetDialog.cancel());
        sBinding.btnSave.setOnClickListener(e -> {
            if (message != null){
                int origObfuscationTypeId = message.getObfuscationTypeId();
                message.setMessage(sBinding.txtCustomNotificationMessage.getText().toString());
                int weight = sBinding.chkCustomNotificationWeight.isChecked() ? (sBinding.chpNotificationWeightCustom.isChecked() ? customNotificationWeightValue : getSelectedWeight(customNotificationWeightChips)) : 1;
                if (weight != -1) { message.setWeight(weight); }
                message.setObfuscationTypeId(0);
                db.getNotificationMessageDao().update(message).blockingAwait();
                rcvaNotificationEditor.notifyMessageChanged(message, origObfuscationTypeId, message.getObfuscationTypeId());
            }
            else {
                int weight = sBinding.chkCustomNotificationWeight.isChecked() ? (sBinding.chpNotificationWeightCustom.isChecked() ? customNotificationWeightValue : getSelectedWeight(customNotificationWeightChips)) : 1;
                if (weight == -1) weight = 1;
                NotificationMessage newMessage = new NotificationMessage(
                        getNotificationCategoryId(),
                        sBinding.txtCustomNotificationMessage.getText().toString(),
                        0,
                        weight
                );
                long messageId = db.getNotificationMessageDao().insert(newMessage);
                newMessage.setId((int)messageId);
                rcvaNotificationEditor.notifyMessageAdded(newMessage);
            }

            // TODO save changes / create new notification message
            bottomSheetDialog.dismiss();
        });

        if (message != null) {
            sBinding.txtCustomNotificationMessage.setText(message.getMessage());
            switch(message.getWeight()) {
                case 1:
                    sBinding.chkCustomNotificationWeight.setChecked(false);
                    break;
                case 2:
                case 3:
                case 4:
                case 5:
                    sBinding.chkCustomNotificationWeight.setChecked(true);
                    customNotificationWeightChips[message.getWeight() - 2].setChecked(true);
                    break;
                default:
                    sBinding.chkCustomNotificationWeight.setChecked(true);
                    sBinding.chpNotificationWeightCustom.setChecked(true);
                    customNotificationWeightValue = message.getWeight();
                    sBinding.chpNotificationWeightCustom.setText("Custom (" + customNotificationWeightValue + ")");
            }
        }
        else {
            sBinding.txtCustomNotificationMessage.requestFocus();
        }

        // Fix issue with EditText hidden behind soft-keyboard
        bottomSheetDialog.setOnShowListener(dialog -> new Handler().postDelayed(() -> {
            BottomSheetDialog d = (BottomSheetDialog) dialog;
            BottomSheetBehavior<View> bottomSheetBehavior = BottomSheetBehavior.from(d.findViewById(R.id.design_bottom_sheet));
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }, 0));

        bottomSheetDialog.show();
    }

    private String getNotificationCategoryId() {
        return notificationCategoryId;
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