package com.bitflaker.lucidsourcekit.setup.views

import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.databinding.ActivityAuthenticationBinding
import com.bitflaker.lucidsourcekit.datastore.DataStoreKeys
import com.bitflaker.lucidsourcekit.datastore.getSetting
import com.bitflaker.lucidsourcekit.datastore.updateSetting
import com.bitflaker.lucidsourcekit.main.AuthTypes
import com.bitflaker.lucidsourcekit.main.notification.visual.KeypadButtonModel
import com.bitflaker.lucidsourcekit.main.notification.visual.views.KeypadAdapter
import com.bitflaker.lucidsourcekit.utils.Crypt
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AuthenticationActivity : AppCompatActivity() {
    lateinit var binding: ActivityAuthenticationBinding
    private var selectedAuthType = AuthTypes.None
    private var useBiometrics: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAuthenticationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        showCredentialsSetup(selectedAuthType)
        lifecycleScope.launch(Dispatchers.IO) {
            useBiometrics = getSetting(DataStoreKeys.AUTHENTICATION_USE_BIOMETRICS)
            selectedAuthType = when (getSetting(DataStoreKeys.AUTHENTICATION_TYPE)) {
                "password" -> AuthTypes.Password
                "pin" -> AuthTypes.Pin
                else -> AuthTypes.None
            }
            runOnUiThread {
                binding.chkUseBiometrics.isChecked = useBiometrics
                showCredentialsSetup(selectedAuthType)
            }
        }

        binding.spnrLockType.setPopupBackgroundResource(R.drawable.popup_menu_background_dark)
        binding.spnrLockType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) { }
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) {
                binding.chkUseBiometrics.isEnabled = when (AuthTypes.entries[i]) {
                    AuthTypes.Password, AuthTypes.Pin -> true
                    else -> false
                }
                showCredentialsSetup(AuthTypes.entries[i])
            }
        }

        binding.btnBackAuthentication.setOnClickListener {
            MaterialAlertDialogBuilder(this, R.style.Theme_LucidSourceKit_ThemedDialog)
                .setTitle("Discard changes")
                .setMessage("Do you really want to discard all changes")
                .setPositiveButton(getResources().getString(R.string.yes)) { _, _ -> finish() }
                .setNegativeButton(getResources().getString(R.string.no), null)
                .show()
        }

        binding.btnSave.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                when (selectedAuthType) {
                    AuthTypes.Password -> storePassword()
                    AuthTypes.Pin -> storePIN()
                    AuthTypes.None -> storeWithoutAuthentication()
                }
                finish()
            }
        }
    }

    private suspend fun storeWithoutAuthentication() {
        updateSetting(DataStoreKeys.AUTHENTICATION_TYPE, "none")
    }

    private suspend fun storePIN() {
        if (pin.isEmpty()) {
            Toast.makeText(this@AuthenticationActivity, getResources().getString(R.string.pin_too_short), Toast.LENGTH_SHORT).show()
            return
        }
        val secretKey = Crypt.generateSecretKey()
        val pinCipher = Crypt.encryptStringBlowfish(pin, secretKey) ?: ""
        updateSetting(DataStoreKeys.AUTHENTICATION_TYPE, "pin")
        updateSetting(DataStoreKeys.AUTHENTICATION_HASH, pinCipher)
        updateSetting(DataStoreKeys.AUTHENTICATION_SALT, Base64.encodeToString(secretKey, Base64.NO_WRAP))
        updateSetting(DataStoreKeys.AUTHENTICATION_USE_BIOMETRICS, binding.chkUseBiometrics.isChecked)
    }

    private suspend fun storePassword() {
        if (password.isEmpty()) {
            Toast.makeText(this@AuthenticationActivity, getResources().getString(R.string.password_too_short), Toast.LENGTH_SHORT).show()
            return
        }
        val salt = Crypt.generateSalt()
        val pwHash = Crypt.encryptString(password, salt)
        updateSetting(DataStoreKeys.AUTHENTICATION_TYPE, "password")
        updateSetting(DataStoreKeys.AUTHENTICATION_HASH, pwHash)
        updateSetting(DataStoreKeys.AUTHENTICATION_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
        updateSetting(DataStoreKeys.AUTHENTICATION_USE_BIOMETRICS, binding.chkUseBiometrics.isChecked)
    }

    fun showCredentialsSetup(authType: AuthTypes) {
        selectedAuthType = authType
        when (selectedAuthType) {
            AuthTypes.Password -> showPasswordSetup()
            AuthTypes.Pin -> showPinSetup()
            AuthTypes.None -> showFinishSetup()
        }
    }

    fun showPasswordSetup() {
        if (!::binding.isInitialized) return

        binding.txtTitle.text = resources.getString(R.string.setup_auth_data_title_password)
        binding.txtDescription.setText(R.string.setup_privacy_password)
        binding.txtPassword.setText("")
        binding.txtPassword.visibility = View.VISIBLE
        binding.llPin.visibility = View.GONE
    }

    fun showFinishSetup() {
        if (!::binding.isInitialized) return

        binding.txtTitle.text = resources.getString(R.string.setup_finish_setup_title)
        binding.txtDescription.setText(R.string.setup_finish_content)
        binding.llPin.visibility = View.GONE
        binding.txtPassword.visibility = View.GONE
    }

    fun showPinSetup() {
        if (!::binding.isInitialized) return

        binding.txtTitle.setText(R.string.setup_auth_data_title_pin)
        binding.txtDescription.setText(R.string.setup_privacy_pin)
        binding.llPin.visibility = View.VISIBLE
        binding.txtPassword.visibility = View.GONE
        setupPinAuthentication()
    }

    private fun setupPinAuthentication() {
        pin = ""
        binding.txtEnteredPin.text = ""

        // Generate the keypad
        val buttons = arrayOf(
            '1', '2', '3',
            '4', '5', '6',
            '7', '8', '9',
            null, '0', '#'
        ).map(::KeypadButtonModel).toList()

        // Set the icon for delete
        buttons[buttons.size - 1].buttonIcon = ResourcesCompat.getDrawable(
            resources,
            R.drawable.rounded_backspace_24,
            theme
        )

        // Configure keypad adapter
        binding.rcvKeypad.layoutManager = GridLayoutManager(this, 3)
        binding.rcvKeypad.adapter = KeypadAdapter(this, buttons).apply {
            onButtonClick = {
                if (it == '#') {
                    // Remove last number
                    if (!pin.isEmpty()) {
                        pin = pin.substring(0, pin.length - 1)
                    }
                } else {
                    pin += it

                    // Reset PIN automatically when entered PIN gets too long
                    if (pin.length > 8) {
                        pin = ""
                        binding.txtEnteredPin.text = ""
                        Toast.makeText(context, "PIN too long", Toast.LENGTH_SHORT).show()
                    }
                }
                binding.txtEnteredPin.text = "•".repeat(pin.length)
            }
        }
    }

    var pin: String = ""
        private set

    val password: String
        get() = binding.txtPassword.text.toString()
}