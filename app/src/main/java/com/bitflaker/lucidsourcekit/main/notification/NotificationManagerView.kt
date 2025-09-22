package com.bitflaker.lucidsourcekit.main.notification

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.NumberPicker
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bitflaker.lucidsourcekit.database.MainDatabase
import com.bitflaker.lucidsourcekit.database.notifications.entities.NotificationCategory
import com.bitflaker.lucidsourcekit.databinding.ActivityNotificationManagerBinding
import com.bitflaker.lucidsourcekit.databinding.SheetNotificationSettingsBinding
import com.bitflaker.lucidsourcekit.utils.Tools
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.data.datastore.DataStoreKeys
import com.bitflaker.lucidsourcekit.data.datastore.getSetting
import com.bitflaker.lucidsourcekit.data.datastore.updateSetting
import com.bitflaker.lucidsourcekit.main.alarms.AlarmHandler
import com.bitflaker.lucidsourcekit.utils.showToastLong
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.InvalidParameterException
import java.text.DateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.Array
import kotlin.Int
import kotlin.IntArray
import kotlin.Long
import kotlin.arrayOf
import kotlin.text.format

private const val PERMISSION_REQUEST_CODE = 772

class NotificationManagerView : AppCompatActivity() {
    private var customDailyNotificationsCount = 0
    private var currentDeliveryProgress = 0
    private lateinit var rcvaNotificationCategories: RecyclerViewAdapterNotificationCategories
    private lateinit var db: MainDatabase
    private lateinit var binding: ActivityNotificationManagerBinding
    private var categorySettingsBottomSheet: BottomSheetDialog? = null
    private lateinit var categories: MutableList<NotificationCategory>
    private val deliveryStatusUpdated: Runnable = Runnable {
        Handler(Looper.getMainLooper()).postDelayed(deliveryStatusUpdated, (60 * 1000).toLong())
        updateDeliveryProgress()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = MainDatabase.getInstance(this)
        binding = ActivityNotificationManagerBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Load notification categories and set handlers
        lifecycleScope.launch(Dispatchers.IO) {
            categories = db.notificationCategoryDao.getAll()
            for (category in categories) {
                when (category.id) {
                    "DJR" -> {
                        category.itemHeading = "Dream journal"
                        category.itemDescription = "Reminder for writing down your dreams to the dream journal in order to improve dream recall"
                        category.drawable = R.drawable.ic_baseline_text_fields_24
                    }
                    "RCR" -> {
                        category.itemHeading = "Reality check"
                        category.itemDescription = "Reminder for performing a reality check to train performing reality checks in your dreams"
                        category.drawable = R.drawable.round_model_training_24
                    }
                    "DGR" -> {
                        category.itemHeading = "Daily goals"
                        category.itemDescription = "Reminder for taking a look at your daily goals and to look out for them throughout the day"
                        category.drawable = R.drawable.ic_baseline_bookmark_added_24
                    }
                    "CR" -> {
                        category.itemHeading = "Custom"
                        category.itemDescription = "Reminder for everything you want to be reminded about. You can set your own messages"
                        category.drawable = R.drawable.round_lightbulb_24
                    }
                    "PN" -> {
                        category.itemHeading = "Permanent notification"
                        category.itemDescription = "Permanent notification for LucidSourceKit with some general information at a glance"
                        category.drawable = R.drawable.ic_outline_info_24
                    }
                    else -> {
                        category.itemHeading = ""
                        category.itemDescription = ""
                        category.drawable = -1
                    }
                }

                category.setCategoryClickedListener {
                    createAndShowBottomSheetConfigurator(category)
                }
            }

            runOnUiThread {
                val context = this@NotificationManagerView
                rcvaNotificationCategories = RecyclerViewAdapterNotificationCategories(context, categories)
                binding.rcvNotificationCategories.setAdapter(rcvaNotificationCategories)
                binding.rcvNotificationCategories.setLayoutManager(LinearLayoutManager(context))
                rcvaNotificationCategories.notificationCategoryChanged = ::updateDeliveryProgress

                // Handle auto open request for category
                val autoOpenId = intent.getStringExtra("AUTO_OPEN_ID")
                if (!autoOpenId.isNullOrEmpty()) {
                    rcvaNotificationCategories.openSettingsForCategoryId(autoOpenId)
                }

                // Set delivery progress
                currentDeliveryProgress = getNotificationDeliveryProgress()
                binding.spdoNotificationsDelivered.setData(25f, currentDeliveryProgress.toFloat(), 100f)
                binding.spdoNotificationsDelivered.setPercentageData(true)
                binding.spdoNotificationsDelivered.decimalPlaces = 0
                binding.spdoNotificationsDelivered.setDescription("already delivered")

                updateDeliveryProgress()
            }
        }

        // Update delivery progress every minute
        val calendar = Calendar.getInstance()
        val delay = ((60 - calendar[Calendar.SECOND] - 1) * 1000 + 1000 - calendar[Calendar.MILLISECOND]).toLong()
        Handler(Looper.getMainLooper()).postDelayed(deliveryStatusUpdated, delay)

        // Set notification paused handlers
        lifecycleScope.launch(Dispatchers.IO) {
            val isPaused = getSetting(DataStoreKeys.NOTIFICATION_PAUSED_ALL)
            runOnUiThread {
                binding.txtNotificationsDisabledInfo.visibility = if (isPaused) View.VISIBLE else View.GONE
                binding.btnMoreNotificationOptions.setOnClickListener {
                    lifecycleScope.launch(Dispatchers.IO) {
                        showNotificationOptionsPopup()
                    }
                }
            }
        }

        requestPermissionIfRequired()
    }

    private suspend fun showNotificationOptionsPopup() {
        val isPaused = getSetting(DataStoreKeys.NOTIFICATION_PAUSED_ALL) ?: false
        runOnUiThread {
            val popup = PopupMenu(ContextThemeWrapper(this, R.style.Theme_LucidSourceKit_PopupMenu), binding.btnMoreNotificationOptions)
            popup.menuInflater.inflate(R.menu.more_notification_options, popup.menu)
            popup.menu.findItem(R.id.itm_pause_notifications).title = if (isPaused) "Resume notifications" else "Pause notifications"
            popup.setOnMenuItemClickListener { item ->
                lifecycleScope.launch(Dispatchers.IO) {     // TODO: Check can dialogs be shown from IO thread?
                    val isPaused = getSetting(DataStoreKeys.NOTIFICATION_PAUSED_ALL) ?: false
                    when (item.itemId) {
                        R.id.itm_pause_notifications -> showDialogPauseResumeNotifications(popup, isPaused)
                        R.id.itm_disable_notifications -> showDisableNotifications()
                        else -> throw InvalidParameterException("Unknown popup entry selected")
                    }
                }
                true
            }
            popup.show()
        }
    }

    private fun showDialogPauseResumeNotifications(popup: PopupMenu, isPaused: Boolean) = runOnUiThread {
        MaterialAlertDialogBuilder(this, R.style.Theme_LucidSourceKit_ThemedDialog)
            .setTitle(if (isPaused) "Resume notifications" else "Pause notifications")
            .setMessage(if (isPaused) "Do you really want to resume all notifications?" else "Do you really want to pause all notifications for the time being? You can re-enable all notifications any time later.")
            .setNegativeButton(getResources().getString(R.string.no), null)
            .setPositiveButton(getResources().getString(R.string.yes)) { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    updateSetting(DataStoreKeys.NOTIFICATION_PAUSED_ALL, !isPaused)
                    runOnUiThread {
                        val pauseItem = popup.menu.findItem(R.id.itm_pause_notifications)
                        pauseItem.title = if (!isPaused) "Resume notifications" else "Pause notifications"
                        binding.txtNotificationsDisabledInfo.visibility = if (!isPaused) View.VISIBLE else View.GONE
                    }
                }
            }
            .show()
    }

    private fun showDisableNotifications() {
        MaterialAlertDialogBuilder(this, R.style.Theme_LucidSourceKit_ThemedDialog)
            .setTitle("Disable notifications")
            .setMessage("Do you really want to disable all notifications?")
            .setNegativeButton(getResources().getString(R.string.no), null)
            .setPositiveButton(getResources().getString(R.string.yes)) { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    for (category in categories) {
                        category.isEnabled = false
                        db.notificationCategoryDao.update(category)
                        runOnUiThread {
                            rcvaNotificationCategories.notifyCategoryChanged(category)
                        }
                    }
                }
            }
            .show()
    }

    private fun updateDeliveryProgress() {
        val newDeliveryProgress = getNotificationDeliveryProgress()
        if (currentDeliveryProgress != newDeliveryProgress) {
            currentDeliveryProgress = newDeliveryProgress
            binding.spdoNotificationsDelivered.updateValue(currentDeliveryProgress.toFloat())
        }
    }

    private fun getNotificationDeliveryProgress(): Int {
        val from = rcvaNotificationCategories.notificationTimeframeFrom
        val to = rcvaNotificationCategories.notificationTimeframeTo
        val current = Tools.getTimeOfDayMillis(Calendar.getInstance())

        if (current <= from) {
            return 0
        }
        if (current >= to) {
            return 100
        }

        return (100.0 * (current - from) / (to - from)).toInt()
    }

    private fun requestPermissionIfRequired() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
            finish()
        }
    }

    var notificationMessageEditorLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val categoryId = result.data?.getStringExtra("CATEGORY_ID")
        if (result.resultCode == RESULT_OK && categoryId != null) {
            categorySettingsBottomSheet?.let {
                if (it.isShowing) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val counter = it.findViewById<TextView>(R.id.txt_messages_count)!!
                        val count = db.notificationMessageDao.getCountOfMessagesForCategory(categoryId)

                        runOnUiThread {
                            counter.text = String.format(Locale.getDefault(), "%d", count)
                        }
                    }
                }
            }
        }
    }

    private fun createAndShowBottomSheetConfigurator(category: NotificationCategory) {
        val notificationsTimeFrom = Calendar.getInstance()
        val notificationsTimeTo = Calendar.getInstance()
        val tf = DateFormat.getTimeInstance(DateFormat.SHORT)
        val sBinding = SheetNotificationSettingsBinding.inflate(layoutInflater)
        val sheet = BottomSheetDialog(this, R.style.BottomSheetDialogStyle)
        sheet.setContentView(sBinding.root)

        // Set saved values
        notificationsTimeFrom.setTimeInMillis(Tools.getTimeFromCurrentMidnight(category.timeFrom))
        notificationsTimeTo.setTimeInMillis(Tools.getTimeFromCurrentMidnight(category.timeTo))
        customDailyNotificationsCount = category.dailyNotificationCount
        category.isEnabled = customDailyNotificationsCount > 0

        // Set state of full screen notification in reality check category
        if (category.id != "RCR") {
            sBinding.crdFullScreenNotification.visibility = View.GONE
        }

        // Load settings / data and update fields
        lifecycleScope.launch(Dispatchers.IO) {
            val isFullScreenNotification = getSetting(DataStoreKeys.NOTIFICATION_RC_REMINDER_FULL_SCREEN) ?: false
            val messageCount = db.notificationMessageDao.getCountOfMessagesForCategory(category.id)

            runOnUiThread {
                sBinding.swtFullScreenNotification.setChecked(isFullScreenNotification)
                sBinding.txtMessagesCount.text = String.format(Locale.getDefault(), "%d", messageCount)
            }
        }

        // Set field values
        sBinding.txtNotificationCount.text = if (customDailyNotificationsCount == 0) "None" else String.format(Locale.getDefault(), "%d", customDailyNotificationsCount)
        sBinding.txtNotificationTimeFrom.text = tf.format(notificationsTimeFrom.getTime())
        sBinding.txtNotificationTimeTo.text = tf.format(notificationsTimeTo.getTime())
        sBinding.txtNotificationSettingsHeading.text = category.itemHeading

        // Set handler for timeframe start
        sBinding.crdNotificationTimeFrom.setOnClickListener {
            TimePickerDialog(this, { _, hourFrom, minuteFrom ->
                    notificationsTimeFrom.set(Calendar.HOUR_OF_DAY, hourFrom)
                    notificationsTimeFrom.set(Calendar.MINUTE, minuteFrom)
                    sBinding.txtNotificationTimeFrom.text = tf.format(notificationsTimeFrom.getTime())
                    category.timeFrom = Tools.getTimeOfDayMillis(notificationsTimeFrom)
                },
                notificationsTimeFrom.get(Calendar.HOUR_OF_DAY),
                notificationsTimeFrom.get(Calendar.MINUTE),
                true
            ).show()
        }

        // Set handler for timeframe end
        sBinding.crdNotificationTimeTo.setOnClickListener {
            TimePickerDialog(this, { _, hourFrom: Int, minuteFrom: Int ->
                    notificationsTimeTo.set(Calendar.HOUR_OF_DAY, hourFrom)
                    notificationsTimeTo.set(Calendar.MINUTE, minuteFrom)
                    sBinding.txtNotificationTimeTo.text = tf.format(notificationsTimeTo.getTime())
                    category.timeTo = Tools.getTimeOfDayMillis(notificationsTimeTo)
                },
                notificationsTimeTo.get(Calendar.HOUR_OF_DAY),
                notificationsTimeTo.get(Calendar.MINUTE),
                true
            ).show()
        }

        // Set handler for daily notification count
        sBinding.crdDailyNotifications.setOnClickListener {
            val numberPicker = NumberPicker(this).apply {
                setMaxValue(99)
                setMinValue(0)
                value = customDailyNotificationsCount
            }

            MaterialAlertDialogBuilder(this)
                .setView(numberPicker)
                .setTitle("Daily notification count")
                .setPositiveButton(getResources().getString(R.string.ok)) { _, _ ->
                    customDailyNotificationsCount = numberPicker.value
                    category.dailyNotificationCount = customDailyNotificationsCount
                    sBinding.txtNotificationCount.text = if (customDailyNotificationsCount == 0) "None" else String.format(Locale.getDefault(), "%d", customDailyNotificationsCount)
                    category.isEnabled = customDailyNotificationsCount > 0
                }
                .setNegativeButton(getResources().getString(R.string.cancel), null)
                .create()
                .show()
        }

        // Set handler for notification message editing
        sBinding.crdNotificationMessages.setOnClickListener {
            val intent = Intent(this, NotificationManagerEditorView::class.java)
            intent.putExtra("CATEGORY_ID", category.id)
            notificationMessageEditorLauncher.launch(intent)
        }

        // Set handler for toggling full screen notification
        sBinding.crdFullScreenNotification.setOnClickListener {
            sBinding.swtFullScreenNotification.setChecked(!sBinding.swtFullScreenNotification.isChecked)
        }

        // Set save handler
        sBinding.btnSave.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                // Disable category in case there are no compliant notification messages available
                val compliantMessageCount = db.notificationMessageDao.getCountOfMessagesForCategoryAndObfuscationType(category.id)
                if (compliantMessageCount == 0 && category.isEnabled) {
                    runOnUiThread {
                        showToastLong(this@NotificationManagerView, "No messages comply with current settings, disabling category")
                    }
                    category.isEnabled = false
                }

                // Save the changes to the category settings
                db.getNotificationCategoryDao().update(category)

                // Update the fullscreen notification setting for the reality check category
                if (category.id == "RCR") {
                    updateSetting(DataStoreKeys.NOTIFICATION_RC_REMINDER_FULL_SCREEN, sBinding.swtFullScreenNotification.isChecked)
                }

                // Schedule the next notification
                AlarmHandler.scheduleNextNotification(this@NotificationManagerView)

                // Close BottomSheet and notify about changed settings
                runOnUiThread {
                    sheet.dismiss()
                    rcvaNotificationCategories.notifyCategoryChanged(category)
                }
            }
        }

        sheet.show()
        categorySettingsBottomSheet = sheet
    }
}