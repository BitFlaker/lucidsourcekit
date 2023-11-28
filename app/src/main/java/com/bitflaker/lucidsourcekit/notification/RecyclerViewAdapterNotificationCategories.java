package com.bitflaker.lucidsourcekit.notification;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.notifications.entities.NotificationCategory;
import com.bitflaker.lucidsourcekit.general.NotificationObfuscationLookup;

import java.util.Comparator;
import java.util.List;

public class RecyclerViewAdapterNotificationCategories extends RecyclerView.Adapter<RecyclerViewAdapterNotificationCategories.MainViewHolderNotificationCategories> {
    private final Context context;
    private final List<NotificationCategory> notificationCategories;
    private OnNotificationCategoryChanged mNotificationCategoryChanged;

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
        Drawable enabledStateDrawable = ResourcesCompat.getDrawable(context.getResources(), current.isEnabled() ? R.drawable.round_check_24 : R.drawable.round_clear_24, context.getTheme());
        holder.count.setCompoundDrawablesRelativeWithIntrinsicBounds(enabledStateDrawable, null, null, null);
        holder.categoryIcon.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(), current.getDrawable(), context.getTheme()));

        holder.card.setBackground(ResourcesCompat.getDrawable(context.getResources(), current.isEnabled() ? R.drawable.shade_enabled : R.drawable.shade_disabled, context.getTheme()));

        holder.card.setOnClickListener(e -> current.getCategoryClickedListener().notificationCategoryClicked());
    }

    @Override
    public int getItemCount() {
        return notificationCategories.size();
    }

    public void openSettingsForCategoryId(String autoOpenId) {
        for(NotificationCategory notificationCategory : notificationCategories) {
            if (notificationCategory.getId().equals(autoOpenId)) {
                notificationCategory.getCategoryClickedListener().notificationCategoryClicked();
                break;
            }
        }
    }

    public void notifyCategoryChanged(NotificationCategory category) {
        for (int i = 0; i < notificationCategories.size(); i++) {
            if (notificationCategories.get(i).getId().equals(category.getId())) {
                notificationCategories.set(i, category);
                notifyItemChanged(i);
                if(mNotificationCategoryChanged != null) {
                    mNotificationCategoryChanged.onEvent();
                }
                break;
            }
        }
    }

    public int getDailyNotificationCount() {
        int totalDailyCount = 0;
        for (NotificationCategory notificationCategory : notificationCategories) {
            if (notificationCategory.isEnabled()) {
                totalDailyCount += notificationCategory.getDailyNotificationCount();
            }
        }
        return totalDailyCount;
    }

    public long getEnabledCategoriesCount() {
        return notificationCategories.stream().filter(NotificationCategory::isEnabled).count();
    }

    public long getNotificationTimeframeFrom() {
        return notificationCategories.stream()
                .filter(NotificationCategory::isEnabled)
                .min(Comparator.comparingLong(NotificationCategory::getTimeFrom))
                .map(NotificationCategory::getTimeFrom)
                .orElse(-1L);
    }

    public long getNotificationTimeframeTo() {
        return notificationCategories.stream()
                .filter(NotificationCategory::isEnabled)
                .max(Comparator.comparingLong(NotificationCategory::getTimeTo))
                .map(NotificationCategory::getTimeTo)
                .orElse(-1L);
    }

    public int getObfuscationPercentage() {
        int totalNotificationCount = 0;
        double notificationPercentageValue = 0;
        NotificationObfuscationLookup lookup = NotificationObfuscationLookup.parse(MainDatabase.getInstance(context).getNotificationMessageDao().getMessageCountsForObfuscationType().blockingGet());
        for (NotificationCategory category : notificationCategories) {
            long notificationCount = lookup.getNotificationCount(category.getId(), category.getObfuscationTypeId());
            totalNotificationCount += notificationCount;
            notificationPercentageValue += notificationCount * (category.getObfuscationTypeId() * 0.5);
        }
        return (int) (notificationPercentageValue / totalNotificationCount * 100);
    }

    public static class MainViewHolderNotificationCategories extends RecyclerView.ViewHolder {
        ConstraintLayout card;
        TextView heading, description, count;
        ImageView categoryIcon;

        public MainViewHolderNotificationCategories(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.cl_notification_entry);
            heading = itemView.findViewById(R.id.txt_notification_category_heading);
            description = itemView.findViewById(R.id.txt_notification_category_description);
            count = itemView.findViewById(R.id.txt_notification_category_count);
            categoryIcon = itemView.findViewById(R.id.img_notification_category_icon);
        }
    }

    public interface OnNotificationCategoryChanged {
        void onEvent();
    }

    public void setNotificationCategoryChangedListener(OnNotificationCategoryChanged listener) {
        this.mNotificationCategoryChanged = listener;
    }
}
