package com.bitflaker.lucidsourcekit.main.notification.views

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bitflaker.lucidsourcekit.database.notifications.entities.NotificationMessage
import com.bitflaker.lucidsourcekit.databinding.EntryNotificationEditorBinding

class RecyclerViewAdapterNotificationEditor(
    private val context: Context?,
    private val notificationMessages: MutableList<NotificationMessage>
) : RecyclerView.Adapter<RecyclerViewAdapterNotificationEditor.MainViewHolder>() {
    class MainViewHolder(var binding: EntryNotificationEditorBinding) : RecyclerView.ViewHolder(binding.root)
    var messageClickedListener: ((NotificationMessage) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
        val binding = EntryNotificationEditorBinding.inflate(LayoutInflater.from(context), parent, false)
        return MainViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return notificationMessages.size
    }

    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
        val current = notificationMessages[position]
        holder.binding.txtNotificationMessage.text = current.message
        if (messageClickedListener != null) {
            holder.binding.crdNotificationMessage.setOnClickListener {
                messageClickedListener?.invoke(current)
            }
        }
    }

    fun notifyMessageChanged(message: NotificationMessage) {
        notifyItemChanged(notificationMessages.indexOf(message))
    }

    fun notifyMessageAdded(newMessage: NotificationMessage) {
        val index = getSuitableIndex(newMessage)
        notificationMessages.add(index, newMessage)
        notifyItemInserted(index)
    }

    private fun getSuitableIndex(newMessage: NotificationMessage): Int {
        if (notificationMessages.isEmpty()) {
            return 0
        }
        var index = notificationMessages.binarySearch(newMessage, Comparator { m1, m2 -> m1.id - m2.id })
        if (index < 0) {
            index = -(index + 1)
        }
        return index
    }
}
