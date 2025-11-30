package com.bitflaker.lucidsourcekit.main.alarms

import android.Manifest
import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.data.enums.AlarmToneType
import com.bitflaker.lucidsourcekit.database.MainDatabase
import com.bitflaker.lucidsourcekit.database.alarms.updated.entities.StoredAlarm
import com.bitflaker.lucidsourcekit.databinding.ActivityAlarmEditorBinding
import com.bitflaker.lucidsourcekit.utils.Tools
import com.bitflaker.lucidsourcekit.utils.dpToPx
import com.bitflaker.lucidsourcekit.utils.getParcelableExtraSafe
import com.bitflaker.lucidsourcekit.utils.isPermissionGranted
import com.bitflaker.lucidsourcekit.utils.showToastLong
import com.bitflaker.lucidsourcekit.utils.singleLine
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

private const val PERMISSION_REQUEST_CODE = 776
private val noRepeatPattern = booleanArrayOf(false, false, false, false, false, false, false)
private val everydayRepeatPattern = booleanArrayOf(true, true, true, true, true, true, true)
private val allWeekdaysRepeatPattern = booleanArrayOf(false, true, true, true, true, true, false)
private val allWeekendsRepeatPattern = booleanArrayOf(true, false, false, false, false, false, true)
private val weekdayShorts: Array<String> = arrayOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
private val supportedAudioFiles: Array<String> = arrayOf(
    "3gp",
    "m4a",
    "aac",
    "ts",
    "amr",
    "flac",
    "mid",
    "xmf",
    "mxmf",
    "rtttl",
    "rtx",
    "ota",
    "imy",
    "mp3",
    "mkv",
    "ogg",
    "wav"
)

class AlarmEditorView : AppCompatActivity() {
    private lateinit var ringtoneUri: Uri
    private lateinit var storedAlarm: StoredAlarm
    private lateinit var binding: ActivityAlarmEditorBinding
    private var db = MainDatabase.getInstance(this)
    private var currentVolIncMin = 2
    private var currentVolIncSec = 30
    private lateinit var weekdayChips: Array<Chip>

    val ringtoneSelectorLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        ringtoneUri = result.data?.getParcelableExtraSafe(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            ?: RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
        binding.txtToneSelected.text = RingtoneManager.getRingtone(this, ringtoneUri).getTitle(this)
        storedAlarm.alarmUri = ringtoneUri.toString()
    }

    var customFileSelectorLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        val uri = result.data?.data
        val path = uri?.path
        if (uri == null || path == null) {
            return@registerForActivityResult
        }

        // Ensure the audio type is supported
        val file = File(path)
        if (!supportedAudioFiles.contains(file.extension)) {
            showToastLong(this, "Unsupported audio file type")
            return@registerForActivityResult
        }

        ringtoneUri = uri
        binding.txtToneSelected.text = file.nameWithoutExtension
        storedAlarm.alarmUri = ringtoneUri.toString()
    }

    var scheduleAlarmSettingsLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        val data = result.data
        if (data == null || data.action != AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) {
            showToastLong(this, "Permission required to schedule alarms")
            finish()
        }
    }

    var drawOverOtherAppsSettingsLauncher = registerForActivityResult(StartActivityForResult()) {
        if (!Settings.canDrawOverlays(this)) {
            showToastLong(this, "Permission required to display alarms")
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityAlarmEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        weekdayChips = arrayOf(
            binding.chpSunday,
            binding.chpMonday,
            binding.chpTuesday,
            binding.chpWednesday,
            binding.chpThursday,
            binding.chpFriday,
            binding.chpSaturday
        )

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.txtAlarmName.singleLine()

        // Ensure the app has permission to draw over other apps for the alarm to work properly
        if (!Settings.canDrawOverlays(this)) {
            showRequestDrawOverAppsPermissionDialog()
        }

        // Set repeat weekday button click handlers
        for (i in weekdayChips.indices) {
            val currentButton = weekdayChips[i]
            currentButton.setOnClickListener {
                storedAlarm.pattern[i] = currentButton.isChecked
                updateAlarmRepeatText()
            }
        }

        // Get default ringtone and set alarm selection
        ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
        binding.txtToneSelected.text = RingtoneManager.getRingtone(this, ringtoneUri).getTitle(this)

        // Load alarm data and configure related listeners
        val alarmId = intent.getLongExtra("ALARM_ID", -1)
        if (alarmId != -1L) {
            lifecycleScope.launch(Dispatchers.IO) {
                loadAlarmData(alarmId)
            }
        } else {
            loadDefaultAlarmData()
            setTimeChangedListeners()
        }

        // Set handler for changing the bed time
        binding.crdBedtimeTime.setOnClickListener {
            TimePickerDialog(
                this,
                { _, hour, minute -> binding.slpClkSetTime.setBedTime(hour, minute) },
                binding.slpClkSetTime.hoursToBedTime,
                binding.slpClkSetTime.minutesToBedTime,
                true
            ).show()
        }

        // Set handler for changing the alarm time
        binding.crdAlarmTime.setOnClickListener {
            TimePickerDialog(
                this,
                { _, hour, minute -> binding.slpClkSetTime.setAlarmTime(hour, minute) },
                binding.slpClkSetTime.hoursToAlarm,
                binding.slpClkSetTime.minutesToAlarm,
                true
            ).show()
        }

        // Set sleep clock properties
        binding.slpClkSetTime.setDrawHours(true)
        binding.slpClkSetTime.isDrawTimeSetterButtons = true

        // Set handler for change in selected alarm tone type
        binding.chpGrpAlarmTone.setOnCheckedStateChangeListener { _, _ ->
            if (binding.chpRingtone.isChecked) {
                storedAlarm.alarmToneTypeId = AlarmToneType.RINGTONE.ordinal
                binding.txtToneSelected.text = RingtoneManager.getRingtone(this, ringtoneUri).getTitle(this)
                storedAlarm.alarmUri = ringtoneUri.toString()
            } else if (binding.chpBinauralBeats.isChecked) {
                // TODO: Implement binaural beats selection and alarm
                storedAlarm.alarmToneTypeId = AlarmToneType.BINAURAL_BEAT.ordinal
                binding.txtToneSelected.text = "- NONE -"
                storedAlarm.alarmUri = Uri.EMPTY.toString()
            } else if (binding.chpCustomFile.isChecked) {
                if (!isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
                }
                storedAlarm.alarmToneTypeId = AlarmToneType.CUSTOM_FILE.ordinal
                binding.txtToneSelected.text = "- NONE -"
                storedAlarm.alarmUri = Uri.EMPTY.toString()
            }
        }

        // Set handler for selecting alarm tone of selected alarm tone type
        binding.crdAlarmTone.setOnClickListener {
            if (binding.chpRingtone.isChecked) {
                // TODO: Do not pass ringtone uri if the uri did not come from a ringtone selection (e.g. custom file)
                ringtoneSelectorLauncher.launch(Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Tone")
                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, ringtoneUri)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                })
            } else if (binding.chpBinauralBeats.isChecked) {
                // TODO: open binaural beats selector
            } else if (binding.chpCustomFile.isChecked) {
                customFileSelectorLauncher.launch(Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.INTERNAL_CONTENT_URI))
            }
        }

        // Set handler for change in alarm tone volume
        binding.sldAlarmVolume.addOnChangeListener { _, value, _ ->
            binding.txtCurrAlarmVolume.text = String.format(Locale.ENGLISH, "%d%%", (value * 100).roundToInt())
            storedAlarm.alarmVolume = value
        }

        // Set handler for alarm volume increase time
        binding.crdAlarmVolumeIncrease.setOnClickListener {
            showAlarmVolumeIncreaseDialog()
        }

        // Set handler for vibration and flashlight enabled status
        binding.swtVibrateAlarm.setOnCheckedChangeListener { _, checked ->
            storedAlarm.isVibrationActive = checked
        }
        binding.swtAlarmUseFlashlight.setOnCheckedChangeListener { _, checked ->
            storedAlarm.isFlashlightActive = checked
        }

        // Set handler for creating and setting the alarm
        binding.btnCreateAlarm.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                // TODO: make checks (like no tone selected with custom file)
                storedAlarm.title = binding.txtAlarmName.getText().toString()
                storedAlarm.isAlarmActive = true
                // TODO: make this work with one time only alarms as well
                if (storedAlarm.alarmId == 0L) {
                    // Create the alarm and schedule it
                    storedAlarm.alarmId = db.storedAlarmDao.insert(storedAlarm)
                    scheduleAlarmAndExit(true)
                } else {
                    // Cancel the alarm if it currently is running, then update the stored alarm in the
                    // database and finally schedule the alarm updated alarm
                    AlarmHandler.cancelRepeatingAlarm(applicationContext, storedAlarm.alarmId)
                    storedAlarm.requestCodeActiveAlarm = -1
                    db.storedAlarmDao.update(storedAlarm)
                    scheduleAlarmAndExit(false)
                }
            }
        }

        // Ensure the permission to schedule exact alarms is granted
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            showRequestExactAlarmPermission()
        }

        // Set handler for back button
        binding.btnCloseAlarmCreator.setOnClickListener { finish() }
    }

    private suspend fun loadAlarmData(alarmId: Long) {
        storedAlarm = db.storedAlarmDao.getById(alarmId)
        runOnUiThread {
            setEditValuesFromItem()
            binding.slpClkSetTime.setOnFirstDrawFinishedListener {
                val alarmHours = TimeUnit.MILLISECONDS.toHours(storedAlarm.alarmTimestamp)
                val alarmMinutes = TimeUnit.MILLISECONDS.toMinutes(storedAlarm.alarmTimestamp) - TimeUnit.HOURS.toMinutes(alarmHours)
                val bedtimeHours = TimeUnit.MILLISECONDS.toHours(storedAlarm.bedtimeTimestamp)
                val bedtimeMinutes = TimeUnit.MILLISECONDS.toMinutes(storedAlarm.bedtimeTimestamp) - TimeUnit.HOURS.toMinutes(bedtimeHours)
                binding.slpClkSetTime.setAlarmTime(alarmHours.toInt(), alarmMinutes.toInt())
                binding.slpClkSetTime.setBedTime(bedtimeHours.toInt(), bedtimeMinutes.toInt())
                runOnUiThread {
                    setTimeChangedListeners()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun showRequestExactAlarmPermission() {
        MaterialAlertDialogBuilder(this, R.style.Theme_LucidSourceKit_ThemedDialog)
            .setTitle("Permission")
            .setMessage("To ensure alarms go off on time, the permission to schedule exact alarms is required. Grant the permission to proceed.")
            .setPositiveButton(getResources().getString(R.string.ok)) { _, _ ->
                scheduleAlarmSettingsLauncher.launch(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            }
            .setOnCancelListener { dialog: DialogInterface? ->
                showToastLong(this, "Permission required to schedule alarms")
                finish()
            }
            .show()
    }

    private fun showAlarmVolumeIncreaseDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setLayoutParams(LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        }

        // Create and add minute part
        val minuteNumberPicker = NumberPicker(this).apply {
            value = currentVolIncMin
            setMaxValue(59)
            setMinValue(0)
        }
        container.addView(minuteNumberPicker)

        // Create and add colon separator
        container.addView(TextView(this).apply {
            text = ":"
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                leftMargin = 5.dpToPx
                rightMargin = 5.dpToPx
            }
        })

        // Create and add second part
        val secondsNumberPicker = NumberPicker(this).apply {
            maxValue = 59
            minValue = 0
            value = currentVolIncSec
        }
        container.addView(secondsNumberPicker)

        // Show the dialog containing the controls
        MaterialAlertDialogBuilder(this)
            .setView(container)
            .setTitle("Increase volume for")
            .setPositiveButton(getResources().getString(R.string.ok)) { _, _ ->
                currentVolIncMin = minuteNumberPicker.value
                currentVolIncSec = secondsNumberPicker.value
                binding.txtIncVolumeFor.text = String.format(Locale.ENGLISH, "%dm %ds", currentVolIncMin, currentVolIncSec)
                storedAlarm.alarmVolumeIncreaseTimestamp = currentVolIncMin.toLong() * 60L * 1000L + currentVolIncSec.toLong() * 1000L
            }
            .setNegativeButton(getResources().getString(R.string.cancel), null)
            .create()
            .show()
    }

    private fun showRequestDrawOverAppsPermissionDialog() {
        MaterialAlertDialogBuilder(this, R.style.Theme_LucidSourceKit_ThemedDialog)
            .setTitle("Permission")
            .setMessage("Displaying an alarm requires the app to open the alarm viewer on top of other applications. Grant the permission to proceed.")
            .setPositiveButton(getResources().getString(R.string.ok)) { _, _ ->
                drawOverOtherAppsSettingsLauncher.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri()))
            }
            .setOnCancelListener {
                showToastLong(this, "Permission required to display alarms")
                finish()
            }
            .show()
    }

    private suspend fun scheduleAlarmAndExit(createdNewAlarm: Boolean) {
        AlarmHandler.scheduleAlarmRepeatedlyAt(
            applicationContext,
            storedAlarm.alarmId,
            Tools.getMidnightTime() + storedAlarm.alarmTimestamp,
            storedAlarm.pattern,
            Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1,
            1000 * 60 * 60 * 24
        )
        setResult(RESULT_OK, Intent().apply {
            putExtra("CREATED_NEW_ALARM", createdNewAlarm)
            putExtra("ALARM_ID", storedAlarm.alarmId)
        })
        finish()
    }

    private fun loadDefaultAlarmData() {
        storedAlarm = StoredAlarm()
        storedAlarm.alarmId = 0
        storedAlarm.requestCodeActiveAlarm = -1
        storedAlarm.pattern = noRepeatPattern.copyOf(noRepeatPattern.size)
        storedAlarm.bedtimeTimestamp = binding.slpClkSetTime.hoursToBedTime.toLong() * 60L * 60L * 1000L + binding.slpClkSetTime.minutesToBedTime.toLong() * 60L * 1000L
        storedAlarm.alarmTimestamp = binding.slpClkSetTime.hoursToAlarm.toLong() * 60L * 60L * 1000L + binding.slpClkSetTime.minutesToAlarm.toLong() * 60L * 1000L
        storedAlarm.alarmToneTypeId = when {
            binding.chpRingtone.isChecked -> AlarmToneType.RINGTONE.ordinal
            binding.chpCustomFile.isChecked -> AlarmToneType.CUSTOM_FILE.ordinal
            binding.chpBinauralBeats.isChecked -> AlarmToneType.BINAURAL_BEAT.ordinal
            else -> -1
        }
        storedAlarm.alarmUri = ringtoneUri.toString()
        storedAlarm.alarmVolume = binding.sldAlarmVolume.value
        storedAlarm.alarmVolumeIncreaseTimestamp = currentVolIncMin.toLong() * 60L * 1000L + currentVolIncSec.toLong() * 1000L
        storedAlarm.isFlashlightActive = binding.swtAlarmUseFlashlight.isChecked
        storedAlarm.isVibrationActive = binding.swtVibrateAlarm.isChecked
        storedAlarm.title = ""
    }

    private fun setTimeChangedListeners() {
        // Set bed time text and handler
        binding.txtTimeBedtime.text = String.format(Locale.ENGLISH, "%02d:%02d", binding.slpClkSetTime.hoursToBedTime, binding.slpClkSetTime.minutesToBedTime)
        binding.slpClkSetTime.setOnBedtimeChangedListener { hours, minutes ->
            storedAlarm.bedtimeTimestamp = hours.toLong() * 60L * 60L * 1000L + minutes.toLong() * 60L * 1000L
            binding.txtTimeBedtime.text = String.format(Locale.ENGLISH, "%02d:%02d", hours, minutes)
        }

        // Set alarm time text and handler
        binding.txtTimeAlarm.text = String.format(Locale.ENGLISH, "%02d:%02d", binding.slpClkSetTime.hoursToAlarm, binding.slpClkSetTime.minutesToAlarm)
        binding.slpClkSetTime.setOnAlarmTimeChangedListener { hours, minutes ->
            storedAlarm.alarmTimestamp = hours.toLong() * 60L * 60L * 1000L + minutes.toLong() * 60L * 1000L
            binding.txtTimeAlarm.text = String.format(Locale.ENGLISH, "%02d:%02d", hours, minutes)
        }
    }

    private fun setEditValuesFromItem() {
        binding.txtAlarmName.setText(storedAlarm.title)

        // Select the stored alarm tone type
        binding.chpRingtone.isChecked = storedAlarm.alarmToneTypeId == AlarmToneType.RINGTONE.ordinal
        binding.chpBinauralBeats.isChecked = storedAlarm.alarmToneTypeId == AlarmToneType.BINAURAL_BEAT.ordinal
        binding.chpCustomFile.isChecked = storedAlarm.alarmToneTypeId == AlarmToneType.CUSTOM_FILE.ordinal

        // Set the stored alarm uri and update the displayed selected text
        ringtoneUri = storedAlarm.alarmUri.toUri()
        when (AlarmToneType.entries[storedAlarm.alarmToneTypeId]) {
            AlarmToneType.RINGTONE -> binding.txtToneSelected.text = RingtoneManager.getRingtone(this, ringtoneUri).getTitle(this)
            AlarmToneType.CUSTOM_FILE -> binding.txtToneSelected.text = File(ringtoneUri.path!!).nameWithoutExtension
            AlarmToneType.BINAURAL_BEAT -> { }
        }

        // Set alarm volume info
        binding.sldAlarmVolume.value = storedAlarm.alarmVolume
        binding.txtCurrAlarmVolume.text = String.format(Locale.ENGLISH, "%d%%", (binding.sldAlarmVolume.value * 100).roundToInt())

        // Set volume increment info
        currentVolIncMin = TimeUnit.MILLISECONDS.toMinutes(storedAlarm.alarmVolumeIncreaseTimestamp).toInt()
        currentVolIncSec = (TimeUnit.MILLISECONDS.toSeconds(storedAlarm.alarmVolumeIncreaseTimestamp) - TimeUnit.MINUTES.toSeconds(currentVolIncMin.toLong())).toInt()
        binding.txtIncVolumeFor.text = String.format(Locale.ENGLISH, "%dm %ds", currentVolIncMin, currentVolIncSec)

        // Set switch states for vibration and flashlight usage
        binding.swtAlarmUseFlashlight.isChecked = storedAlarm.isFlashlightActive
        binding.swtVibrateAlarm.isChecked = storedAlarm.isVibrationActive

        // Check weekday repeat chips based on stored pattern
        for (i in storedAlarm.pattern.indices) {
            weekdayChips[i].isChecked = storedAlarm.pattern[i]
        }

        updateAlarmRepeatText()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
            binding.chpRingtone.isChecked = true
        }
    }

    private fun updateAlarmRepeatText() {
        val pattern = currentRepeatPattern
        binding.txtRepeatPatternText.text = if (pattern.contentEquals(everydayRepeatPattern)) {
            "Repeat every day"
        } else if (pattern.contentEquals(allWeekdaysRepeatPattern)) {
            "Repeat every weekday"
        } else if (pattern.contentEquals(allWeekendsRepeatPattern)) {
            "Repeat every weekend"
        } else if (pattern.contentEquals(noRepeatPattern)) {
            "Repeat only once"
        } else {
            val activeWeekdays = ArrayList<String>()
            for (i in pattern.indices) {
                if (pattern[i]) {
                    activeWeekdays.add(weekdayShorts[i])
                }
            }
            String.format(Locale.ENGLISH, "Repeat every %s", activeWeekdays.joinToString(", "))
        }
    }

    private val currentRepeatPattern: BooleanArray
        get() = weekdayChips.map { it.isChecked }.toBooleanArray()
}