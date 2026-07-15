package com.bitflaker.lucidsourcekit.setup.views

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bitflaker.lucidsourcekit.MainActivity
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.databinding.ActivityGettingStartedBinding
import com.bitflaker.lucidsourcekit.datastore.DataStoreKeys
import com.bitflaker.lucidsourcekit.datastore.updateSetting
import com.bitflaker.lucidsourcekit.main.MainViewer.Companion.backupLoadDialogResult
import com.bitflaker.lucidsourcekit.main.MainViewer.Companion.promptImportBackup
import com.bitflaker.lucidsourcekit.setup.GettingStartedOption
import com.bitflaker.lucidsourcekit.setup.RecyclerViewAdapterGettingStartedOptions
import com.bitflaker.lucidsourcekit.utils.Tools
import com.bitflaker.lucidsourcekit.utils.loadLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GettingStartedActivity : AppCompatActivity() {
    private val backupLoadDialogLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(StartActivityForResult()) { backupLoadDialogResult(this, it) }
    lateinit var binding: ActivityGettingStartedBinding
    var isSettings: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityGettingStartedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, maxOf(ime.bottom, systemBars.bottom))
            binding.spBarPlaceholder.minimumHeight = systemBars.top
            insets
        }

        if (intent.getBooleanExtra("IS_SETTINGS", false)) {
            isSettings = true
            configureLanguageDropDown()
            configureTitleSettings()
        }

        val optionsAdapter = RecyclerViewAdapterGettingStartedOptions(this, !isSettings, listOf(
            GettingStartedOption(
                R.drawable.rounded_key_24,
                "Configure authentication",
                "Protect the app with a password or PIN and biometric authentication",
                false,
                ::onOptionAuthentication
            ),
            GettingStartedOption(
                R.drawable.round_history_edu_24,
                "Import questionnaires",
                "Add questionnaires from a JSON file",
                false,
                ::onOptionImportQuestionnaires
            ),
            GettingStartedOption(
                R.drawable.rounded_settings_b_roll_24,
                "Import settings",
                "Apply settings from a JSON file",
                false,
                ::onOptionImportSettings
            ),
            GettingStartedOption(
                R.drawable.rounded_settings_backup_restore_24,
                "Restore backup",
                "Load a backup ZIP file to restore all journal entries, settings, etc.",
                false,
                ::onOptionRestoreBackup
            )
        ))
        binding.rcvGettingStartedOptions.adapter = optionsAdapter
        binding.rcvGettingStartedOptions.layoutManager = LinearLayoutManager(this)

        binding.btnFinish.setOnClickListener {
            if (!isSettings) {
                val intent = Intent(this, MainActivity::class.java)
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
            }
            finish()
        }
    }

    private fun configureTitleSettings() {
        binding.txtGetStartedTitle.text = resources.getString(R.string.settings)
        binding.txtGetStartedDescription.text = ""
    }

    private fun configureLanguageDropDown() {
        binding.spnrLanguage.isVisible = true
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
                        if (isSettings) {
                            configureTitleSettings()
                        }
                    }
                }
            }
        }
    }

    fun onOptionAuthentication() {
        startActivity(Intent(this, AuthenticationActivity::class.java))
    }

    fun onOptionImportQuestionnaires() {
        Tools.showPlaceholderDialog(this)
    }

    fun onOptionImportSettings() {
        Tools.showPlaceholderDialog(this)
    }

    fun onOptionRestoreBackup() {
        promptImportBackup(this, backupLoadDialogLauncher)
    }
}