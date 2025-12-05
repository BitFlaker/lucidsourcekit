package com.bitflaker.lucidsourcekit.main.binauralbeats

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bitflaker.lucidsourcekit.databinding.EntryBinauralBinding
import com.bitflaker.lucidsourcekit.main.binauralbeats.RecyclerViewAdapterBinauralBeatsSelector.MainViewHolderBinauralBeats
import java.util.Locale

class RecyclerViewAdapterBinauralBeatsSelector(
    private val context: Context?,
    private val binauralBeats: MutableList<BinauralBeat>
) : RecyclerView.Adapter<MainViewHolderBinauralBeats>() {
    class MainViewHolderBinauralBeats(var binding: EntryBinauralBinding) : RecyclerView.ViewHolder(binding.root)

    var onEntryClickedListener: ((BinauralBeat, Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolderBinauralBeats {
        val binding = EntryBinauralBinding.inflate(LayoutInflater.from(context), parent, false)
        binding.lgBinauralGradient.setDrawProgressIndicator(false)
        return MainViewHolderBinauralBeats(binding)
    }

    override fun onBindViewHolder(holder: MainViewHolderBinauralBeats, position: Int) {
        val current = binauralBeats[position]
        holder.binding.txtBinauralTitle.text = current.title
        holder.binding.txtBinauralDescription.text = current.description
        holder.binding.txtBinauralBeatsBaseFrequency.text = getBaseFrequencyString(current)

        // Get minute, seconds and hour parts
        val seconds = current.frequencyList.duration.toInt()
        val sec = seconds % 60
        val min = (seconds / 60) % 60
        val hours = (seconds / 60) / 60

        // Convert minute, second and hour parts to string
        val secS = String.format(Locale.ENGLISH, "%02d", sec)
        val minS = String.format(Locale.ENGLISH, "%02d", min)
        val hoursS = String.format(Locale.ENGLISH, "%02d", hours)

        // Set the duration text to the calculated values
        holder.binding.txtBinauralBeatsDuration.text = if (hours > 0) "$hoursS:" else "$minS:$secS" // TODO: Check if there should be a minS:secS appended to hourS

        // Configure the binaural beat line chart gradient
        holder.binding.lgBinauralGradient.setData(
            current.frequencyList,
            32f,
            3f,
            0f,
            true,
            Brainwaves.stageColors,
            Brainwaves.stageFrequencyCenters
        )

        // Set beat selection listener
        holder.binding.crdBinauralSelectionCard.setOnClickListener {
            onEntryClickedListener?.invoke(current, position)
        }
    }

    fun getBaseFrequencyString(binauralBeat: BinauralBeat): String {
        if (binauralBeat.baseFrequency == binauralBeat.baseFrequency.toInt().toFloat()) {
            return String.format(Locale.getDefault(), "%d Hz", binauralBeat.baseFrequency.toInt())
        }
        return String.format("%s Hz", binauralBeat.baseFrequency)
    }

    override fun getItemCount(): Int {
        return binauralBeats.size
    }
}
