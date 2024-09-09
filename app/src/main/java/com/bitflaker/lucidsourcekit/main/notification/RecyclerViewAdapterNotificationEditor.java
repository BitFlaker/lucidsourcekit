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
import com.bitflaker.lucidsourcekit.databinding.EntryNotificationEditorHeadingBinding;
import com.bitflaker.lucidsourcekit.utils.Tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class RecyclerViewAdapterNotificationEditor extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int DEFAULT_HEADING_MARGIN_TOP = 36;
    private static final int FIRST_HEADING_MARGIN_TOP = 8;
    private final Context context;
    private final List<NotificationMessageModel> notificationMessages;
    private OnMessageClickedListener mMessageClickedListener;
    private final HashMap<Integer, Integer> obfuscationCategoryCounter;
    private final HashMap<Integer, MainViewHolderNotificationHeading> obfuscationCategoryHeadingLayout;

    /**
     *
     * @param context the context for the recycler view adapter
     * @param notificationMessages list of messages ordered by OBFUSCATION-TYPE-ID, ID
     */
    public RecyclerViewAdapterNotificationEditor(Context context, List<NotificationMessage> notificationMessages) {
        this.context = context;
        this.obfuscationCategoryCounter = new HashMap<>();
        this.obfuscationCategoryHeadingLayout = new HashMap<>();
        this.notificationMessages = addHeadingsToMessages(notificationMessages);
    }

    private List<NotificationMessageModel> addHeadingsToMessages(List<NotificationMessage> notificationMessages) {
        List<NotificationMessageModel> messagesWithHeadings = new ArrayList<>();

        for (int i = 0; i < notificationMessages.size(); i++) {
            if(i == 0 || notificationMessages.get(i).getObfuscationTypeId() != notificationMessages.get(i - 1).getObfuscationTypeId()) {
                messagesWithHeadings.add(new NotificationMessageHeading(notificationMessages.get(i).getObfuscationTypeId()));
            }
            messagesWithHeadings.add(notificationMessages.get(i));
            updateObfuscationCategoryCounter(notificationMessages.get(i).getObfuscationTypeId(), true);
        }

        return messagesWithHeadings;
    }

    private void updateObfuscationCategoryCounter(int obfuscationTypeId, boolean wasAdded) {
        int newValue = obfuscationCategoryCounter.getOrDefault(obfuscationTypeId, 0) + (wasAdded ? 1 : -1);
        obfuscationCategoryCounter.put(obfuscationTypeId, newValue);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if(viewType == 0) {
            return new MainViewHolderNotificationMessage(EntryNotificationEditorBinding.inflate(inflater, parent, false), viewType);
        }
        else {
            return new MainViewHolderNotificationHeading(EntryNotificationEditorHeadingBinding.inflate(inflater, parent, false), viewType);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        NotificationMessageModel currentModel = notificationMessages.get(position);
        if(currentModel.getType() == 0) {
            NotificationMessage current = (NotificationMessage)currentModel;
            MainViewHolderNotificationMessage currentHolder = (MainViewHolderNotificationMessage)holder;

            currentHolder.binding.txtNotificationMessage.setText(current.getMessage());
            currentHolder.binding.imgNotificationObfuscationIndicator.setImageDrawable(resolveObfuscationTypeIcon(current.getObfuscationTypeId()));
            if(mMessageClickedListener != null) {
                currentHolder.binding.crdNotificationMessage.setOnClickListener(e -> mMessageClickedListener.onEvent(current));
            }
        }
        else if (currentModel.getType() == 1) {
            NotificationMessageHeading current = (NotificationMessageHeading)currentModel;
            MainViewHolderNotificationHeading currentHolder = (MainViewHolderNotificationHeading)holder;
            obfuscationCategoryHeadingLayout.put(current.getObfuscationTypeId(), currentHolder);

            currentHolder.binding.txtNotificationObfuscationHeading.setText(resolveObfuscationTypeHeading(current.getObfuscationTypeId()));
            RecyclerView.LayoutParams lParamsHeading = (RecyclerView.LayoutParams)currentHolder.binding.llNotificationObfuscationHeading.getLayoutParams();
            lParamsHeading.topMargin = Tools.dpToPx(context, position == 0 ? FIRST_HEADING_MARGIN_TOP : DEFAULT_HEADING_MARGIN_TOP);

            currentHolder.binding.llNotificationObfuscationHeading.setLayoutParams(lParamsHeading);
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

    @Override
    public int getItemViewType(int position) {
        return notificationMessages.get(position).getType();
    }

    public void setOnMessageClickedListener(OnMessageClickedListener messageClickedListener) {
        this.mMessageClickedListener = messageClickedListener;
    }

    public void notifyMessageChanged(NotificationMessage message, int oldObfuscationTypeId, int newObfuscationTypeId) {
        if(oldObfuscationTypeId == newObfuscationTypeId) {
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

            updateObfuscationCategoryCounter(oldObfuscationTypeId, false);
            updateObfuscationCategoryCounter(newObfuscationTypeId, true);
            updateCategoryHeadings();
        }
    }

    public void notifyMessageAdded(NotificationMessage newMessage) {
        if(notificationMessages.isEmpty()) {
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
                return;
            }
        }
        updateObfuscationCategoryCounter(newMessage.getObfuscationTypeId(), true);
        updateCategoryHeadings();
    }

    private void updateCategoryHeadings() {
        removeEmptyCategoryHeadings();
        addNewCategoryHeadings();
    }

    private void addNewCategoryHeadings() {
        for (int i = 0; i < notificationMessages.size(); i++) {
            if(i == 0 && notificationMessages.get(i).getType() == 0 || notificationMessages.get(i).getType() == 0 && notificationMessages.get(i - 1).getType() == 0 && ((NotificationMessage)notificationMessages.get(i)).getObfuscationTypeId() != ((NotificationMessage)notificationMessages.get(i - 1)).getObfuscationTypeId()) {
                if(i == 0 && notificationMessages.get(i).getType() == 0 && notificationMessages.size() > i + 1 && notificationMessages.get(i + 1).getType() == 1){
                    NotificationMessageHeading heading = (NotificationMessageHeading)notificationMessages.get(i + 1);
                    notifyItemChanged(heading.getObfuscationTypeId(), obfuscationCategoryHeadingLayout.get(heading.getObfuscationTypeId()));
                }
                notificationMessages.add(i, new NotificationMessageHeading(((NotificationMessage) notificationMessages.get(i)).getObfuscationTypeId()));
                notifyItemInserted(i);
                i++;
            }
        }
    }

    private void removeEmptyCategoryHeadings() {
        boolean wasFirstHeadingDeleted = false;
        NotificationMessageHeading nextHeading = null;
        for (int i = 0; i < notificationMessages.size(); i++) {
            if(notificationMessages.get(i).getType() == 1) {
                NotificationMessageHeading heading = (NotificationMessageHeading) notificationMessages.get(i);
                if(wasFirstHeadingDeleted && nextHeading == null) { nextHeading = heading; }
                if(obfuscationCategoryCounter.getOrDefault(heading.getObfuscationTypeId(), 0) == 0) {
                    notificationMessages.remove(i);
                    notifyItemRemoved(i);
                    if(i == 0) { wasFirstHeadingDeleted = true; nextHeading = null; }
                    i--;
                }
            }
        }
        if(wasFirstHeadingDeleted) {
            notifyItemChanged(notificationMessages.indexOf(nextHeading), obfuscationCategoryHeadingLayout.get(nextHeading.getObfuscationTypeId()));
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

    public static class MainViewHolderNotificationHeading extends RecyclerView.ViewHolder {
        int viewType;
        EntryNotificationEditorHeadingBinding binding;

        public MainViewHolderNotificationHeading(@NonNull EntryNotificationEditorHeadingBinding binding, int viewType) {
            super(binding.getRoot());
            this.viewType = viewType;
            this.binding = binding;
        }
    }

    public interface OnMessageClickedListener {
        void onEvent(NotificationMessage message);
    }
}
