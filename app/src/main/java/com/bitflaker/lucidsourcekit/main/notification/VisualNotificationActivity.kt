package com.bitflaker.lucidsourcekit.main.notification

import android.graphics.Color
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.bitflaker.lucidsourcekit.databinding.ActivityVisualNotificationBinding

class VisualNotificationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVisualNotificationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityVisualNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val color = Color.parseColor("#43474E")
        binding.gcCircle.setMaskColor(Color.BLACK)
        binding.gcCircle.setColor(color)
        binding.gcCircle.setAnimationDuration(1000)
        binding.gcCircle.setStops(0.6f, 0.005f)
        binding.gcCircle.startAnimation(1400)

        binding.gsStripe.startAnimations(256)
    }
}