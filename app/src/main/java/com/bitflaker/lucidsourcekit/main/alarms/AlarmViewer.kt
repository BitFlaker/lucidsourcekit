package com.bitflaker.lucidsourcekit.main.alarms

import android.animation.ValueAnimator
import android.app.AlarmManager
import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bitflaker.lucidsourcekit.MainActivity
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.database.MainDatabase
import com.bitflaker.lucidsourcekit.database.alarms.updated.entities.StoredAlarm
import com.bitflaker.lucidsourcekit.databinding.ActivityAlarmViewerBinding
import com.bitflaker.lucidsourcekit.utils.Tools
import com.bitflaker.lucidsourcekit.utils.resolveDrawable
import com.bitflaker.lucidsourcekit.utils.getDefaultVibrator
import com.bitflaker.lucidsourcekit.utils.onBackPressed
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Calendar
import java.util.Timer
import kotlin.Exception
import kotlin.Float
import kotlin.String
import androidx.core.net.toUri
import com.bitflaker.lucidsourcekit.utils.createTimerAction
import com.bitflaker.lucidsourcekit.utils.vibrateFor

class AlarmViewer : AppCompatActivity() {
    private lateinit var binding: ActivityAlarmViewerBinding
    private lateinit var alarmManager: AlarmManager
    private var mediaPlayer = MediaPlayer()
    private lateinit var vib: Vibrator
    private lateinit var storedAlarm: StoredAlarm
    private var isSnoozing = false
    private var isFlashlightOn = false
    private val alarmStopByMode = AlarmStopByMode.SWIPE
    private val dateFormat: DateFormat = DateFormat.getDateInstance(DateFormat.FULL)
    private val timeFormat: DateFormat = DateFormat.getTimeInstance(DateFormat.SHORT)
    private var vibrationTimer: Timer? = null
    private var flashlightTimer: Timer? = null
    private var cameraId: String? = null
    private var valueIncreaser: ValueAnimator? = null
    private var camManager: CameraManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlarmViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        vib = getDefaultVibrator()

        // Configure activity to wake the phone and display on top of the lockscreen in case it is
        // locked and keep the screen on while the activity is running
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        Tools.makeStatusBarTransparent(this)

        // Set the icons for the alarm action slider
        binding.ossAlarmSlider.setData(resolveDrawable(R.drawable.ic_baseline_check_24), resolveDrawable(R.drawable.ic_baseline_snooze_24))

        // Get the id of the stored alarm
        val id = intent.getLongExtra("STORED_ALARM_ID", -1)
        if (id == -1L) {
            finish()
        }

        // Load the stored alarm and configure the title, alarm sound, vibration and flashlight
        lifecycleScope.launch(Dispatchers.IO) {
            storedAlarm = MainDatabase.getInstance(this@AlarmViewer).storedAlarmDao.getById(id)

            runOnUiThread {
                binding.txtAlarmName.text = storedAlarm.title

                // Try to play the alarm sound
                tryPlayAlarmSound()

                // Check if the vibration pattern is enabled and if so start playing it
                if (storedAlarm.isVibrationActive) {
                    vibrationTimer = createTimerAction(0, 1500) {
                        vib.vibrateFor(1200)
                    }
                }

                // Check if the flashlight pattern is enabled and the flashlight is available and if so, start playing it
                if (storedAlarm.isFlashlightActive && packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                    tryPlayFlashlightPattern()
                }
            }
        }

        // Set alarm stop type (whether to stop the alarm by swiping or button click)
        if (alarmStopByMode == AlarmStopByMode.BUTTON) {
            binding.llAlarmStopButtonContainer.visibility = View.VISIBLE
            binding.crdAlarmSliderContainer.visibility = View.GONE
            binding.btnSnooze.setOnClickListener { snoozeAlarmSelected() }
            binding.btnStopAlarm.setOnClickListener {
                stopAlarm()
                binding.llAlarmStopButtonContainer.visibility = View.GONE
                showActions()
            }
        } else if (alarmStopByMode == AlarmStopByMode.SWIPE) {
            binding.llAlarmStopButtonContainer.visibility = View.GONE
            binding.crdAlarmSliderContainer.visibility = View.VISIBLE
            binding.ossAlarmSlider.setOnLeftSideSelectedListener { stopAlarm() }
            binding.ossAlarmSlider.setOnRightSideSelectedListener { snoozeAlarmSelected() }
            binding.ossAlarmSlider.setOnFadedAwayListener {
                binding.crdAlarmSliderContainer.visibility = View.GONE
                showActions()
            }
        }

        // Ensure the post alarm stop actions are hidden
        binding.llQuickAccessActions.visibility = View.GONE
        binding.llQuickAccessActions.setAlpha(0.0f)

        // Configure click listeners for post alarm stop action buttons
        binding.btnOpenBinauralPlayer.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).apply {
                putExtra("INITIAL_PAGE", "binaural")
                setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            finishAffinity()
        }
        binding.btnOpenJournal.setOnClickListener {
            MaterialAlertDialogBuilder(this, R.style.Theme_LucidSourceKit_ThemedDialog)
                .setTitle("Journal type")
                .setView(JournalTypeDialog.generateContent(this, ::finishAffinity))
                .show()
        }
        binding.btnOpenApp.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).apply {
                setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            finishAffinity()
        }

        // Configure close button
        binding.btnCloseViewer.setOnClickListener { finishAndRemoveTask() }

        // Set the currently displayed time
        val cal = Calendar.getInstance()
        binding.txtCurrentTime.text = timeFormat.format(cal.time)
        binding.txtCurrentDate.text = dateFormat.format(cal.time)

        // Create timer to update displayed time after every minute change
        cal.set(Calendar.MINUTE, cal.get(Calendar.MINUTE) + 1)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        createTimerAction(cal.timeInMillis - Calendar.getInstance().timeInMillis, 60000L) {
            val currentTime = Calendar.getInstance().time
            runOnUiThread {
                binding.txtCurrentTime.text = timeFormat.format(currentTime)
                binding.txtCurrentDate.text = dateFormat.format(currentTime)
            }
        }

        onBackPressed {
            resetAlarm()
            finishAndRemoveTask()
            finish()
        }
    }

    private fun tryPlayFlashlightPattern() {
        camManager = getSystemService(CAMERA_SERVICE) as CameraManager
        camManager?.let { manager ->
            try {
                // Try to get the first available flashlight
                val camera = manager.cameraIdList.firstOrNull {
                    manager.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                } ?: return
                cameraId = camera

                // Schedule the pattern for the flashlight
                manager.setTorchMode(camera, true)
                flashlightTimer = createTimerAction(0, 500) {
                    manager.setTorchMode(camera, isFlashlightOn)
                    isFlashlightOn = !isFlashlightOn
                }
            } catch (e: Exception) {
                Log.e("ALARM_VIEWER", "Failed to register and start flashlight pattern: ${e.message}")
            }
        }
    }

    private fun tryPlayAlarmSound() {
        try {
            mediaPlayer.setAudioAttributes(AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build())
            mediaPlayer.isLooping = true
            mediaPlayer.setDataSource(applicationContext, storedAlarm.alarmUri.toUri())
            mediaPlayer.prepare()
            mediaPlayer.setVolume(storedAlarm.alarmVolume, storedAlarm.alarmVolume)

            // Create value animator to slowly increase the volume if defined
            if (storedAlarm.alarmVolumeIncreaseTimestamp != 0L) {
                valueIncreaser = ValueAnimator.ofFloat(0f, storedAlarm.alarmVolume).apply {
                    duration = storedAlarm.alarmVolumeIncreaseTimestamp
                    interpolator = LinearInterpolator()
                    addUpdateListener { animator ->
                        val current = animator.animatedValue as Float
                        mediaPlayer.setVolume(current, current)
                    }
                }
                valueIncreaser?.start()
            }
            mediaPlayer.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showActions() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (isSnoozing) {
            finish()
            return
        }

        // Show action button container and fade them in
        binding.llQuickAccessActions.visibility = View.VISIBLE
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            interpolator = LinearInterpolator()
            addUpdateListener { valueAnimator ->
                binding.llQuickAccessActions.setAlpha(valueAnimator.animatedValue as Float)
            }
        }.start()
    }

    private fun snoozeAlarmSelected() {
        isSnoozing = true
        resetAlarm()

        // Create snooze alarm trigger and and set exact alarm
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("ALARM_TYPE", "SNOOZING_ALARM")
            putExtra("STORED_ALARM_ID", storedAlarm.alarmId)
        }
        val alarmIntent = PendingIntent.getBroadcast(
            applicationContext,
            AlarmHandler.SNOOZING_ALARM_REQUEST_CODE_START_VALUE + storedAlarm.alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + (5 * 60000),
            alarmIntent
        )

        finish()
    }

    private fun stopAlarm() {
        isSnoozing = false
        resetAlarm()
    }

    private fun resetAlarm() {
        mediaPlayer.stop()
        vibrationTimer?.cancel()
        flashlightTimer?.cancel()
        if (valueIncreaser?.isRunning == true) {
            valueIncreaser?.cancel()
        }
        cameraId?.let {
            try {
                camManager?.setTorchMode(it, false)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    enum class AlarmStopByMode {
        SWIPE,
        BUTTON
    }
}