package com.bitflaker.lucidsourcekit;

import android.animation.ValueAnimator;
import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.alarms.AlarmReceiverManager;
import com.bitflaker.lucidsourcekit.alarms.OutsideSlider;
import com.bitflaker.lucidsourcekit.alarms.QuickAccessAction;
import com.bitflaker.lucidsourcekit.alarms.RecyclerViewAdapterQuickAccessActions;
import com.bitflaker.lucidsourcekit.general.JournalTypes;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.bitflaker.lucidsourcekit.main.dreamjournal.DreamJournalEntryEditor;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class AlarmDisplayer extends AppCompatActivity {
    private AlarmManager alarmManager;
    private PendingIntent alarmIntent;
    private OutsideSlider alarmSlider;
    private RecyclerView quickAccessActionsView;
    private ImageButton closeDisplayer;
    private TextView currentTimeView, alarmName;
    private boolean isSnoozing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_displayer);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            keyguardManager.requestDismissKeyguard(this, null);
        }
        else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        Tools.makeStatusBarTransparent(this);
        alarmSlider = findViewById(R.id.oss_alarm_slider);
        quickAccessActionsView = findViewById(R.id.rcv_quick_access_actions);
        closeDisplayer = findViewById(R.id.btn_close_displayer);
        currentTimeView = findViewById(R.id.txt_current_time);
        alarmName = findViewById(R.id.txt_alarm_name);
        alarmSlider.setData(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_check_24, getTheme()), ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_snooze_24, getTheme()));
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        quickAccessActionsView.setVisibility(View.GONE);
        closeDisplayer.setVisibility(View.GONE);
        quickAccessActionsView.setAlpha(0);
        closeDisplayer.setAlpha(0.0f);

        alarmSlider.setOnLeftSideSelectedListener(() -> {
            isSnoozing = false;
            Intent intent = new Intent(this, AlarmReceiverManager.class);
            alarmIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
            if (alarmManager != null) {
                alarmManager.cancel(alarmIntent);
                System.out.println("Alarm cancelled");
            }
        });

        alarmSlider.setOnRightSideSelectedListener(() -> {
            isSnoozing = true;
            System.out.println("SNOOZING");
        });

        alarmSlider.setOnFadedAwayListener(() -> {
            alarmSlider.setVisibility(View.GONE);
            getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            if(!isSnoozing){
                quickAccessActionsView.setVisibility(View.VISIBLE);
                closeDisplayer.setVisibility(View.VISIBLE);
                ValueAnimator opacityAnim = ValueAnimator.ofFloat(0, 1);
                opacityAnim.setDuration(300);
                opacityAnim.setInterpolator(new LinearInterpolator());
                opacityAnim.addUpdateListener((valueAnimator) -> {
                    quickAccessActionsView.setAlpha((float)valueAnimator.getAnimatedValue());
                    closeDisplayer.setAlpha((float)valueAnimator.getAnimatedValue());
                });
                opacityAnim.start();
            }
            else {
                finish();
            }
        });

        List<QuickAccessAction> quickAccessActions = new ArrayList<>();
        quickAccessActions.add(new QuickAccessAction("Add text journal entry", "Write down your dreams now so you do not forget them", ResourcesCompat.getDrawable(getResources(), R.drawable.ic_round_book_24, getTheme()), ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_text_fields_24, getTheme()), () -> {
            showJournalCreator(JournalTypes.Text);
        }));
        quickAccessActions.add(new QuickAccessAction("Add forms journal entry", "Write down your dreams into the set template for writing them down quickly and easily", ResourcesCompat.getDrawable(getResources(), R.drawable.ic_round_book_24, getTheme()), ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_ballot_24, getTheme()), () -> {
            showJournalCreator(JournalTypes.Forms);
        }));
        quickAccessActions.add(new QuickAccessAction("Listen to binaural beats", "Listening to binaural beats while going back to sleep might help to induce lucid dreams", ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_bedtime_24, getTheme()), null, () -> {

        }));
        RecyclerViewAdapterQuickAccessActions rcvAQAA = new RecyclerViewAdapterQuickAccessActions(this, quickAccessActions);
        rcvAQAA.setOnEntryClickedListener(quickAccessAction -> quickAccessAction.getOnSelectedListener().onEvent());
        quickAccessActionsView.setLayoutManager(new LinearLayoutManager(this));
        quickAccessActionsView.setAdapter(rcvAQAA);
        closeDisplayer.setOnClickListener(e -> finish());

        Calendar cal = Calendar.getInstance();
        currentTimeView.setText(String.format(Locale.ENGLISH, "%2d:%2d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)));
        cal.set(Calendar.SECOND, cal.get(Calendar.SECOND) + 1);
        cal.set(Calendar.MILLISECOND, 0);

        Timer clockTimer = new Timer();
        clockTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Calendar cal = Calendar.getInstance();
                runOnUiThread(() -> currentTimeView.setText(String.format(Locale.ENGLISH, "%2d:%2d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))));
            }
        }, cal.getTimeInMillis() - Calendar.getInstance().getTimeInMillis(), 1000);
    }

    private void showJournalCreator(JournalTypes type) {
        // TODO start loading animation
        Intent intent = new Intent(this, DreamJournalEntryEditor.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("type", type.ordinal());
        startActivity(intent);
    }
}