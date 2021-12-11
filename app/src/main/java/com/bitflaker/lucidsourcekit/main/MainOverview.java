package com.bitflaker.lucidsourcekit.main;

import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bitflaker.lucidsourcekit.R;

public class MainOverview extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main_overview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fillStats();
        // TODO: blue background on nearest mountain / app background is too blue -> make less blue
    }

    private void fillStats() {
        ((TextView) getView().findViewById(R.id.txt_total_journal_entries)).setText(Html.fromHtml("<span><big><big>45</big></big></span><br /><small><small><br /></small></small><span><strong>Total Journal<br />Entries</strong></span>", Html.FROM_HTML_MODE_COMPACT));
        ((TextView) getView().findViewById(R.id.txt_lucid_journal_entries)).setText(Html.fromHtml("<span><big><big>3</big></big></span><br /><small><small><br /></small></small><span><strong>Lucid Journal<br />Entries</strong></span>", Html.FROM_HTML_MODE_COMPACT));
        ((TextView) getView().findViewById(R.id.txt_last_journal_entry)).setText(Html.fromHtml("<span><big><big>34</big></big><br /><small><small><br /></small></small></span><span><strong>Goals Reached<br />in Total</strong></span>", Html.FROM_HTML_MODE_COMPACT));
        ((TextView) getView().findViewById(R.id.txt_app_open_streak)).setText(Html.fromHtml("<span><big><big>28</big></big></span><br /><small><small><br /></small></small><span><strong>Days Opening<br />Streak</strong></span>", Html.FROM_HTML_MODE_COMPACT));

        ((TextView) getView().findViewById(R.id.txt_alarm1)).setText(Html.fromHtml("<span><big><big><strong>03:12</strong></big></big></span><br /><span>first night timer</span>", Html.FROM_HTML_MODE_COMPACT));
        ((TextView) getView().findViewById(R.id.txt_alarm2)).setText(Html.fromHtml("<span><big><big><strong>06:24</strong></big></big></span><br /><span>last night timer</span>", Html.FROM_HTML_MODE_COMPACT));
    }
}