package com.bitflaker.lucidsourcekit.notification;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.database.notifications.entities.NotificationCategory;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class RecyclerViewAdapterNotificationCategories extends RecyclerView.Adapter<RecyclerViewAdapterNotificationCategories.MainViewHolderNotificationCategories> {
    private final Context context;
    private final List<NotificationCategory> notificationCategories;

    public RecyclerViewAdapterNotificationCategories(Context context, List<NotificationCategory> notificationCategories) {
        this.context = context;
        this.notificationCategories = notificationCategories;
    }

    @NonNull
    @Override
    public MainViewHolderNotificationCategories onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.notification_category_entry, parent, false);
        return new MainViewHolderNotificationCategories(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MainViewHolderNotificationCategories holder, int position) {
        NotificationCategory current = notificationCategories.get(position);
        holder.heading.setText(current.getItemHeading());
        holder.description.setText(current.getItemDescription());
        holder.count.setText(current.getDailyNotificationCount() == 0 || !current.isEnabled() ? "No notifications" : current.getDailyNotificationCount() + " daily notifications");
        holder.categoryIcon.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(), current.getDrawable(), context.getTheme()));
        holder.activeIndicator.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(), current.isEnabled() ? R.drawable.check_badge_check : R.drawable.check_badge_cross, context.getTheme()));
        holder.card.setOnClickListener(e -> current.getCategoryClickedListener().notificationCategoryClicked());
    }

    @Override
    public int getItemCount() {
        return notificationCategories.size();
    }

    public static class MainViewHolderNotificationCategories extends RecyclerView.ViewHolder {
        MaterialCardView card;
        TextView heading, description, count;
        ImageView activeIndicator, categoryIcon;

        public MainViewHolderNotificationCategories(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.crd_notification_entry);
            heading = itemView.findViewById(R.id.txt_notification_category_heading);
            description = itemView.findViewById(R.id.txt_notification_category_description);
            count = itemView.findViewById(R.id.txt_notification_category_count);
            activeIndicator = itemView.findViewById(R.id.img_notification_category_active);
            categoryIcon = itemView.findViewById(R.id.img_notification_category_icon);
        }
    }
}
