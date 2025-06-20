package com.bitflaker.lucidsourcekit.setup;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.data.datastore.DataStoreKeys;
import com.bitflaker.lucidsourcekit.data.datastore.DataStoreManager;
import com.bitflaker.lucidsourcekit.databinding.FragmentSetupLanguageBinding;
import com.bitflaker.lucidsourcekit.utils.Tools;

public class SetupLanguageView extends Fragment {
    private OnLanguageChangedListener mListener;
    private FragmentSetupLanguageBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSetupLanguageBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.spnrLanguage.setPopupBackgroundResource(R.drawable.popup_menu_background_dark);
        binding.spnrLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                // TODO: set originally set language and don't overwrite with default selection
                String lang = binding.spnrLanguage.getSelectedItem().toString().split("\\(")[1];
                lang = lang.substring(0, lang.length() - 1);
                DataStoreManager.getInstance().updateSetting(DataStoreKeys.LANGUAGE, lang).blockingSubscribe();
                binding.txtLanguageSupportNotice.setVisibility(lang.equals("de") ? VISIBLE : GONE);
                Tools.loadLanguage(getActivity());
                if(mListener!=null) {
                    mListener.onEvent(lang);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
    }

    public void updateLanguages() {
        if(getView() != null) {
            binding.welcomeHeader.setText(getContext().getResources().getString(R.string.setup_language_title));
            binding.txtSelectLanguage.setText(getContext().getResources().getString(R.string.setup_languages_description));
        }
    }

    public interface OnLanguageChangedListener {
        void onEvent(String langCode);
    }

    public void setLanguageChangedListener(OnLanguageChangedListener eventListener) {
        mListener = eventListener;
    }
}