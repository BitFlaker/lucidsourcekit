package com.bitflaker.lucidsourcekit.main.notification.views

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bitflaker.lucidsourcekit.database.notifications.entities.NotificationCategory
import com.bitflaker.lucidsourcekit.databinding.EntryNotificationCategoryBinding
import com.bitflaker.lucidsourcekit.main.notification.views.RecyclerViewAdapterNotificationCategories.MainViewHolder

class RecyclerViewAdapterNotificationCategories(
    private val context: Context?,
    private val notificationCategories: MutableList<NotificationCategory>
) : RecyclerView.Adapter<MainViewHolder>() {
    class MainViewHolder(var binding: EntryNotificationCategoryBinding) : RecyclerView.ViewHolder(binding.root)
    var notificationCategoryChanged: (() -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
        val binding = EntryNotificationCategoryBinding.inflate(LayoutInflater.from(context), parent, false)
        return MainViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return notificationCategories.size
    }

    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
        val current = notificationCategories[position]
        holder.binding.txtNotificationCategoryHeading.text = current.itemHeading
        holder.binding.txtNotificationCategoryDescription.text = current.itemDescription
        holder.binding.txtNotificationCategoryCount.text = if (current.dailyNotificationCount == 0 || !current.isEnabled) "Disabled" else current.dailyNotificationCount.toString() + " daily"
        holder.binding.crdNotificationEntry.setOnClickListener {
            current.categoryClickedListener.notificationCategoryClicked()
        }
    }

    fun openSettingsForCategoryId(autoOpenId: String) {
        for (notificationCategory in notificationCategories) {
            if (notificationCategory.id == autoOpenId) {
                notificationCategory.categoryClickedListener.notificationCategoryClicked()
                break
            }
        }
    }

    fun notifyCategoryChanged(category: NotificationCategory) {
        for (i in notificationCategories.indices) {
            if (notificationCategories[i].id == category.id) {
                notificationCategories[i] = category
                notifyItemChanged(i)
                notificationCategoryChanged?.invoke()
                break
            }
        }
    }

//    val dailyNotificationCount: Int
//        get() = notificationCategories.filter { it.isEnabled }.sumOf { it.dailyNotificationCount }
//
//    val enabledCategoriesCount: Int
//        get() = notificationCategories.count { it.isEnabled }

    val notificationTimeframeFrom: Long
        get() = notificationCategories
            .filter { it.isEnabled }
            .minByOrNull { it.timeFrom }
            ?.timeFrom ?: -1L

    val notificationTimeframeTo: Long
        get() = notificationCategories
            .filter { it.isEnabled }
            .maxByOrNull { it.timeTo }
            ?.timeTo ?: -1L
}
