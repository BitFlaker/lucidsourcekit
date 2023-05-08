package com.bitflaker.lucidsourcekit.setup;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bitflaker.lucidsourcekit.R;

public class SetupConsent extends Fragment {
    private OnConsentChangedListener mListener;
    private boolean lastCheckState = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_setup_consent, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        CheckBox chkConsent = ((CheckBox)getView().findViewById(R.id.chk_accept_risk));
        chkConsent.setOnCheckedChangeListener((compoundButton, b) -> {
            if(b){
                chkConsent.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_baseline_check_box_24, 0, 0, 0);
                if(mListener != null && !lastCheckState){ mListener.onEvent(true); lastCheckState = true; }
                return;
            }
            chkConsent.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_baseline_check_box_outline_blank_24, 0, 0, 0);
            if(mListener != null && lastCheckState) { mListener.onEvent(false); lastCheckState = false;}
        });
    }

    public void updateLanguages() {
        if(getView() != null){
            ((TextView)getView().findViewById(R.id.txt_experimental_title)).setText(getContext().getResources().getString(R.string.setup_experimental_title));
            ((TextView)getView().findViewById(R.id.txt_experimental_description)).setText(getContext().getResources().getString(R.string.setup_experimental_description));
            ((TextView)getView().findViewById(R.id.chk_accept_risk)).setText(getContext().getResources().getString(R.string.setup_consent_checkbox));
        }
    }

    public interface OnConsentChangedListener {
        void onEvent(Boolean checked);
    }

    public void setOnConsentChangedListener(OnConsentChangedListener eventListener) {
        mListener = eventListener;
    }

    public boolean hasConsentListener() {
        System.out.println("mListener == null: " + (mListener == null));
        return mListener != null;
    }
}