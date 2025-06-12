package com.bitflaker.lucidsourcekit.main.questionnaire

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.NumberPicker
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.database.questionnaire.entities.Question
import com.bitflaker.lucidsourcekit.databinding.EntryQuestionRangeEditorBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class RecyclerViewQuestionRange(
    val context: Context,
    val item: Question
): RecyclerView.Adapter<RecyclerViewQuestionRange.MainViewHolder>() {
    class MainViewHolder(val binding: EntryQuestionRangeEditorBinding) : ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
        val binding = EntryQuestionRangeEditorBinding.inflate(LayoutInflater.from(context), parent, false)
        return MainViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return 1
    }

    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
        holder.binding.txtValueFrom.text = item.valueFrom?.toString() ?: "0"
        holder.binding.txtValueTo.text = item.valueTo?.toString() ?: "10"
        holder.binding.crdFrom.setOnClickListener {
            showSelector(item.valueFrom ?: 0, 0)
        }
        holder.binding.crdTo.setOnClickListener {
            showSelector(item.valueTo ?: 0, 1)
        }
    }

    private fun showSelector(defaultValue: Int, rangeIndex: Int) {
        val picker = NumberPicker(context).apply {
            maxValue = if (rangeIndex == 0 && item.valueTo != null) item.valueTo!! else 999
            minValue = if (rangeIndex == 1 && item.valueFrom != null) item.valueFrom!! else 0
            value = defaultValue
        }
        MaterialAlertDialogBuilder(context)
            .setView(picker)
            .setTitle("Select " + if (rangeIndex == 0) "minimum" else "maximum")
            .setPositiveButton(context.resources.getString(R.string.ok)) { _, _ ->
                if (rangeIndex == 0) {
                    if (picker.value > (item.valueTo ?: Int.MAX_VALUE)) return@setPositiveButton
                    item.valueFrom = picker.value
                }
                else {
                    if (picker.value < (item.valueFrom ?: Int.MIN_VALUE)) return@setPositiveButton
                    item.valueTo = picker.value
                }
                notifyItemChanged(0)
            }
            .setNegativeButton(context.resources.getString(R.string.cancel), null)
            .create()
            .show()
    }
}