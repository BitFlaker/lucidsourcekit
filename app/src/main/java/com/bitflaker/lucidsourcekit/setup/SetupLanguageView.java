package com.bitflaker.lucidsourcekit.setup;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.utils.Tools;
import com.bitflaker.lucidsourcekit.data.datastore.DataStoreKeys;
import com.bitflaker.lucidsourcekit.data.datastore.DataStoreManager;

public class SetupLanguageView extends Fragment {
    OnLanguageChangedListener mListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_setup_language, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Spinner languageSpinner = getView().findViewById(R.id.spnr_language);
        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                // TODO: set originally set language and don't overwrite with default selection
                String lang = languageSpinner.getSelectedItem().toString().split("\\(")[1];
                lang = lang.substring(0, lang.length() - 1);
                DataStoreManager.getInstance().updateSetting(DataStoreKeys.LANGUAGE, lang).blockingSubscribe();
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
            ((TextView) getView().findViewById(R.id.welcome_header)).setText(getContext().getResources().getString(R.string.setup_language_title));
            ((TextView) getView().findViewById(R.id.txt_select_language)).setText(getContext().getResources().getString(R.string.setup_languages_description));
        }
    }

    public interface OnLanguageChangedListener {
        void onEvent(String langCode);
    }

    public void setLanguageChangedListener(OnLanguageChangedListener eventListener) {
        mListener = eventListener;
    }
}