package com.bitflaker.lucidsourcekit.utils.export

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.res.ResourcesCompat
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.databinding.ItemPopupEntryBinding

class ExportTypeAdapter(context: Context) : ArrayAdapter<ExportTypes>(context, R.layout.item_popup_entry, ExportTypes.entries.toTypedArray()) {
    private val layoutInflater: LayoutInflater = LayoutInflater.from(context)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding = getBinding(convertView, parent)
        getItem(position)?.let { setEntry(binding, it) }
        return binding.root
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding = getBinding(convertView, parent)
        getItem(position)?.let { setEntry(binding, it) }
        return binding.root
    }

    private fun getBinding(convertView: View?, parent: ViewGroup): ItemPopupEntryBinding {
        return if (convertView != null)
            ItemPopupEntryBinding.bind(convertView)
        else
            ItemPopupEntryBinding.inflate(layoutInflater, parent, false)
    }

    private fun setEntry(binding: ItemPopupEntryBinding, value: ExportTypes) {
        when (value) {
            ExportTypes.PrintAndPDF -> {
                binding.txtText.text = "Print / PDF"
                binding.txtText.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(context.resources, R.drawable.rounded_print_24, context.theme), null, null, null)
            }
            ExportTypes.HTML -> {
                binding.txtText.text = "HTML"
                binding.txtText.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(context.resources, R.drawable.rounded_html_24, context.theme), null, null, null)
            }
            ExportTypes.Markdown -> {
                binding.txtText.text = "Markdown"
                binding.txtText.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(context.resources, R.drawable.rounded_markdown_24, context.theme), null, null, null)
            }
        }
    }
}