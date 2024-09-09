package com.bitflaker.lucidsourcekit.setup;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.databinding.FragmentSetupConsentBinding;

public class SetupConsentView extends Fragment {
    private OnConsentChangedListener mListener;
    private FragmentSetupConsentBinding binding;
    private boolean lastCheckState = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSetupConsentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.chkAcceptRisk.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) {
                binding.chkAcceptRisk.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_baseline_check_box_24, 0, 0, 0);
                if(mListener != null && !lastCheckState){ mListener.onEvent(true); lastCheckState = true; }
                return;
            }
            binding.chkAcceptRisk.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_baseline_check_box_outline_blank_24, 0, 0, 0);
            if(mListener != null && lastCheckState) { mListener.onEvent(false); lastCheckState = false;}
        });
    }

    public void updateLanguages() {
        if (getView() != null) {
            binding.txtExperimentalTitle.setText(getContext().getResources().getString(R.string.setup_experimental_title));
            binding.txtExperimentalDescription.setText(getContext().getResources().getString(R.string.setup_experimental_description));
            binding.chkAcceptRisk.setText(getContext().getResources().getString(R.string.setup_consent_checkbox));
        }
    }

    public interface OnConsentChangedListener {
        void onEvent(Boolean checked);
    }

    public void setOnConsentChangedListener(OnConsentChangedListener eventListener) {
        mListener = eventListener;
    }
}