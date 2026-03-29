package com.bitflaker.lucidsourcekit.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.databinding.FragmentSetupAuthDataBinding
import com.bitflaker.lucidsourcekit.main.AuthTypes
import com.bitflaker.lucidsourcekit.main.notification.visual.KeypadButtonModel
import com.bitflaker.lucidsourcekit.main.notification.visual.views.KeypadAdapter

class SetupAuthDataView : Fragment() {
    private lateinit var binding: FragmentSetupAuthDataBinding
    private var selectedAuthType = AuthTypes.Pin

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSetupAuthDataBinding.inflate(inflater, container, false)
        showCredentialsSetup(selectedAuthType)
        return binding.root
    }

    fun onLanguageUpdated() {
        if (!::binding.isInitialized) return

        binding.txtSetupPassword.hint = requireContext().resources.getString(R.string.login_password_hint)
        showCredentialsSetup(selectedAuthType)
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

        binding.txtAuthDataTitle.text = requireContext().resources.getString(R.string.setup_auth_data_title_password)
        binding.txtAuthDataDescription.setText(R.string.setup_privacy_password)
        binding.txtSetupPassword.setText("")
        binding.txtSetupPassword.visibility = View.VISIBLE
        binding.llSetupPinLayout.visibility = View.GONE
    }

    fun showFinishSetup() {
        if (!::binding.isInitialized) return

        binding.txtAuthDataTitle.text = requireContext().resources.getString(R.string.setup_finish_setup_title)
        binding.txtAuthDataDescription.setText(R.string.setup_finish_content)
        binding.llSetupPinLayout.visibility = View.GONE
        binding.txtSetupPassword.visibility = View.GONE
    }

    fun showPinSetup() {
        if (!::binding.isInitialized) return

        binding.txtAuthDataTitle.setText(R.string.setup_auth_data_title_pin)
        binding.txtAuthDataDescription.setText(R.string.setup_privacy_pin)
        binding.llSetupPinLayout.visibility = View.VISIBLE
        binding.txtSetupPassword.visibility = View.GONE
        setupPinAuthentication()
    }

    private fun setupPinAuthentication() {
        pin = ""
        binding.txtSetupEnteredPin.text = ""
        val context = requireContext()

        // Generate the keypad
        val buttons = arrayOf(
            '1', '2', '3',
            '4', '5', '6',
            '7', '8', '9',
            null, '0', '#'
        ).map(::KeypadButtonModel).toList()

        // Set the icon for delete
        buttons[buttons.size - 1].buttonIcon = ResourcesCompat.getDrawable(
            context.resources,
            R.drawable.rounded_backspace_24,
            context.theme
        )

        // Configure keypad adapter
        binding.rcvKeypad.layoutManager = GridLayoutManager(context, 3)
        binding.rcvKeypad.adapter = KeypadAdapter(context, buttons).apply {
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
                        binding.txtSetupEnteredPin.text = ""
                        Toast.makeText(context, "PIN too long", Toast.LENGTH_SHORT).show()
                    }
                }
                binding.txtSetupEnteredPin.text = "•".repeat(pin.length)
            }
        }
    }

    var pin: String = ""
        private set

    val password: String
        get() = binding.txtSetupPassword.text.toString()
}