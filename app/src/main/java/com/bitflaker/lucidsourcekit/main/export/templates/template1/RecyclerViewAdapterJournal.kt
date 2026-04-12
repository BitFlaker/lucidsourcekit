package com.bitflaker.lucidsourcekit.main.export.templates.template1

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bitflaker.lucidsourcekit.databinding.EntryPdfExportJournalEntryBinding
import com.bitflaker.lucidsourcekit.main.export.templates.data.JournalEntryExportData

class RecyclerViewAdapterJournal(
    private val context: Context,
    private val entries: List<JournalEntryExportData>
) : RecyclerView.Adapter<RecyclerViewAdapterJournal.MainViewHolder>() {
    class MainViewHolder(var binding: EntryPdfExportJournalEntryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
        val binding = EntryPdfExportJournalEntryBinding.inflate(LayoutInflater.from(context), parent, false)
        return MainViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
        val current = entries[position]
        holder.binding.txtJournalTitle.text = current.title
        holder.binding.txtJournalText.text = current.text
    }

    override fun getItemCount() = entries.size
}