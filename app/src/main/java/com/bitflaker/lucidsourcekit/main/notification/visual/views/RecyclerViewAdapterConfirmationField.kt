package com.bitflaker.lucidsourcekit.main.notification.visual.views

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.databinding.EntryConfirmationDigitBinding
import com.bitflaker.lucidsourcekit.main.notification.visual.ConfirmationFieldModel
import com.bitflaker.lucidsourcekit.utils.attrColorStateList
import java.security.InvalidParameterException

class RecyclerViewAdapterConfirmationField(
    val context: Context,
    private val validSequence: ByteArray,
    private val items: List<ConfirmationFieldModel>
) : RecyclerView.Adapter<RecyclerViewAdapterConfirmationField.MainViewHolder>() {
    private var hasConfirmed = false
    private var selectedIndex = 0

    init {
        if (validSequence.size != items.size) {
            throw InvalidParameterException("Matching the sequence is impossible as the input field count is not the same size as the valid sequence")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
        val binding = EntryConfirmationDigitBinding.inflate(LayoutInflater.from(context), parent, false)
        return MainViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MainViewHolder, @SuppressLint("RecyclerView") position: Int) {    // Suppressing this issue as the item order never changes
        val current = items[position]

        holder.binding.txtConfirmationDigit.text = current.digit?.toString() ?: ""

        // Get stroke color based on current state
        val strokeColor = if (!hasConfirmed && selectedIndex == position) R.attr.colorPrimary
                          else if (!hasConfirmed) R.attr.colorSurfaceContainer
                          else if (current.isValid) R.attr.colorSuccess
                          else R.attr.colorError

        // Set stroke color and click listener
        holder.binding.crdDigitContainer.setStrokeColor(context.attrColorStateList(strokeColor))
        holder.binding.crdDigitContainer.setOnClickListener {
            if (position != selectedIndex) {
                val oldIndex = selectedIndex
                selectedIndex = position
                notifyItemChanged(oldIndex)
                notifyItemChanged(position)
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun confirm() {
        hasConfirmed = true
        for (i in validSequence.indices) {
            items[i].isValid = items[i].digit == validSequence[i]
        }
        notifyItemRangeChanged(0, items.size)
    }

    fun setCurrentValue(value: Char) {
        if (!hasConfirmed && value.isDigit()) {
            items[selectedIndex].digit = value.digitToInt().toByte()
            notifyItemChanged(selectedIndex)
        }
    }

    fun moveNext() {
        val oldIndex = selectedIndex
        selectedIndex = (selectedIndex + 1) % items.size
        notifyItemChanged(oldIndex)
        notifyItemChanged(selectedIndex)
    }

    class MainViewHolder(val binding: EntryConfirmationDigitBinding) : ViewHolder(binding.root)
}