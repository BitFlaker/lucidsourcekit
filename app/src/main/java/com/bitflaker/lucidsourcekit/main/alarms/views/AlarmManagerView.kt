package com.bitflaker.lucidsourcekit.main.alarms.views

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.database.MainDatabase
import com.bitflaker.lucidsourcekit.databinding.ActivityAlarmManagerBinding
import com.bitflaker.lucidsourcekit.main.alarms.AlarmHandler.cancelRepeatingAlarm
import com.bitflaker.lucidsourcekit.main.alarms.views.RecyclerViewAdapterAlarms.OnSelectionModeStateChanged
import com.bitflaker.lucidsourcekit.utils.Tools
import com.bitflaker.lucidsourcekit.utils.attrColorStateList
import com.bitflaker.lucidsourcekit.utils.dpToPx
import com.bitflaker.lucidsourcekit.utils.onBackPressed
import com.bitflaker.lucidsourcekit.views.SleepClock.ClockType
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Timer
import java.util.TimerTask
import kotlin.Boolean
import kotlin.collections.ArrayList
import kotlin.math.sign
import kotlin.text.isEmpty

class AlarmManagerView : AppCompatActivity() {
    private lateinit var binding: ActivityAlarmManagerBinding
    private var db = MainDatabase.getInstance(this)
    private var adapterAlarms = RecyclerViewAdapterAlarms(this, this, ArrayList())
    private var isInSelectionMode = false
    private var nextAlarmTimeStamp = 0L
    private var nextBedtimeTimeStamp = 0L
    private var nextTimeToCalcTask: TimerTask? = null
    private val alarmInteractionLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        lifecycleScope.launch(Dispatchers.IO) {
            val data = result.data
            if (result.resultCode == RESULT_OK) {
                if (data != null && data.hasExtra("CREATED_NEW_ALARM") && data.hasExtra("ALARM_ID")) {
                    val alarmId = data.getLongExtra("ALARM_ID", -1)
                    if (data.getBooleanExtra("CREATED_NEW_ALARM", false)) {
                        adapterAlarms.loadAddedAlarmWithId(alarmId)
                    } else {
                        adapterAlarms.reloadModifiedAlarmWithId(alarmId)
                    }
                }
                fetchNextAlarmAndDisplay(false)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlarmManagerBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Start the animation cycle of the clock
        binding.slpClock.setClockType(ClockType.DEFAULT)

        // Set handler for Add / Delete alarm button
        binding.fabAddAlarm.setOnClickListener {
            if (!isInSelectionMode) {
                alarmInteractionLauncher.launch(Intent(this, AlarmEditorView::class.java))
            } else {
                showDeleteAllAlarmsPrompt()
            }
        }

        // Configure the alarm listing adapter
        adapterAlarms.horizontalPadding = 8.dpToPx
        adapterAlarms.onSelectionModeStateChangedListener = object : OnSelectionModeStateChanged {
            override fun onSelectionModeEntered() {
                binding.fabAddAlarm.backgroundTintList = attrColorStateList(R.attr.colorErrorContainer)
                binding.fabAddAlarm.setImageTintList(attrColorStateList(R.attr.colorOnErrorContainer))
                binding.fabAddAlarm.setImageResource(R.drawable.ic_baseline_delete_24)
                isInSelectionMode = true
            }

            override fun onSelectionModeLeft() {
                binding.fabAddAlarm.backgroundTintList = attrColorStateList(R.attr.colorPrimaryContainer)
                binding.fabAddAlarm.setImageTintList(attrColorStateList(R.attr.colorOnPrimaryContainer))
                binding.fabAddAlarm.setImageResource(R.drawable.ic_round_add_24)
                isInSelectionMode = false
            }
        }
        adapterAlarms.onEntryClickedListener = { storedAlarm ->
            alarmInteractionLauncher.launch(Intent(this, AlarmEditorView::class.java).apply {
                putExtra("ALARM_ID", storedAlarm.alarmId)
            })
        }
        adapterAlarms.onEntryActiveStateChangedListener = { _, _ ->
            fetchNextAlarmAndDisplay(false)
        }
        binding.rcvListAlarms.setAdapter(adapterAlarms)
        binding.rcvListAlarms.setLayoutManager(LinearLayoutManager(this))

        // Load all alarms to display in the adapter and update the alarm markers on the clock
        lifecycleScope.launch(Dispatchers.IO) {
            val storedAlarms = db.storedAlarmDao.getAll()
            runOnUiThread {
                adapterAlarms.setData(storedAlarms)
            }
            updateClockMarkers()
        }

        // Get next alarm data for preview on the top
        fetchNextAlarmAndDisplay(false)

        // Exit selection mode or close manager on back press
        onBackPressed {
            if (isInSelectionMode) {
                adapterAlarms.setSelectionMode(false)
            } else {
                finish()
            }
        }
    }

    private fun showDeleteAllAlarmsPrompt() {
        MaterialAlertDialogBuilder(this, R.style.Theme_LucidSourceKit_ThemedDialog)
            .setTitle("Delete Alarms")
            .setMessage("Do you really want to delete the selected alarms?")
            .setPositiveButton(getResources().getString(R.string.yes)) { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    val alarmIds = adapterAlarms.selectedStoredAlarmIds
                    val allAlarms = db.storedAlarmDao.getAllById(alarmIds)

                    // TODO: also support one time only alarms
                    // Cancel all scheduled alarms
                    for (alarm in allAlarms) {
                        cancelRepeatingAlarm(applicationContext, alarm.alarmId)
                        db.storedAlarmDao.delete(alarm)
                    }

                    runOnUiThread {
                        adapterAlarms.removeSelectedAlarmIds()
                        fetchNextAlarmAndDisplay(false)
                    }
                }
            }
            .setNegativeButton(getResources().getString(R.string.no), null)
            .show()
    }

    private suspend fun updateClockMarkers() {
        val alarms = db.storedAlarmDao.getAll()
        val alarmEnabledTimes = alarms.groupBy { it.isAlarmActive }
            .mapValues { it.value.map {
                alarm -> alarm.alarmTimestamp
            } }

        runOnUiThread {
            binding.slpClock.setActiveAlarmMarkers(alarmEnabledTimes[true]?.toList())
            binding.slpClock.setInactiveAlarmMarkers(alarmEnabledTimes[false]?.toList())
        }
    }

    private fun fetchNextAlarmAndDisplay(triggeredByAlarm: Boolean) {
        lifecycleScope.launch(Dispatchers.IO) {
            updateClockMarkers()
            val current = db.activeAlarmDao.getNextUpcomingAlarmTimestamp().firstOrNull()
            if (current != null && triggeredByAlarm && nextAlarmTimeStamp != current.alarmTimestamp) {
                adapterAlarms.alarmWentOff(current.alarmTimestamp)
            }

            // Show next alarm / bedtime or reset values in case there is no alarm
            runOnUiThread {
                if (current == null) {
                    nextAlarmTimeStamp = -1
                    nextBedtimeTimeStamp = -1
                    nextTimeToCalcTask?.cancel()
                    nextTimeToCalcTask = null
                    binding.txtTimeToNextBedtime.text = "--"
                    binding.txtTimeToNextAlarm.text = "--"
                } else {
                    nextAlarmTimeStamp = current.alarmTimestamp
                    nextBedtimeTimeStamp = current.bedtimeTimestamp
                    if (nextTimeToCalcTask == null) {
                        startTimeToAlarmUpdater()
                    }
                    setNextTimeTo()
                }
            }
        }
    }

    private fun startTimeToAlarmUpdater() {
        // TODO: Fix that sometimes the alarm still says 00:00:01 even though the alarm already went off (maybe load new data after a slight delay after an exact minute)
        // => for a workaround that might work the alarm should be refreshed 150ms after the alarm should have gone off
        val calDelay = Calendar.getInstance()
        calDelay.set(Calendar.MINUTE, calDelay.get(Calendar.MINUTE) + 1)
        calDelay.set(Calendar.SECOND, 0)
        calDelay.set(Calendar.MILLISECOND, 150)

        // Set preview calculation task
        nextTimeToCalcTask = object : TimerTask() {
            override fun run() {
                setNextTimeTo()
            }
        }

        // Schedule the timer to run every minute and about 150ms after the minute changed
        Timer().schedule(nextTimeToCalcTask, calDelay.timeInMillis - Calendar.getInstance().timeInMillis, (60 * 1000).toLong())
    }

    private fun setNextTimeTo() {
        // Adding 60000 to always round up to the next minute (so when e.g. 50 seconds are left for
        // the alarm to go off, it would show 00:00:00, but with the added 60000ms it will show 00:00:01)
        val diffAlarmTime = nextAlarmTimeStamp + 60000 - Calendar.getInstance().timeInMillis
        val timeOfDayAlarm = Tools.getTimeOfDayMillis(nextAlarmTimeStamp)
        val bedtimeDiff = if (nextBedtimeTimeStamp <= timeOfDayAlarm) timeOfDayAlarm - nextBedtimeTimeStamp else timeOfDayAlarm + 1000 * 60 * 60 * 24 - nextBedtimeTimeStamp
        var diffBedtime = diffAlarmTime - bedtimeDiff
        val bedtimeDirection = sign(diffBedtime.toFloat()).toLong()
        diffBedtime *= bedtimeDirection
        val bedtimeSuffix = if (bedtimeDirection < 0) " ago" else ""

        // Get time to values as formatted timespans
        val timeToAlarm = Tools.getTimeSpanStringZeroed(diffAlarmTime)
        val timeToBedtime = Tools.getTimeSpanStringZeroed(diffBedtime) + bedtimeSuffix

        runOnUiThread {
            binding.txtTimeToNextAlarm.text = timeToAlarm
            binding.txtTimeToNextBedtime.text = timeToBedtime
            if (timeToAlarm.isEmpty()) {
                // TODO: Is this case even possible??
                // Fetching time of next alarm with a delay of 20ms as the rescheduling takes some time
                // and the fetched data might still be the same
                Handler(Looper.getMainLooper()).postDelayed({
                    fetchNextAlarmAndDisplay(true)
                }, 15)
            }
        }
    }
}