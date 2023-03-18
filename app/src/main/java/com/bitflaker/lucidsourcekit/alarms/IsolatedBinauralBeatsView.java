package com.bitflaker.lucidsourcekit.alarms;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.bitflaker.lucidsourcekit.main.BinauralBeatsView;

public class IsolatedBinauralBeatsView extends AppCompatActivity {
    private Fragment binauralBeatsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Tools.getTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_isolated_binaural_beats_view);
        Tools.makeStatusBarTransparent(this);

        binauralBeatsFragment = new BinauralBeatsView();
        loadFragment(binauralBeatsFragment);
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.csl_isolated_binaural_beats_player, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAndRemoveTask();
    }
}