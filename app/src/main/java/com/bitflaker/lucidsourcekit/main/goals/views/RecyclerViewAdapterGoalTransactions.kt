package com.bitflaker.lucidsourcekit.main.goals.views

import android.app.TimePickerDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.database.MainDatabase
import com.bitflaker.lucidsourcekit.database.goals.entities.ShuffleTransaction
import com.bitflaker.lucidsourcekit.databinding.EntryGoalTransactionBinding
import com.bitflaker.lucidsourcekit.main.goals.views.RecyclerViewAdapterGoalTransactions.MainViewHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Calendar
import java.util.Locale

class RecyclerViewAdapterGoalTransactions(
    private val fragment: Fragment,
    private val shuffleTransactions: MutableList<ShuffleTransaction>
) : RecyclerView.Adapter<MainViewHolder>() {
    class MainViewHolder(var binding: EntryGoalTransactionBinding) : RecyclerView.ViewHolder(binding.root)

    private val context = fragment.requireContext()
    private val db = MainDatabase.getInstance(context)
    private val formatter: DateFormat = DateFormat.getTimeInstance(DateFormat.SHORT)
    var onDeletedListener: ((ShuffleTransaction) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
        val binding = EntryGoalTransactionBinding.inflate(LayoutInflater.from(context), parent, false)
        return MainViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
        val currentTransaction = shuffleTransactions[position]
        val calendar = Calendar.getInstance()

        calendar.timeInMillis = currentTransaction.achievedAt
        holder.binding.txtTransactionCount.text = String.format(Locale.getDefault(), "%d", position + 1)
        holder.binding.btnGoalAchievedTime.text = formatter.format(calendar.time)

        // Set handler for changing the achieved timestamp
        holder.binding.btnGoalAchievedTime.setOnClickListener {
            TimePickerDialog(context, R.style.Theme_LucidSourceKit_TimePickerDialog, { _, hourFrom, minuteFrom ->
                val tempCalendar = Calendar.getInstance()
                tempCalendar.timeInMillis = calendar.timeInMillis
                tempCalendar.set(Calendar.HOUR_OF_DAY, hourFrom)
                tempCalendar.set(Calendar.MINUTE, minuteFrom)
                if (Calendar.getInstance().timeInMillis + 120_000 < tempCalendar.timeInMillis) {
                    Toast.makeText(context, "Time cannot be in the future", Toast.LENGTH_SHORT).show()
                    return@TimePickerDialog
                }
                calendar.timeInMillis = tempCalendar.timeInMillis
                fragment.lifecycleScope.launch(Dispatchers.IO) {
                        currentTransaction.achievedAt = calendar.timeInMillis
                        db.shuffleTransactionDao.update(currentTransaction)

                        fragment.requireActivity().runOnUiThread {
                            holder.binding.btnGoalAchievedTime.text = formatter.format(calendar.time)
                        }
                    }
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        }

        // Set the handler for deleting an achieved entry
        holder.binding.btnDeleteGoalTransaction.setOnClickListener {
            fragment.lifecycleScope.launch(Dispatchers.IO) {
                db.getShuffleTransactionDao().delete(currentTransaction)
                shuffleTransactions.remove(currentTransaction)

                fragment.requireActivity().runOnUiThread {
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(0, itemCount)
                    onDeletedListener?.invoke(currentTransaction)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return shuffleTransactions.size
    }
}
