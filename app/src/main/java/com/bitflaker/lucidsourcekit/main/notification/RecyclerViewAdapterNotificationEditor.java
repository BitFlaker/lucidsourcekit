package com.bitflaker.lucidsourcekit.main.notification;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.database.notifications.entities.NotificationMessage;
import com.bitflaker.lucidsourcekit.databinding.EntryNotificationEditorBinding;

import java.util.Collections;
import java.util.List;

public class RecyclerViewAdapterNotificationEditor extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final Context context;
    private final List<NotificationMessage> notificationMessages;
    private OnMessageClickedListener mMessageClickedListener;

    /**
     *
     * @param context the context for the recycler view adapter
     * @param notificationMessages list of messages ordered by OBFUSCATION-TYPE-ID, ID
     */
    public RecyclerViewAdapterNotificationEditor(Context context, List<NotificationMessage> notificationMessages) {
        this.context = context;
        this.notificationMessages = notificationMessages;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        return new MainViewHolderNotificationMessage(EntryNotificationEditorBinding.inflate(inflater, parent, false), viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        NotificationMessage current = notificationMessages.get(position);
        MainViewHolderNotificationMessage currentHolder = (MainViewHolderNotificationMessage)holder;
        currentHolder.binding.txtNotificationMessage.setText(current.getMessage());
        if (mMessageClickedListener != null) {
            currentHolder.binding.crdNotificationMessage.setOnClickListener(e -> mMessageClickedListener.onEvent(current));
        }
    }

    @Override
    public int getItemCount() {
        return notificationMessages.size();
    }

    public void setOnMessageClickedListener(OnMessageClickedListener messageClickedListener) {
        this.mMessageClickedListener = messageClickedListener;
    }

    public void notifyMessageChanged(NotificationMessage message, int oldObfuscationTypeId, int newObfuscationTypeId) {
        if (oldObfuscationTypeId == newObfuscationTypeId) {
            int index = notificationMessages.indexOf(message);
            notifyItemChanged(index);
        }
        else {
            int previousIndex = notificationMessages.indexOf(message);
            notificationMessages.remove(previousIndex);
            int suitableIndex = getSuitableIndex(message);
            notificationMessages.add(suitableIndex, message);
            notifyItemMoved(previousIndex, suitableIndex);
            notifyItemChanged(suitableIndex);
        }
    }

    public void notifyMessageAdded(NotificationMessage newMessage) {
        if (notificationMessages.isEmpty()) {
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

    public static class MainViewHolderNotificationMessage extends RecyclerView.ViewHolder {
        int viewType;
        EntryNotificationEditorBinding binding;

        public MainViewHolderNotificationMessage(@NonNull EntryNotificationEditorBinding binding, int viewType) {
            super(binding.getRoot());
            this.viewType = viewType;
            this.binding = binding;
        }
    }

    public interface OnMessageClickedListener {
        void onEvent(NotificationMessage message);
    }
}
