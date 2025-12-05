package com.bitflaker.lucidsourcekit.setup;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.main.AuthTypes;
import com.bitflaker.lucidsourcekit.databinding.FragmentSetupAuthDataBinding;
import com.bitflaker.lucidsourcekit.main.notification.visual.KeypadAdapter;
import com.bitflaker.lucidsourcekit.main.notification.visual.KeypadButtonModel;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import kotlin.Unit;

public class SetupAuthDataView extends Fragment {
    private final AlphaAnimation buttonClick = new AlphaAnimation(1F, 0.5F);
    private AuthTypes selectedAuthType = AuthTypes.Pin;
    private String enteredPin;
    private FragmentSetupAuthDataBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSetupAuthDataBinding.inflate(inflater, container, false);
        showCredentialsSetup(selectedAuthType);
        return binding.getRoot();
    }

    public void onLanguageUpdated() {
        if(getView() != null) {
            binding.txtSetupPassword.setHint(getContext().getResources().getString(R.string.login_password_hint));
            showCredentialsSetup(selectedAuthType);
        }
    }

    public void showCredentialsSetup(AuthTypes authType) {
        selectedAuthType = authType;
        if (binding == null || getContext() == null) {
            return;
        }
        switch (selectedAuthType) {
            case Password: showPasswordSetup(); break;
            case Pin: showPinSetup(); break;
            case None: showFinishSetup(); break;
        }
    }

    public void showPasswordSetup() {
        binding.txtAuthDataTitle.setText(getContext().getResources().getString(R.string.setup_auth_data_title_password));
        binding.txtAuthDataDescription.setText(R.string.setup_privacy_password);
        binding.llSetupPinLayout.setVisibility(View.GONE);
        binding.txtSetupPassword.setText("");
        binding.txtSetupPassword.setVisibility(View.VISIBLE);
    }

    public void showFinishSetup() {
        binding.txtAuthDataTitle.setText(getContext().getResources().getString(R.string.setup_finish_setup_title));
        binding.txtAuthDataDescription.setText(R.string.setup_finish_content);
        binding.llSetupPinLayout.setVisibility(View.GONE);
        binding.txtSetupPassword.setVisibility(View.GONE);
    }

    public void showPinSetup() {
        binding.txtAuthDataTitle.setText(R.string.setup_auth_data_title_pin);
        binding.txtAuthDataDescription.setText(R.string.setup_privacy_pin);
        binding.llSetupPinLayout.setVisibility(View.VISIBLE);
        binding.txtSetupPassword.setVisibility(View.GONE);
        setupPinAuthentication();
    }

    private void setupPinAuthentication() {
        enteredPin = "";
        binding.txtSetupEnteredPin.setText("");

        // Generate the keypad
        List<KeypadButtonModel> buttons = Arrays.stream(new Character[] {
                '1', '2', '3',
                '4', '5', '6',
                '7', '8', '9',
                null, '0', '#'
        }).map(c -> new KeypadButtonModel(c, null)).collect(Collectors.toList());

        // Set the icon for delete
        buttons.get(buttons.size() - 1).setButtonIcon(ResourcesCompat.getDrawable(getContext().getResources(), R.drawable.rounded_backspace_24, getContext().getTheme()));

        // Configure keypad adapter
        KeypadAdapter keypadAdapter = new KeypadAdapter(getContext(), buttons);
        binding.rcvKeypad.setAdapter(keypadAdapter);
        binding.rcvKeypad.setLayoutManager(new GridLayoutManager(getContext(), 3));
        keypadAdapter.setOnButtonClick(value -> {
            if (value == '#') {
                if (!enteredPin.isEmpty()) {
                    enteredPin = enteredPin.substring(0, enteredPin.length() - 1);
                    StringBuilder pinText = new StringBuilder();
                    for(int i = 0; i < enteredPin.length(); i++) { pinText.append("•"); }
                    binding.txtSetupEnteredPin.setText(pinText.toString());
                }
            }
            else {
                binding.txtSetupEnteredPin.setText(binding.txtSetupEnteredPin.getText() + "•");
                enteredPin += value;
                if (enteredPin.length() > 8) {
                    enteredPin = "";
                    binding.txtSetupEnteredPin.setText("");
                    Toast.makeText(getContext(), "PIN too long!", Toast.LENGTH_SHORT).show();
                }
            }
            return Unit.INSTANCE;
        });
    }

    public String getPin() {
        return enteredPin;
    }

    public String getPassword() {
        return String.valueOf(binding.txtSetupPassword.getText());
    }
}