package com.bitflaker.lucidsourcekit.main.about

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bitflaker.lucidsourcekit.databinding.ActivityAboutBinding
import com.bitflaker.lucidsourcekit.utils.Tools
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity

class AboutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnAboutClose.setOnClickListener {
            finish()
        }
        binding.crdUserGuide.setOnClickListener {
            Tools.showPlaceholderDialog(this)
        }
        binding.crdIssues.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/BitFlaker/lucidsourcekit/issues".toUri()))
        }
        binding.crdSourceCode.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/BitFlaker/lucidsourcekit".toUri()))
        }
        binding.btnOssLicenses.setOnClickListener {
            startActivity(Intent(this, OssLicensesMenuActivity::class.java))
        }
        binding.btnPrivacyPolicy.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, "https://bitflaker.github.io/lucidsourcekit/privacy".toUri()))
        }
        binding.btnReadme.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/BitFlaker/lucidsourcekit/blob/main/README.md".toUri()))
        }
    }
}