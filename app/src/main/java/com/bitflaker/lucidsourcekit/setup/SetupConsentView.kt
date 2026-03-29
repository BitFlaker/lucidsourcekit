package com.bitflaker.lucidsourcekit.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.databinding.FragmentSetupConsentBinding

class SetupConsentView : Fragment() {
    private lateinit var binding: FragmentSetupConsentBinding
    var onConsentChangedListener: ((Boolean) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSetupConsentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.chkAcceptRisk.setOnCheckedChangeListener { _, checked ->
            binding.chkAcceptRisk.setCompoundDrawablesRelativeWithIntrinsicBounds(
                if (checked) R.drawable.ic_baseline_check_box_24 else R.drawable.ic_baseline_check_box_outline_blank_24,
                0,
                0,
                0
            )
            onConsentChangedListener?.invoke(checked)
        }
    }

    fun onLanguageUpdated() {
        if (view != null) {
            binding.txtExperimentalTitle.text = requireContext().resources.getString(R.string.setup_experimental_title)
            binding.txtExperimentalDescription.text = requireContext().resources.getString(R.string.setup_experimental_description)
            binding.chkAcceptRisk.text = requireContext().resources.getString(R.string.setup_consent_checkbox)
        }
    }
}