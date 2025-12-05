package com.bitflaker.lucidsourcekit.main.alarms

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.TextView
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.database.MainDatabase
import com.bitflaker.lucidsourcekit.database.alarms.entities.StoredAlarm
import com.bitflaker.lucidsourcekit.databinding.EntryAlarmBinding
import com.bitflaker.lucidsourcekit.main.alarms.AlarmHandler.cancelRepeatingAlarm
import com.bitflaker.lucidsourcekit.main.alarms.AlarmHandler.scheduleAlarmRepeatedlyAt
import com.bitflaker.lucidsourcekit.main.alarms.RecyclerViewAdapterAlarms.MainViewHolderAlarms
import com.bitflaker.lucidsourcekit.utils.Tools
import com.bitflaker.lucidsourcekit.utils.attrColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class RecyclerViewAdapterAlarms(
    private val lifecycleOwner: LifecycleOwner,
    private val activity: Activity,
    private var storedAlarms: MutableList<StoredAlarm>
) : RecyclerView.Adapter<MainViewHolderAlarms>() {
    class MainViewHolderAlarms(var binding: EntryAlarmBinding) : RecyclerView.ViewHolder(binding.root)

    private lateinit var weekdays: List<TextView>
    private val db = MainDatabase.getInstance(activity)
    private val loadedItems = ArrayList<MainViewHolderAlarms>()
    var onSelectionModeStateChangedListener: OnSelectionModeStateChanged? = null
    var onEntryActiveStateChangedListener: ((StoredAlarm, Boolean) -> Unit)? = null
    var onEntryClickedListener: ((StoredAlarm) -> Unit)? = null
    private var isInSelectionMode = false
    var selectedStoredAlarmIds = ArrayList<Long>()
        private set
    private var selectedIndexes = ArrayList<Int>()
    private var selectionModeEnabled = true
    private var controlsHidden = false
    var horizontalPadding = 0
    var controlsVisible = true

    private val periodicEntryUpdateTask: Runnable = Runnable {
        Handler(Looper.getMainLooper()).postDelayed(periodicEntryUpdateTask, 60000)
        notifyItemRangeChanged(0, itemCount)
    }

    init {
        // Start periodic entry update task at next full minute
        val calendar = Calendar.getInstance()
        val delay = (59000 - calendar.get(Calendar.SECOND) * 1000 + 1000 - calendar.get(Calendar.MILLISECOND)).toLong()
        Handler(Looper.getMainLooper()).postDelayed(periodicEntryUpdateTask, delay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolderAlarms {
        val binding = EntryAlarmBinding.inflate(LayoutInflater.from(activity), parent, false)
        weekdays = listOf(
            binding.txtAlarmsWeekMo,
            binding.txtAlarmsWeekTu,
            binding.txtAlarmsWeekWe,
            binding.txtAlarmsWeekTh,
            binding.txtAlarmsWeekFr,
            binding.txtAlarmsWeekSa,
            binding.txtAlarmsWeekSu
        )
        return MainViewHolderAlarms(binding)
    }

    fun setSelectionModeActive(holder: MainViewHolderAlarms, isInSelectionMode: Boolean) {
        if (!controlsHidden) {
            if (isInSelectionMode) {
                holder.binding.chkAlarmSelected.visibility = View.VISIBLE
                holder.binding.swtAlarmActive.visibility = View.GONE
            } else {
                holder.binding.chkAlarmSelected.isChecked = false
                holder.binding.chkAlarmSelected.visibility = View.GONE
                holder.binding.swtAlarmActive.visibility = View.VISIBLE
            }
        }
    }

    override fun onBindViewHolder(holder: MainViewHolderAlarms, position: Int) {
        loadedItems.add(holder)
        if (!controlsVisible) {
            holder.binding.chkAlarmSelected.visibility = View.GONE
            holder.binding.swtAlarmActive.visibility = View.GONE
            controlsHidden = true
        }
        else {
            holder.binding.txtAlarmIn.visibility = View.GONE
        }

        val alarm = storedAlarms[position]
        holder.binding.txtAlarmsTitle.text = alarm.title
        holder.binding.clQuestionnaire.updateLayoutParams<MarginLayoutParams> {
            setMargins(horizontalPadding, 0, horizontalPadding, 0)
        }

        val alarmHours = TimeUnit.MILLISECONDS.toHours(alarm.alarmTimestamp)
        val alarmMinutes = TimeUnit.MILLISECONDS.toMinutes(alarm.alarmTimestamp) - TimeUnit.HOURS.toMinutes(alarmHours)
        val isPmAm = !DateFormat.is24HourFormat(activity)
        var alarmTime = String.format(Locale.getDefault(), "%02d:%02d", alarmHours, alarmMinutes)

        // TODO: Move to DateFormat instead of SimpleDateFormat
        val clock24H = SimpleDateFormat("HH:mm", Locale.getDefault())
        val clock12H = SimpleDateFormat("hh:mm", Locale.getDefault())
        if (isPmAm) {
            try {
                val date = clock24H.parse(alarmTime) ?: Date()
                alarmTime = clock12H.format(date)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        // TODO: This is most definitely going to cause issues when moving and the data is loaded a little delayed
        //       from the IO dispatcher. This requires some workaround or data updating from outside the recycler view
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            // TODO: Ensure after one time alarm completion changing enabled state does not crash the app
            val nextAlarmTimeStamp = db.activeAlarmDao.getById(alarm.requestCodeActiveAlarm)
            if (nextAlarmTimeStamp == null) {
                storedAlarms.remove(alarm)
                activity.runOnUiThread { notifyItemRemoved(position) }
                return@launch
            }

            activity.runOnUiThread {
                // Calculate time to alarm and
                val duration = nextAlarmTimeStamp.initialTime + 60 * 1000 - Calendar.getInstance().timeInMillis
                holder.binding.txtAlarmIn.text = Tools.getTimeSpanStringZeroed(duration)
                holder.binding.txtAlarmsTimePrim.text = alarmTime
                holder.binding.txtAlarmsTimeSec.text = if (isPmAm && alarmHours < 12) " AM" else if (isPmAm) " PM" else ""

                // Set checked state and handler if the alarm is enabled
                holder.binding.swtAlarmActive.setChecked(alarm.isAlarmActive && alarm.requestCodeActiveAlarm != -1)
                holder.binding.swtAlarmActive.jumpDrawablesToCurrentState()
                holder.binding.swtAlarmActive.setOnClickListener {
                    alarm.isAlarmActive = holder.binding.swtAlarmActive.isChecked
                    lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                        if (holder.binding.swtAlarmActive.isChecked) {
                            val index = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1
                            scheduleAlarmRepeatedlyAt(
                                activity,
                                alarm.alarmId,
                                Tools.getMidnightTime() + alarm.alarmTimestamp,
                                alarm.pattern,
                                index,
                                1000 * 60 * 60 * 24
                            )
                            updateStoredAlarmFromDatabase(position, alarm)
                        } else {
                            cancelRepeatingAlarm(activity, alarm.alarmId)
                            updateStoredAlarmFromDatabase(position, alarm)
                        }
                    }
                    onEntryActiveStateChangedListener?.invoke(storedAlarms[position], holder.binding.swtAlarmActive.isChecked)
                }

                // Highlight the repeat pattern weekdays
                val colorTertiary = activity.attrColor(R.attr.tertiaryTextColor)
                val colorPrimary = activity.attrColor(R.attr.secondaryTextColor)
                for (i in alarm.pattern.indices) {
                    val dayIndicator = weekdays[(i + alarm.pattern.size - 1) % alarm.pattern.size]
                    if (alarm.pattern[i]) {
                        dayIndicator.setTextColor(colorPrimary)
                        dayIndicator.paintFlags = dayIndicator.paintFlags or Paint.UNDERLINE_TEXT_FLAG
                    } else {
                        dayIndicator.setTextColor(colorTertiary)
                        dayIndicator.paintFlags = dayIndicator.paintFlags and (Paint.UNDERLINE_TEXT_FLAG.inv())
                    }
                }

                // Forward touch events of the CheckBox to the card container to handle selection changes
                @SuppressLint("ClickableViewAccessibility")
                holder.binding.chkAlarmSelected.setOnTouchListener { v, event ->
                    val container = v.parent.parent as View
                    event.setLocation(container.width.toFloat(), 0f)
                    container.onTouchEvent(event)
                    true
                }
                setSelectionModeActive(holder, isInSelectionMode && selectionModeEnabled)

                // Set listener for selection mode
                holder.binding.chkAlarmSelected.isChecked = selectedStoredAlarmIds.contains(alarm.alarmId)
                holder.binding.crdAlarm.setOnClickListener {
                    if (!isInSelectionMode || !selectionModeEnabled) {
                        onEntryClickedListener?.invoke(storedAlarms[position])
                        return@setOnClickListener
                    }

                    // Toggle the selected state
                    holder.binding.chkAlarmSelected.isChecked = !holder.binding.chkAlarmSelected.isChecked
                    if (holder.binding.chkAlarmSelected.isChecked) {
                        selectedIndexes.add(position)
                        selectedStoredAlarmIds.add(alarm.alarmId)
                    }
                    else {
                        selectedIndexes.remove(position)
                        selectedStoredAlarmIds.remove(alarm.alarmId)
                        setSelectionMode(!selectedIndexes.isEmpty())
                    }
                }

                // Set long click listener in case selection mode is supposed to be enabled
                if (selectionModeEnabled) {
                    holder.binding.crdAlarm.setOnLongClickListener {
                        if (!isInSelectionMode) {
                            setSelectionMode(true)
                            holder.binding.chkAlarmSelected.isChecked = true
                            selectedIndexes.add(position)
                            selectedStoredAlarmIds.add(alarm.alarmId)
                        }
                        true
                    }
                }
            }
        }
    }

    private suspend fun updateStoredAlarmFromDatabase(position: Int, alarm: StoredAlarm) {
        storedAlarms[position] = db.storedAlarmDao.getById(alarm.alarmId)
        activity.runOnUiThread {
            notifyItemChanged(position)
        }
    }

    override fun onViewRecycled(holder: MainViewHolderAlarms) {
        super.onViewRecycled(holder)
        loadedItems.remove(holder)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(storedAlarms: List<StoredAlarm>) {
        this.storedAlarms = storedAlarms.toMutableList()
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = storedAlarms.size

    fun setSelectionMode(isInSelectionMode: Boolean) {
        if (selectionModeEnabled) {
            selectedStoredAlarmIds = ArrayList()
            selectedIndexes = ArrayList()
            this.isInSelectionMode = isInSelectionMode
            loadedItems.forEach { notifyItemChanged(it.bindingAdapterPosition) }
            if (isInSelectionMode) {
                onSelectionModeStateChangedListener?.onSelectionModeEntered()
            } else {
                onSelectionModeStateChangedListener?.onSelectionModeLeft()
            }
        }
    }

    fun removeSelectedAlarmIds() {
        selectedIndexes.sortWith(Comparator.reverseOrder())
        for (i in selectedIndexes.indices) {
            storedAlarms.removeAt(selectedIndexes[i])
            notifyItemRemoved(selectedIndexes[i])
        }
        setSelectionMode(false)
        loadedItems.forEach {
            notifyItemChanged(it.bindingAdapterPosition)
        }
    }

    suspend fun reloadModifiedAlarmWithId(alarmId: Long) {
        val alarm = db.storedAlarmDao.getById(alarmId)
        val updatedIndex = storedAlarms.indexOfFirst { it.alarmId == alarmId }
        if (updatedIndex == -1) {
            return
        }
        storedAlarms[updatedIndex] = alarm

        activity.runOnUiThread {
            notifyItemChanged(updatedIndex)
        }
    }

    suspend fun loadAddedAlarmWithId(alarmId: Long) {
        storedAlarms.add(db.storedAlarmDao.getById(alarmId))
        activity.runOnUiThread {
            notifyItemInserted(storedAlarms.size - 1)
        }
    }

    suspend fun alarmWentOff(alarmTimestamp: Long) {
        val refreshStoredAlarms = db.activeAlarmDao.getStoredAlarmByAlarmTime(alarmTimestamp)
        val positions = refreshStoredAlarms.map { refreshedId ->
            storedAlarms.indexOfFirst { it.alarmId == refreshedId.alarmId }
        }
        for (i in 0..<positions.size) {
            storedAlarms[positions[i]] = refreshStoredAlarms[i]
            activity.runOnUiThread { notifyItemChanged(positions[i]) }
        }
    }

    fun setSelectionModeEnabled(enabled: Boolean) {
        selectionModeEnabled = enabled
    }

    interface OnSelectionModeStateChanged {
        fun onSelectionModeEntered()
        fun onSelectionModeLeft()
    }
}
