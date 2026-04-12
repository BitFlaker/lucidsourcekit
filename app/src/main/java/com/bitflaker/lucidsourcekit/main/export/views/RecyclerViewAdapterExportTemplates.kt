package com.bitflaker.lucidsourcekit.main.export.views

import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bitflaker.lucidsourcekit.databinding.EntryExportTemplateBinding
import com.bitflaker.lucidsourcekit.utils.dpToPx
import com.bitflaker.lucidsourcekit.utils.export.PdfExportTemplate
import com.bitflaker.lucidsourcekit.utils.export.PdfExporter
import kotlinx.coroutines.launch

class RecyclerViewAdapterExportTemplates(
    private val activity: ComponentActivity,
    templates: Array<PdfExportTemplate>
) : RecyclerView.Adapter<RecyclerViewAdapterExportTemplates.MainViewHolder>() {
    class MainViewHolder(var binding: EntryExportTemplateBinding) : RecyclerView.ViewHolder(binding.root)

    val exporter: PdfExporter = PdfExporter(activity)

    init {
        templates.forEach(exporter::addTemplate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
        val binding = EntryExportTemplateBinding.inflate(LayoutInflater.from(activity), parent, false)
        return MainViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
        val isSelected = exporter.selectedTemplateIndex == position
        holder.binding.cpiLoading.visibility = VISIBLE
        holder.binding.txtLoadingText.visibility = VISIBLE
        activity.lifecycleScope.launch {
            holder.binding.imgPreview.setImageBitmap(exporter.getPreviews()[position])
            holder.binding.cpiLoading.visibility = GONE
            holder.binding.txtLoadingText.visibility = GONE
        }
        holder.binding.root.strokeWidth = if (isSelected) 2.dpToPx else 0
        holder.binding.root.setOnClickListener {
            val previous = exporter.selectedTemplateIndex
            exporter.selectedTemplateIndex = position
            notifyItemChanged(previous)
            notifyItemChanged(position)
        }
        holder.binding.crdSelected.visibility = if (isSelected) VISIBLE else GONE
    }

    override fun getItemCount() = exporter.templates.size
}