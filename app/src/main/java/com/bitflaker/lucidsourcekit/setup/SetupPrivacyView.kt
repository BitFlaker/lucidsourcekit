package com.bitflaker.lucidsourcekit.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.databinding.FragmentSetupPrivacyBinding
import com.bitflaker.lucidsourcekit.main.AuthTypes

class SetupPrivacyView : Fragment() {
    private lateinit var binding: FragmentSetupPrivacyBinding
    var onAuthTypeChangedListener: ((AuthTypes) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSetupPrivacyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.spnrLockType.setPopupBackgroundResource(R.drawable.popup_menu_background_dark)
        binding.spnrLockType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) { }
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) {
                when (AuthTypes.entries[i]) {
                    AuthTypes.Password, AuthTypes.Pin -> {
                        binding.chkUseBiometrics.visibility = View.VISIBLE
                        binding.chkUseBiometrics.isEnabled = true
                    }
                    AuthTypes.None -> {
                        binding.chkUseBiometrics.visibility = View.INVISIBLE
                        binding.chkUseBiometrics.isEnabled = false
                    }
                }
                onAuthTypeChangedListener?.invoke(AuthTypes.entries[i])
            }
        }
        binding.chkUseBiometrics.setOnCheckedChangeListener { _, checked ->
            binding.chkUseBiometrics.setCompoundDrawablesRelativeWithIntrinsicBounds(
                if (checked) R.drawable.ic_baseline_check_box_24 else R.drawable.ic_baseline_check_box_outline_blank_24,
                0,
                0,
                0
            )
        }
    }

    fun onLanguageUpdated() {
        binding.spnrLockType.adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.lock_types,
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.txtPrivacyTitle.text = requireContext().resources.getString(R.string.setup_security_title)
        binding.txtPrivacyDescription.text = requireContext().resources.getString(R.string.setup_security_description)
        binding.chkUseBiometrics.setText(R.string.setup_privacy_biometrics)
        binding.spnrLockType.setSelection(binding.spnrLockType.selectedItemPosition, false)
    }

    val useBiometrics: Boolean
        get() = binding.chkUseBiometrics.isChecked
}