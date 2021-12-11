package com.bitflaker.lucidsourcekit.setup;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.general.AuthTypes;

public class SetupPrivacy extends Fragment {
    OnAuthTypeChangedListener mListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_setup_privacy, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        CheckBox chkUseBiometrics = ((CheckBox)getView().findViewById(R.id.chk_use_biometrics));
        chkUseBiometrics.setOnCheckedChangeListener((compoundButton, b) -> {
            if(b){
                chkUseBiometrics.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_baseline_check_box_24, 0, 0, 0);
                return;
            }
            chkUseBiometrics.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_baseline_check_box_outline_blank_24, 0, 0, 0);
        });
        ((Spinner)getView().findViewById(R.id.spnr_lock_type)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                switch (AuthTypes.values()[i]) {
                    case Password:
                    case Pin: chkUseBiometrics.setEnabled(true); break;
                    case None: chkUseBiometrics.setEnabled(false); break;
                }
                if(mListener!=null) {
                    mListener.onEvent(AuthTypes.values()[i]);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    public void updateLanguages() {
        if(getView() != null){
            ((TextView)getView().findViewById(R.id.txt_privacy_title)).setText(getContext().getResources().getString(R.string.setup_security_title));
            ((TextView)getView().findViewById(R.id.txt_privacy_description)).setText(getContext().getResources().getString(R.string.setup_security_description));
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(), R.array.lock_types, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            Spinner spnrLockTypes = (Spinner)getView().findViewById(R.id.spnr_lock_type);
            int selPos = spnrLockTypes.getSelectedItemPosition();
            spnrLockTypes.setAdapter(adapter);
            spnrLockTypes.setSelection(selPos, false);
            ((CheckBox) getView().findViewById(R.id.chk_use_biometrics)).setText(R.string.setup_privacy_biometrics);
        }
    }

    public boolean getUseBiometrics(){
        return ((CheckBox)getView().findViewById(R.id.chk_use_biometrics)).isChecked();
    }

    public interface OnAuthTypeChangedListener {
        void onEvent(AuthTypes authType);
    }

    public void setOnAuthTypeChangedListener(OnAuthTypeChangedListener eventListener) {
        mListener = eventListener;
    }
}