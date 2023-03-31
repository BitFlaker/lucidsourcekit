package com.bitflaker.lucidsourcekit.notification;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.google.android.material.card.MaterialCardView;

import java.util.Collections;
import java.util.List;

public class RecyclerViewAdapterNotificationEditor extends RecyclerView.Adapter<RecyclerViewAdapterNotificationEditor.MainViewHolderNotificationEditor> {
    private final Context context;
    private List<NotificationMessage> notificationMessages;
    private OnMessageClickedListener mMessageClickedListener;

    /**
     *
     * @param context
     * @param notificationMessages List of messages ordered by OBFUSCATION-TYPE-ID, ID
     */
    public RecyclerViewAdapterNotificationEditor(Context context, List<NotificationMessage> notificationMessages) {
        this.context = context;
        this.notificationMessages = notificationMessages;
    }

    @NonNull
    @Override
    public MainViewHolderNotificationEditor onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.entry_notification_editor, parent, false);
        return new MainViewHolderNotificationEditor(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MainViewHolderNotificationEditor holder, int position) {
        NotificationMessage current = notificationMessages.get(position);

        LinearLayout.LayoutParams lParamsHeading = (LinearLayout.LayoutParams)holder.headingContainer.getLayoutParams();
        lParamsHeading.topMargin = Tools.dpToPx(context, position == 0 ? 6 : 36);
        holder.headingContainer.setLayoutParams(lParamsHeading);

        LinearLayout.LayoutParams lParamsCard = (LinearLayout.LayoutParams)holder.card.getLayoutParams();
        lParamsCard.bottomMargin = Tools.dpToPx(context, position == notificationMessages.size() - 1 ? 20 : 0);
        holder.card.setLayoutParams(lParamsCard);

        if(position == 0 || notificationMessages.get(position-1).getObfuscationTypeId() != current.getObfuscationTypeId()){
            holder.headingContainer.setVisibility(View.VISIBLE);
            holder.heading.setText(resolveObfuscationTypeHeading(current.getObfuscationTypeId()));
        }
        else {
            holder.headingContainer.setVisibility(View.GONE);
        }
        holder.message.setText(current.getMessage());
        holder.obfuscationIndicator.setImageDrawable(resolveObfuscationTypeIcon(current.getObfuscationTypeId()));
        if(mMessageClickedListener != null) {
            holder.card.setOnClickListener(e -> mMessageClickedListener.onEvent(current));
        }
    }

    private Drawable resolveObfuscationTypeIcon(int obfuscationTypeId) {
        int resource;
        switch (obfuscationTypeId) {
            case 0:
                resource = R.drawable.round_notifications_none_24;
                break;
            case 1:
                resource = R.drawable.round_notifications_24;
                break;
            case 2:
                resource = R.drawable.round_notification_important_24;
                break;
            default:
                resource = R.drawable.ic_baseline_cross_24;
                break;
        }
        return ResourcesCompat.getDrawable(context.getResources(), resource, context.getTheme());
    }

    private String resolveObfuscationTypeHeading(int obfuscationTypeId) {
        switch (obfuscationTypeId) {
            case 0:
                return "Transparent mode";
            case 1:
                return "Neutral mode";
            case 2:
                return "Obfuscated mode";
            default:
                return "-- INVALID MODE --";
        }
    }

    @Override
    public int getItemCount() {
        return notificationMessages.size();
    }

    public void setOnMessageClickedListener(OnMessageClickedListener messageClickedListener) {
        this.mMessageClickedListener = messageClickedListener;
    }

    public void notifyMessageChanged(NotificationMessage message, boolean obfuscationTypeIdChanged) {
        if(!obfuscationTypeIdChanged){
            int index = notificationMessages.indexOf(message);
            notifyItemChanged(index);
        }
        else {
            int previousIndex = notificationMessages.indexOf(message);
            notificationMessages.remove(previousIndex);
            int suitableIndex = getSuitableIndex(message);
            System.out.println(suitableIndex);
            notificationMessages.add(suitableIndex, message);
            notifyItemMoved(previousIndex, suitableIndex);
            // TODO: if you move the last item up, the special last item bottom margin will remain on the item
        }
    }

    public void notifyMessageAdded(NotificationMessage newMessage) {
        if(notificationMessages.size() == 0){
            notificationMessages.add(newMessage);
            notifyItemInserted(0);
        }
        else {
            int suitableIndex = getSuitableIndex(newMessage);
            if(suitableIndex >= 0){
                notificationMessages.add(suitableIndex, newMessage);
                notifyItemInserted(suitableIndex);
            }
            else {
                System.err.println("notifyMessageAdded(NotificationMessage) -> suitableIndex = " + suitableIndex);
            }
        }
    }

    private int getSuitableIndex(NotificationMessage newMessage) {
        int index = Collections.binarySearch(notificationMessages, newMessage, (message1, message2) -> {
            if (message1.getObfuscationTypeId() == message2.getObfuscationTypeId()) {
                return message1.getId() - message2.getId();
            } else {
                return message1.getObfuscationTypeId() - message2.getObfuscationTypeId();
            }
        });
        if (index < 0) {
            index = -(index + 1);
        }
        return index;
    }

    public static class MainViewHolderNotificationEditor extends RecyclerView.ViewHolder {
        MaterialCardView card;
        LinearLayout headingContainer;
        TextView heading, message;
        ImageView obfuscationIndicator;

        public MainViewHolderNotificationEditor(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.crd_notification_message);
            headingContainer = itemView.findViewById(R.id.ll_notification_obfuscation_heading);
            heading = itemView.findViewById(R.id.txt_notification_obfuscation_heading);
            message = itemView.findViewById(R.id.txt_notification_message);
            obfuscationIndicator = itemView.findViewById(R.id.img_notification_obfuscation_indicator);
        }
    }

    public interface OnMessageClickedListener {
        void onEvent(NotificationMessage message);
    }
}
