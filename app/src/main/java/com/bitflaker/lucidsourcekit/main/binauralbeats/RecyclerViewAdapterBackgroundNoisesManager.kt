package com.bitflaker.lucidsourcekit.main.binauralbeats

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.bitflaker.lucidsourcekit.databinding.EntryBinauralBackgroundNoiseBinding
import com.bitflaker.lucidsourcekit.main.binauralbeats.RecyclerViewAdapterBackgroundNoisesManager.MainViewHolderBackgroundNoises

class RecyclerViewAdapterBackgroundNoisesManager(
    private val context: Context,
    private val backgroundNoises: MutableList<BackgroundNoise>
) : RecyclerView.Adapter<MainViewHolderBackgroundNoises>() {
    class MainViewHolderBackgroundNoises(var binding: EntryBinauralBackgroundNoiseBinding) : RecyclerView.ViewHolder(binding.root)

    var onVolumeChangedListener: ((BackgroundNoise, Int) -> Unit)? = null
    var onEntryClickedListener: ((BackgroundNoise, Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolderBackgroundNoises {
        val binding = EntryBinauralBackgroundNoiseBinding.inflate(LayoutInflater.from(context), parent, false)
        return MainViewHolderBackgroundNoises(binding)
    }

    override fun onBindViewHolder(holder: MainViewHolderBackgroundNoises, position: Int) {
        holder.binding.txtBackgroundNoiseName.text = backgroundNoises[position].name
        holder.binding.sldVolumeBackgroundNoise.value = backgroundNoises[position].volume
        holder.binding.imgNoiseIcon.setImageDrawable(ResourcesCompat.getDrawable(context.resources, backgroundNoises[position].icon, context.theme))
        holder.binding.imgNoiseIcon.setOnClickListener {
            // TODO display that it is paused by for example changing slider color to gray?
            backgroundNoises[position].isPaused = !backgroundNoises[position].isPaused
            onEntryClickedListener?.invoke(backgroundNoises[position], position)
        }
        holder.binding.sldVolumeBackgroundNoise.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                backgroundNoises[position].volume = value
                onVolumeChangedListener?.invoke(backgroundNoises[position], position)
            }
        }
    }

    override fun getItemCount(): Int {
        return backgroundNoises.size
    }
}
