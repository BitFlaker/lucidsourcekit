package com.bitflaker.lucidsourcekit.main.notification.visual

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bitflaker.lucidsourcekit.databinding.EntryKeypadButtonBinding

class KeypadAdapter(
    val context: Context,
    private val items: List<KeypadButtonModel>
) : RecyclerView.Adapter<KeypadAdapter.MainViewHolder>() {
    var onButtonClick: ((Char) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
        val binding = EntryKeypadButtonBinding.inflate(LayoutInflater.from(context), parent, false)
        return MainViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
        val item = items[position]
        val value = item.buttonValue
        holder.binding.btnKeypad.isEnabled = value != null
        if (item.buttonIcon != null) {
            holder.binding.btnKeypad.text = ""
            holder.binding.btnKeypad.icon = item.buttonIcon
        }
        else {
            holder.binding.btnKeypad.text = value?.toString() ?: ""
            holder.binding.btnKeypad.icon = null
        }
        holder.binding.btnKeypad.setOnClickListener {
            if (value != null) {
                onButtonClick?.invoke(value)
            }
        }
    }

    class MainViewHolder(val binding: EntryKeypadButtonBinding) : ViewHolder(binding.root)
}