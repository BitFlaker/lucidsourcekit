package com.bitflaker.lucidsourcekit.setup;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.databinding.FragmentSetupOpenSourceBinding;

public class SetupOpenSourceView extends Fragment {
    private FragmentSetupOpenSourceBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSetupOpenSourceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.txtOpenSourceDescription.setMovementMethod(LinkMovementMethod.getInstance());
        binding.btnOpenReadme.setOnClickListener(e -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/BitFlaker/lucidsourcekit/blob/main/README.md"));
            startActivity(browserIntent);
        });
        binding.btnPrivacyPolicy.setOnClickListener(e -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://bitflaker.github.io/lucidsourcekit/privacy"));
            startActivity(browserIntent);
        });
    }

    public void onLanguageUpdated() {
        if(getView() != null){
            binding.txtOpenSourceTitle.setText(getContext().getResources().getString(R.string.setup_open_source_title));
            binding.txtOpenSourceDescription.setText(getContext().getResources().getString(R.string.setup_open_source_description));
        }
    }
}