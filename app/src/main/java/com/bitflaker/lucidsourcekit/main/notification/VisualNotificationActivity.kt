package com.bitflaker.lucidsourcekit.main.notification

import android.app.KeyguardManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.bitflaker.lucidsourcekit.databinding.ActivityVisualNotificationBinding
import com.bitflaker.lucidsourcekit.utils.Tools

class VisualNotificationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVisualNotificationBinding
    private val circleColor = Color.parseColor("#43474E")
    private val presetMaskCharacters = charArrayOf('⁈', '⁜', '#', 'ᚏ', '⁂', '‽', '⁕', '∞', 'ᚔ')
    private val stripeAnimationDelay: Long = 256
    private val circleAnimationDelay: Long = 1152

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityVisualNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        Tools.makeStatusBarTransparent(this)

        // Configure gradient circle
        binding.gcCircle.setMaskColor(Color.BLACK)
        binding.gcCircle.setColor(circleColor)
        binding.gcCircle.setAnimationDuration(1000)
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
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}