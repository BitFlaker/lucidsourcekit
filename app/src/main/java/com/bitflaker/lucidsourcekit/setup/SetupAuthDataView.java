package com.bitflaker.lucidsourcekit.setup;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.data.enums.AuthTypes;
import com.bitflaker.lucidsourcekit.databinding.FragmentSetupAuthDataBinding;
import com.google.android.material.button.MaterialButton;

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

    public void updateLanguages() {
        if(getView() != null) {
            binding.txtSetupPassword.setHint(getContext().getResources().getString(R.string.login_password_hint));
            showCredentialsSetup(selectedAuthType);
        }
    }

    public void showCredentialsSetup(AuthTypes authType) {
        selectedAuthType = authType;
        if (binding == null) {
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
        buttonClick.setDuration(100);
        enteredPin = "";
        binding.txtSetupEnteredPin.setText("");

        MaterialButton[] pinButtons = {
                binding.btnSetupPin0,
                binding.btnSetupPin1,
                binding.btnSetupPin2,
                binding.btnSetupPin3,
                binding.btnSetupPin4,
                binding.btnSetupPin5,
                binding.btnSetupPin6,
                binding.btnSetupPin7,
                binding.btnSetupPin8,
                binding.btnSetupPin9,
        };

        for (MaterialButton pinButton : pinButtons) {
            pinButton.setOnClickListener(e -> {
                pinButton.startAnimation(buttonClick);
                binding.txtSetupEnteredPin.setText(binding.txtSetupEnteredPin.getText() + "•");
                enteredPin += pinButton.getText();

                if(enteredPin.length() > 8) {
                    enteredPin = "";
                    binding.txtSetupEnteredPin.setText("");
                    Toast.makeText(getContext(), "PIN too long!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        binding.btnSetupDelete.setOnClickListener(e -> {
            binding.btnSetupDelete.startAnimation(buttonClick);
            if(!enteredPin.isEmpty()) {
                enteredPin = enteredPin.substring(0, enteredPin.length() - 1);
                StringBuilder pinText = new StringBuilder();
                for(int i = 0; i < enteredPin.length(); i++) { pinText.append("•"); }
                binding.txtSetupEnteredPin.setText(pinText.toString());
            }
        });
    }

    public String getPin() {
        return enteredPin;
    }

    public String getPassword() {
        return String.valueOf(binding.txtSetupPassword.getText());
    }
}