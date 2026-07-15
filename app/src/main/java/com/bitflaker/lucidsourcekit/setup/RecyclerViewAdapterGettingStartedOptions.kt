package com.bitflaker.lucidsourcekit.setup

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bitflaker.lucidsourcekit.databinding.EntryGettingStartedOptionBinding

class RecyclerViewAdapterGettingStartedOptions(
    private val context: Context,
    private val isCheckingOptions: Boolean,
    private val options: List<GettingStartedOption>
) : RecyclerView.Adapter<RecyclerViewAdapterGettingStartedOptions.MainViewHolder>() {
    class MainViewHolder(var binding: EntryGettingStartedOptionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
        val binding = EntryGettingStartedOptionBinding.inflate(LayoutInflater.from(context), parent, false)
        return MainViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return options.size
    }

    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
        val current = options[position]
        holder.binding.txtTitle.text = current.title
        holder.binding.txtDescription.text = current.description
        holder.binding.imgIcon.setImageResource(current.icon)
        holder.binding.crdDone.isVisible = current.alreadyCompleted
        holder.binding.root.setOnClickListener {
            current.action.invoke()
            if (isCheckingOptions) {
                current.alreadyCompleted = true
                notifyItemChanged(position)
            }
        }
    }
}
