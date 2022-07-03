package com.bitflaker.lucidsourcekit.main;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.general.FontHandler;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.List;

public class RecyclerViewAdapterAlarms extends RecyclerView.Adapter<RecyclerViewAdapterAlarms.MainViewHolderAlarms> {
    private OnEntryClicked mListener;
    private Context context;
    private List<AlarmData> alarmData;

    public RecyclerViewAdapterAlarms(Context context, List<AlarmData> alarmData) {
        this.context = context;
        this.alarmData = alarmData;
    }

    @NonNull
    @Override
    public MainViewHolderAlarms onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.alarm_entry, parent, false);
        return new MainViewHolderAlarms(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MainViewHolderAlarms holder, int position) {
        holder.title.setText(alarmData.get(position).getTitle());
        DateFormat tf = DateFormat.getTimeInstance(DateFormat.SHORT);
        String tString = tf.format(alarmData.get(position).getTime().getTime());
        boolean isPMAM = tString.toUpperCase().contains("PM") || tString.toUpperCase().contains("AM");
        holder.timePrimary.setText(isPMAM ? tString.replace("PM", "").replace("AM", "") : tString);
        holder.timeSecondary.setText(isPMAM ? (tString.toUpperCase().contains("PM") ? "PM" : "AM") : "");
        holder.active.setChecked(alarmData.get(position).isActive());
        Typeface tfThin = FontHandler.getInstance().getFontByName("sans-serif-thin");
        Typeface tfNormal = FontHandler.getInstance().getFontByName("sans-serif");
        // TODO: enable toggle for either not showing disabled days or showing them only slightly
        for (int i = 0; i < holder.weekdays.size(); i++) {
            TextView dayT = holder.weekdays.get(i);
            dayT.setTypeface(tfThin);
            dayT.setTextColor(Tools.getAttrColor(R.attr.secondaryTextColor, context.getTheme()));
            dayT.setVisibility(View.GONE);
        }
        for (AlarmData.ActiveDays day : alarmData.get(position).getActiveDays()) {
            TextView dayT = holder.weekdays.get(day.ordinal());
            dayT.setTypeface(tfNormal);
            dayT.setTextColor(Tools.getAttrColor(R.attr.primaryTextColor, context.getTheme()));
            dayT.setVisibility(View.VISIBLE);
        }
        holder.card.setOnClickListener(e -> {
            // TODO open editor
        });
    }

    @Override
    public int getItemCount() {
        return alarmData.size();
    }

    public class MainViewHolderAlarms extends RecyclerView.ViewHolder {
        TextView title, timePrimary, timeSecondary;
        TextView dayMo, dayTu, dayWe, dayTh, dayFr, daySa, daySu;
        MaterialCardView card;
        SwitchMaterial active;
        List<TextView> weekdays;

        public MainViewHolderAlarms(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.crd_alarm);
            title = itemView.findViewById(R.id.txt_alarms_title);
            timePrimary = itemView.findViewById(R.id.txt_alarms_time_prim);
            timeSecondary = itemView.findViewById(R.id.txt_alarms_time_sec);
            active = itemView.findViewById(R.id.swt_alarm_active);
            dayMo = itemView.findViewById(R.id.txt_alarms_week_mo);
            dayTu = itemView.findViewById(R.id.txt_alarms_week_tu);
            dayWe = itemView.findViewById(R.id.txt_alarms_week_we);
            dayTh = itemView.findViewById(R.id.txt_alarms_week_th);
            dayFr = itemView.findViewById(R.id.txt_alarms_week_fr);
            daySa = itemView.findViewById(R.id.txt_alarms_week_sa);
            daySu = itemView.findViewById(R.id.txt_alarms_week_su);
            weekdays = Arrays.asList(dayMo, dayTu, dayWe, dayTh, dayFr, daySa, daySu);
        }
    }

    public interface OnEntryClicked {
        void onEvent(AlarmData binauralBeat, int position);
    }

    public void setOnEntryClickedListener(OnEntryClicked eventListener) {
        mListener = eventListener;
    }
}
