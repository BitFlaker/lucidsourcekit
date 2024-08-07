package com.bitflaker.lucidsourcekit.setup;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.data.enums.AuthTypes;
import com.google.android.material.button.MaterialButton;

public class SetupAuthDataView extends Fragment {
    private AlphaAnimation buttonClick = new AlphaAnimation(1F, 0.5F);
    private AuthTypes selectedAuthType = AuthTypes.Pin;
    private String enteredPin;
    private TextView txtAuthDataTitle;
    private TextView txtAuthDataDescription;
    private LinearLayout llPinLayout;
    private EditText txtPassword;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setup_auth_data, container, false);

        txtAuthDataTitle = view.findViewById(R.id.txt_auth_data_title);
        txtAuthDataDescription = view.findViewById(R.id.txt_auth_data_description);
        llPinLayout = view.findViewById(R.id.ll_setup_pinLayout);
        txtPassword = view.findViewById(R.id.txt_setup_password);

        showCredentialsSetup(selectedAuthType);

        return view;
    }

    public void updateLanguages() {
        if(getView() != null) {
            ((TextView)getView().findViewById(R.id.txt_setup_password)).setHint(getContext().getResources().getString(R.string.login_password_hint));
            showCredentialsSetup(selectedAuthType);
        }
    }

    public void showCredentialsSetup(AuthTypes authType) {
        selectedAuthType = authType;
        View view = getView();
        if(view != null) {
            txtAuthDataTitle = view.findViewById(R.id.txt_auth_data_title);
            txtAuthDataDescription = view.findViewById(R.id.txt_auth_data_description);
            llPinLayout = view.findViewById(R.id.ll_setup_pinLayout);
            txtPassword = view.findViewById(R.id.txt_setup_password);
        }
        if(view != null && txtAuthDataTitle != null && txtAuthDataDescription != null && llPinLayout != null && txtPassword != null) {
            switch (selectedAuthType) {
                case Password: showPasswordSetup(); break;
                case Pin: showPinSetup(); break;
                case None: showFinishSetup(); break;
            }
        }
    }

    public void showPasswordSetup() {
        txtAuthDataTitle.setText(getContext().getResources().getString(R.string.setup_auth_data_title_password));
        txtAuthDataDescription.setText(R.string.setup_privacy_password);
        llPinLayout.setVisibility(View.GONE);
        txtPassword.setText("");
        txtPassword.setVisibility(View.VISIBLE);
    }

    public void showFinishSetup() {
        txtAuthDataTitle.setText(getContext().getResources().getString(R.string.setup_finish_setup_title));
        txtAuthDataDescription.setText(R.string.setup_finish_content);
        llPinLayout.setVisibility(View.GONE);
        txtPassword.setVisibility(View.GONE);
    }

    public void showPinSetup() {
        txtAuthDataTitle.setText(R.string.setup_auth_data_title_pin);
        txtAuthDataDescription.setText(R.string.setup_privacy_pin);
        llPinLayout.setVisibility(View.VISIBLE);
        txtPassword.setVisibility(View.GONE);
        setupPinAuthentication();
    }

    private void setupPinAuthentication() {
        buttonClick.setDuration(100);

        View thisView = getView();
        enteredPin = "";
        ((TextView)thisView.findViewById(R.id.txt_setup_enteredPin)).setText("");

        MaterialButton btn_pin0 = (MaterialButton)thisView.findViewById(R.id.btn_setup_pin0);
        MaterialButton btn_pin1 = (MaterialButton)thisView.findViewById(R.id.btn_setup_pin1);
        MaterialButton btn_pin2 = (MaterialButton)thisView.findViewById(R.id.btn_setup_pin2);
        MaterialButton btn_pin3 = (MaterialButton)thisView.findViewById(R.id.btn_setup_pin3);
        MaterialButton btn_pin4 = (MaterialButton)thisView.findViewById(R.id.btn_setup_pin4);
        MaterialButton btn_pin5 = (MaterialButton)thisView.findViewById(R.id.btn_setup_pin5);
        MaterialButton btn_pin6 = (MaterialButton)thisView.findViewById(R.id.btn_setup_pin6);
        MaterialButton btn_pin7 = (MaterialButton)thisView.findViewById(R.id.btn_setup_pin7);
        MaterialButton btn_pin8 = (MaterialButton)thisView.findViewById(R.id.btn_setup_pin8);
        MaterialButton btn_pin9 = (MaterialButton)thisView.findViewById(R.id.btn_setup_pin9);
        MaterialButton[] pinButtons = { btn_pin0, btn_pin1, btn_pin2, btn_pin3, btn_pin4, btn_pin5, btn_pin6, btn_pin7, btn_pin8, btn_pin9};
        MaterialButton btn_delete = (MaterialButton)thisView.findViewById(R.id.btn_setup_delete);

        for (MaterialButton pinButton : pinButtons) {
            pinButton.setOnClickListener(e -> {
                pinButton.startAnimation(buttonClick);
                TextView txtEnteredPin = (TextView)thisView.findViewById(R.id.txt_setup_enteredPin);
                txtEnteredPin.setText(txtEnteredPin.getText() + "\u2022");
                enteredPin += pinButton.getText();

                if(enteredPin.length() > 8) {
                    enteredPin = "";
                    txtEnteredPin.setText("");
                    Toast.makeText(getContext(), "PIN too long!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        btn_delete.setOnClickListener(e -> {
            btn_delete.startAnimation(buttonClick);
            if(enteredPin.length() > 0) {
                enteredPin = enteredPin.substring(0, enteredPin.length() - 1);
                TextView txtEnteredPin = (TextView)thisView.findViewById(R.id.txt_setup_enteredPin);
                StringBuilder pinText = new StringBuilder();
                for(int i = 0; i < enteredPin.length(); i++) { pinText.append("\u2022"); }
                txtEnteredPin.setText(pinText.toString());
            }
        });
    }

    public String getPin() {
        return enteredPin;
    }

    public String getPassword() {
        return String.valueOf(((TextView)getView().findViewById(R.id.txt_setup_password)).getText());
    }
}