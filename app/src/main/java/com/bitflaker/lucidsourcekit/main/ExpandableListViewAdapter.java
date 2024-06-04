package com.bitflaker.lucidsourcekit.main;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.general.Tools;

import java.util.HashMap;
import java.util.List;

public class ExpandableListViewAdapter extends BaseExpandableListAdapter {
    private Context context;
    private List<String> groups;
    private HashMap<String, List<GoalSuggestion>> suggestions;

    public ExpandableListViewAdapter(Context context, List<String> groups, HashMap<String, List<GoalSuggestion>> suggestions) {
        this.context = context;
        this.groups = groups;
        this.suggestions = suggestions;
    }

    @Override
    public int getGroupCount() {
        return this.groups.size();
    }

    @Override
    public int getChildrenCount(int groupPos) {
        return this.suggestions.get(this.groups.get(groupPos)).size();
    }

    @Override
    public Object getGroup(int groupPos) {
        return this.groups.get(groupPos);
    }

    @Override
    public Object getChild(int groupPos, int childPos) {
        return this.suggestions.get(groups.get(groupPos)).get(childPos);
    }

    @Override
    public long getGroupId(int groupPos) {
        return groupPos;
    }

    @Override
    public long getChildId(int groupPos, int childPos) {
        return childPos;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPos, boolean isExpanded, View convertView, ViewGroup parent) {
        String headerTitle = (String) getGroup(groupPos);
        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.expandable_suggestions_group, null);
        }

        TextView groupName = convertView.findViewById(R.id.txt_group_header);
        if(!headerTitle.equals("")) {
            groupName.setVisibility(View.VISIBLE);
            groupName.setTypeface(null, Typeface.BOLD);
            groupName.setText(headerTitle);
        }
        else {
            groupName.setVisibility(View.GONE);
        }
        LinearLayout iconContainer = convertView.findViewById(R.id.ll_suggestion_icon_container);
        if(!isExpanded) {
            iconContainer.removeAllViews();
            for (GoalSuggestion entry : getChildren(groupPos)) {
                ImageView icon = new ImageView(context);
                int dp = Tools.dpToPx(context, 6);
                int dpLarger = Tools.dpToPx(context, 30);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(dpLarger, dpLarger);
                layoutParams.setMargins(0, 0, 0, 0);
                icon.setLayoutParams(layoutParams);
                icon.setPadding(dp, dp, dp, dp);
                icon.setBackground(ResourcesCompat.getDrawable(context.getResources(), R.drawable.rounded_spinner, context.getTheme()));
                icon.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.colorSurfaceContainer, context.getTheme()));
                icon.setImageTintList(Tools.getAttrColorStateList(R.attr.tertiaryTextColor, context.getTheme()));
                icon.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(), entry.getIcon(), context.getTheme()));
                iconContainer.addView(icon);
            }
        }
        else {
            iconContainer.removeAllViews();
        }
        return convertView;
    }

    private List<GoalSuggestion> getChildren(int groupPos) {
        return this.suggestions.get(groups.get(groupPos));
    }

    @Override
    public View getChildView(int groupPos, int childPos, boolean isLastChild, View convertView, ViewGroup parent) {
        final GoalSuggestion entry = (GoalSuggestion) getChild(groupPos, childPos);

        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.expandable_suggestions_entry, null);
        }

        TextView description = convertView.findViewById(R.id.txt_suggestion_text);
        description.setText(entry.getText());
        ImageView icon = convertView.findViewById(R.id.txt_suggestion_icon);
        icon.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(), entry.getIcon(), context.getTheme()));
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPos, int childPos) {
        return true;
    }
}
