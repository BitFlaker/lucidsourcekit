package com.bitflaker.lucidsourcekit.setup

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.databinding.FragmentSetupOpenSourceBinding
import androidx.core.net.toUri

class SetupOpenSourceView : Fragment() {
    private lateinit var binding: FragmentSetupOpenSourceBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSetupOpenSourceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.txtOpenSourceDescription.movementMethod = LinkMovementMethod.getInstance()
        binding.btnOpenReadme.setOnClickListener {
            startActivity(Intent(
                Intent.ACTION_VIEW,
                "https://github.com/BitFlaker/lucidsourcekit/blob/main/README.md".toUri()
            ))
        }
        binding.btnPrivacyPolicy.setOnClickListener {
            startActivity(Intent(
                Intent.ACTION_VIEW,
                "https://bitflaker.github.io/lucidsourcekit/privacy".toUri()
            ))
        }
    }

    fun onLanguageUpdated() {
        if (view != null) {
            binding.txtOpenSourceTitle.text = requireContext().resources.getString(R.string.setup_open_source_title)
            binding.txtOpenSourceDescription.text = requireContext().resources.getString(R.string.setup_open_source_description)
        }
    }
}