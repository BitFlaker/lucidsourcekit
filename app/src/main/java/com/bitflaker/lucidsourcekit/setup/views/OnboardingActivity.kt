package com.bitflaker.lucidsourcekit.setup.views

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.AdapterView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.databinding.ActivityOnboardingBinding
import com.bitflaker.lucidsourcekit.datastore.DataStoreKeys
import com.bitflaker.lucidsourcekit.datastore.updateSetting
import com.bitflaker.lucidsourcekit.utils.loadLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OnboardingActivity : AppCompatActivity() {
    lateinit var binding: ActivityOnboardingBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, maxOf(ime.bottom, systemBars.bottom))
            binding.spBarPlaceholder.minimumHeight = systemBars.top
            insets
        }

        // Configure language drop down
        binding.spnrLanguage.setPopupBackgroundResource(R.drawable.popup_menu_background_dark_padded)
        binding.spnrLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) { }
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) {
                val lang = binding.spnrLanguage.selectedItem.toString()
                lifecycleScope.launch(Dispatchers.IO) {
                    updateSetting(DataStoreKeys.LANGUAGE, lang)
                    loadLanguage()

                    // Apply the new language to the current activity
                    runOnUiThread {
                        binding.txtOnboardingTitle.text = resources.getString(R.string.onboarding_title)
                        binding.txtOnboardingDescription.text = resources.getString(R.string.onboarding_description)
                        binding.btnNext.text = resources.getString(R.string.setup_next)
                        binding.chkAccept.text = getAcceptSpan()
                    }
                }
            }
        }

        // Configure accept checkbox
        binding.chkAccept.text = getAcceptSpan()
        binding.chkAccept.movementMethod = LinkMovementMethod.getInstance()
        binding.chkAccept.highlightColor = Color.TRANSPARENT
        binding.chkAccept.setOnCheckedChangeListener { _, _ ->
            binding.btnNext.isEnabled = binding.chkAccept.isChecked
        }

        // Configure "Next" button
        binding.btnNext.isEnabled = false
        binding.btnNext.setOnClickListener {
            startActivity(Intent(this, GettingStartedActivity::class.java))
            finish()
        }
    }

    private fun getAcceptSpan(): SpannableString {
        val readme = "README"
        val privacy = "Privacy Policy"
        val text = resources.getString(R.string.onboarding_consent)
        val spannable = SpannableString(text)
        val readmeStart = text.indexOf(readme)
        val privacyStart = text.indexOf(privacy)

        spannable.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        "https://github.com/BitFlaker/lucidsourcekit/blob/main/README.md".toUri()
                    )
                )
            }
        }, readmeStart, readmeStart + readme.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        "https://bitflaker.github.io/lucidsourcekit/privacy".toUri()
                    )
                )
            }
        }, privacyStart, privacyStart + privacy.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        return spannable
    }
}