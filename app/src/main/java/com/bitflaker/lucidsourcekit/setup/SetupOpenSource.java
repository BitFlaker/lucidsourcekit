package com.bitflaker.lucidsourcekit.setup;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bitflaker.lucidsourcekit.R;

public class SetupOpenSource extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_setup_open_source, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((TextView) getView().findViewById(R.id.txt_open_source_description)).setMovementMethod(LinkMovementMethod.getInstance());
        getView().findViewById(R.id.btn_open_readme).setOnClickListener(e -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/BitFlaker/lucidsourcekit/blob/main/README.md"));
            startActivity(browserIntent);
        });
    }

    public void updateLanguages() {
        if(getView() != null){
            ((TextView)getView().findViewById(R.id.txt_open_source_title)).setText(getContext().getResources().getString(R.string.setup_open_source_title));
            ((TextView)getView().findViewById(R.id.txt_open_source_description)).setText(getContext().getResources().getString(R.string.setup_open_source_description));
        }
    }
}