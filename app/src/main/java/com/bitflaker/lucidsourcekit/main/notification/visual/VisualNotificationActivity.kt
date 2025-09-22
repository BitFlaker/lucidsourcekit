package com.bitflaker.lucidsourcekit.main.notification.visual

import android.app.KeyguardManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.transition.TransitionManager
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.data.datastore.DataStoreKeys
import com.bitflaker.lucidsourcekit.data.datastore.getSetting
import com.bitflaker.lucidsourcekit.data.datastore.updateSetting
import com.bitflaker.lucidsourcekit.databinding.ActivityVisualNotificationBinding
import com.bitflaker.lucidsourcekit.utils.Tools
import com.bitflaker.lucidsourcekit.utils.fadeIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.TimerTask
import kotlin.random.Random

// TODO: Refactor this code

class VisualNotificationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVisualNotificationBinding
    private val circleColor = Color.parseColor("#43474E")
    private val presetMaskCharacters = charArrayOf('⁈', '⁜', '#', 'ᚏ', '⁂', '‽', '⁕', '∞', 'ᚔ')
    private val stripeAnimationDelay: Long = 256
    private val circleAnimationDelay: Long = 1152
    private val circleAnimationDuration: Long = 1000
    private var activityStartTime = System.currentTimeMillis()
    private val timeToReadText: Long = 3000
    private var discardHandler: DelayedTaskHandler? = null
    private var confirmationTimer: Timer? = Timer()
    private var progressUpdateTimer: Timer? = Timer()
    private val targetConfirmationDigitCount: Int = 2
    private lateinit var currentDigits: ByteArray
    private var isInConfirmationScreen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityVisualNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Request to wake the device and display on lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        lifecycleScope.launch(Dispatchers.IO) {
            currentDigits = getSetting(DataStoreKeys.NOTIFICATION_RC_REMINDER_FULL_SCREEN_CONFIRM_DIGITS)
        }

        Tools.makeStatusBarTransparent(this)

        // Configure gradient circle
        binding.gcCircle.setMaskColor(Color.BLACK)
        binding.gcCircle.setColor(circleColor)
        binding.gcCircle.setAnimationDuration(circleAnimationDuration)
        binding.gcCircle.setStops(0.6f, 0.005f)

        // Configure gradient circle inner text
        binding.gcCircle.setTextColor(Tools.getAttrColor(com.google.android.material.R.attr.colorOutline, theme))
        binding.gcCircle.setTextMaskCharacter(presetMaskCharacters[4])
        binding.gcCircle.setJitterEnabled(true)
        binding.gcCircle.setMaskJitter(48)
        binding.gcCircle.setTextJitter(24)
        binding.gcCircle.setTextSize(Tools.spToPx(this, 20f).toFloat())

        // Start attention grabbing flashing animation
        binding.gsStripe.startAnimations(stripeAnimationDelay)

        // Start gradient circle animation
        binding.gcCircle.startAnimation(stripeAnimationDelay + circleAnimationDelay)

        // Stop the activity automatically after some time
        discardHandler = DelayedTaskHandler({ runOnUiThread { finish() }}, 15000)   // TODO: Make this value adjustable / allow to disable the auto-close functionality
        discardHandler?.start()

        // Configure close button click appearing after displaying next sequence
        binding.btnCloseViewer.setOnClickListener {
            runOnUiThread { finish() }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event != null && !isInConfirmationScreen) {
            if (event.action == MotionEvent.ACTION_DOWN) {
                discardHandler?.pause()
                val openTime = System.currentTimeMillis() - activityStartTime
                val additionalDelay = (stripeAnimationDelay + circleAnimationDelay + circleAnimationDuration - openTime).coerceAtLeast(0L)
                val delay = additionalDelay + timeToReadText
                val startTime = System.currentTimeMillis()

                // Configure timer for progress bar update below
                progressUpdateTimer = Timer()
                progressUpdateTimer?.schedule(object : TimerTask() {
                    override fun run() {
                        val current = System.currentTimeMillis() - startTime
                        binding.piHoldProgress.setProgress(100f * current / delay.toFloat())
                    }
                }, 0L, 4L)

                // Configure timer for showing confirmation screen
                confirmationTimer = Timer()
                confirmationTimer?.schedule(object : TimerTask() {
                    override fun run() {
                        runOnUiThread {
                            showConfirmation()
                        }
                    }
                }, delay)
            }
            else if (event.action == MotionEvent.ACTION_UP) {
                progressUpdateTimer?.cancel()
                confirmationTimer?.cancel()
                discardHandler?.resume()
                binding.piHoldProgress.setProgress(0f)
            }
        }
        return super.onTouchEvent(event)
    }

    private fun showConfirmation() {
        progressUpdateTimer?.cancel()
        discardHandler?.cancel()
        isInConfirmationScreen = true
        binding.gcCircle.reverseAnimation()
        binding.piHoldProgress.visibility = GONE

        lifecycleScope.launch(Dispatchers.IO) {
            // Check if there are any digits yet, otherwise directly generate some
            val generatedToday = getSetting(DataStoreKeys.NOTIFICATION_RC_REMINDER_FULL_SCREEN_CONFIRM_TIME) < Tools.getMidnightTime()
            if (generatedToday || currentDigits.isEmpty()) {
                generateNextDigit(ByteArray(0))
                return@launch
            }

            runOnUiThread {
                // Generate top digit input fields
                val items = List(currentDigits.size) { _ -> ConfirmationFieldModel(null, false) }
                val digitAdapter = RecyclerViewAdapterConfirmationField(this@VisualNotificationActivity, currentDigits, items)
                binding.rcvNumFieldContainer.adapter = digitAdapter
                binding.rcvNumFieldContainer.itemAnimator = null

                // Generate the keypad
                val buttons = listOf(
                    '1', '2', '3',
                    '4', '5', '6',
                    '7', '8', '9',
                    null, '0', '#'
                ).map { KeypadButtonModel(it, null) }

                // Set the icon for confirming
                buttons.last().buttonIcon = ResourcesCompat.getDrawable(resources, R.drawable.rounded_check_24, theme)

                // Build the keypad
                val keypadAdapter = KeypadAdapter(this@VisualNotificationActivity, buttons)
                binding.rcvKeypad.adapter = keypadAdapter
                binding.rcvKeypad.layoutManager = GridLayoutManager(this@VisualNotificationActivity, 3)
                keypadAdapter.onButtonClick = {
                    if (it == '#') {
                        digitAdapter.confirm()
                        generateNextDigit(currentDigits)
                    }
                    else {
                        digitAdapter.setCurrentValue(it)
                        digitAdapter.moveNext()
                    }
                }

                // Fade in the confirmation screen
                Handler(Looper.getMainLooper()).postDelayed({
                    runOnUiThread {
                        binding.rcvNumFieldContainer.fadeIn()
                        binding.rcvKeypad.fadeIn()
                    }
                }, 1000)
            }
        }
    }

    private fun generateNextDigit(digits: ByteArray) {
        lifecycleScope.launch(Dispatchers.IO) {
            val nextDigit = Random.Default.nextInt(0, 10).toByte()
            val newDigits = if (digits.size < targetConfirmationDigitCount) {
                digits + nextDigit
            }
            else {
                for (i in 1..<digits.size) {
                    digits[i - 1] = digits[i]
                }
                digits[digits.lastIndex] = nextDigit
                digits
            }
            updateSetting(DataStoreKeys.NOTIFICATION_RC_REMINDER_FULL_SCREEN_CONFIRM_DIGITS, newDigits)
            updateSetting(DataStoreKeys.NOTIFICATION_RC_REMINDER_FULL_SCREEN_CONFIRM_TIME, System.currentTimeMillis())
            runOnUiThread {
                binding.rcvKeypad.visibility = GONE
                binding.txtNextSequence.text = newDigits.joinToString("")
                binding.llNextSequenceContainer.alpha = 0f
                binding.llNextSequenceContainer.visibility = VISIBLE
                binding.btnCloseViewer.alpha = 0f
                binding.btnCloseViewer.visibility = VISIBLE

                val rootView = binding.main
                val constraintSet = ConstraintSet()
                constraintSet.clone(rootView)
                constraintSet.connect(binding.rcvNumFieldContainer.id, ConstraintSet.BOTTOM, binding.btnCloseViewer.id, ConstraintSet.TOP)
                TransitionManager.beginDelayedTransition(rootView)
                constraintSet.applyTo(rootView)

                // For some reason the keypad does not disappear therefore set adapter
                // to null to force it to be removed (TODO: Fix this workaround)
                binding.rcvKeypad.adapter = null

                // Fade in next sequence after some delay
                Handler(Looper.getMainLooper()).postDelayed({
                    runOnUiThread {
                        binding.llNextSequenceContainer.fadeIn()
                        binding.btnCloseViewer.fadeIn()
                    }
                }, 400)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}