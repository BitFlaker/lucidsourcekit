package com.bitflaker.lucidsourcekit.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.datastore.DataStoreKeys
import com.bitflaker.lucidsourcekit.datastore.updateSetting
import com.bitflaker.lucidsourcekit.databinding.FragmentSetupLanguageBinding
import com.bitflaker.lucidsourcekit.utils.loadLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SetupLanguageView : Fragment() {
    private lateinit var binding: FragmentSetupLanguageBinding
    var onLanguageChangedListener: (() -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSetupLanguageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.spnrLanguage.setPopupBackgroundResource(R.drawable.popup_menu_background_dark)
        binding.spnrLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) {
                // TODO: Set originally set language and don't overwrite with default selection
                var lang = binding.spnrLanguage.selectedItem.toString()
                    .split("(")
                    .dropLastWhile { it.isEmpty() }
                    .drop(1)
                    .first()
                lang = lang.substring(0, lang.length - 1)
                binding.txtLanguageSupportNotice.isVisible = lang == "de"
                lifecycleScope.launch(Dispatchers.IO) {
                    requireContext().updateSetting(DataStoreKeys.LANGUAGE, lang)
                    requireActivity().loadLanguage()
                    onLanguageChangedListener?.invoke()
                }
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) { }
        }
    }

    fun onLanguageUpdated() {
        if (view != null) {
            binding.welcomeHeader.text = requireContext().resources.getString(R.string.setup_language_title)
            binding.txtSelectLanguage.text = requireContext().resources.getString(R.string.setup_languages_description)
        }
    }
}