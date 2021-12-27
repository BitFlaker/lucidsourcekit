package com.bitflaker.lucidsourcekit.general;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bitflaker.lucidsourcekit.R;

import java.util.ArrayList;

public class IconSpinnerAdapter extends ArrayAdapter<IconEntry> {
    private LayoutInflater inflater;
    private ArrayList<IconEntry> entries;
    private ViewHolder holder = null;
    private Context context;

    public IconSpinnerAdapter(@NonNull Context context, int resource, ArrayList<IconEntry> entries) {
        super(context, resource, entries);
        inflater = LayoutInflater.from(context);
        this.entries = entries;
        this.context = context;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    public View getCustomView(int position, View convertView, ViewGroup parent){
        IconEntry entry = entries.get(position);
        View row = convertView;

        if(row == null){
            holder = new ViewHolder();
            row = inflater.inflate(R.layout.icon_spinner_row, parent, false);
            holder.icon = (ImageView) row.findViewById(R.id.img_spinner_icon);
            holder.text = (TextView) row.findViewById(R.id.txt_spinner_text);
            row.setTag(holder);
        }
        else{
            holder = (ViewHolder) row.getTag();
        }

        holder.icon.setImageDrawable(context.getDrawable(entry.getIcon()));
        holder.text.setText(entry.getText());

        return row;
    }

    static class ViewHolder {
        TextView text;
        ImageView icon;
    }
}

