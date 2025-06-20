package com.bitflaker.lucidsourcekit.setup;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.data.enums.AuthTypes;
import com.bitflaker.lucidsourcekit.databinding.FragmentSetupPrivacyBinding;

public class SetupPrivacyView extends Fragment {
    private OnAuthTypeChangedListener mListener;
    private FragmentSetupPrivacyBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSetupPrivacyBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.chkUseBiometrics.setOnCheckedChangeListener((compoundButton, b) -> {
            @DrawableRes int icon = b ? R.drawable.ic_baseline_check_box_24 : R.drawable.ic_baseline_check_box_outline_blank_24;
            binding.chkUseBiometrics.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0);
        });
        binding.spnrLockType.setPopupBackgroundResource(R.drawable.popup_menu_background_dark);
        binding.spnrLockType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                switch (AuthTypes.values()[i]) {
                    case Password:
                    case Pin:
                        binding.chkUseBiometrics.setVisibility(View.VISIBLE);
                        binding.chkUseBiometrics.setEnabled(true);
                        break;
                    case None:
                        binding.chkUseBiometrics.setVisibility(View.INVISIBLE);
                        binding.chkUseBiometrics.setEnabled(false);
                        break;
                }
                if(mListener != null) {
                    mListener.onEvent(AuthTypes.values()[i]);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) { }
        });
    }

    public void updateLanguages() {
        binding.txtPrivacyTitle.setText(getContext().getResources().getString(R.string.setup_security_title));
        binding.txtPrivacyDescription.setText(getContext().getResources().getString(R.string.setup_security_description));
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(), R.array.lock_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        int selPos = binding.spnrLockType.getSelectedItemPosition();
        binding.spnrLockType.setAdapter(adapter);
        binding.spnrLockType.setSelection(selPos, false);
        binding.chkUseBiometrics.setText(R.string.setup_privacy_biometrics);
    }

    public boolean getUseBiometrics(){
        return binding.chkUseBiometrics.isChecked();
    }

    public interface OnAuthTypeChangedListener {
        void onEvent(AuthTypes authType);
    }

    public void setOnAuthTypeChangedListener(OnAuthTypeChangedListener eventListener) {
        mListener = eventListener;
    }
}