package com.bitflaker.lucidsourcekit.main.goals.views

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.database.MainDatabase
import com.bitflaker.lucidsourcekit.database.goals.entities.Goal
import com.bitflaker.lucidsourcekit.database.goals.entities.ShuffleTransaction
import com.bitflaker.lucidsourcekit.databinding.EntryCurrentGoalBinding
import com.bitflaker.lucidsourcekit.databinding.SheetGoalTransactionsBinding
import com.bitflaker.lucidsourcekit.utils.attrColorStateList
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class RecyclerViewAdapterCurrentGoals(
    private val fragment: Fragment,
    private val goals: List<Goal>?,
    private val shuffleId: Int
) : RecyclerView.Adapter<RecyclerViewAdapterCurrentGoals.MainViewHolder>() {

    class MainViewHolder(var binding: EntryCurrentGoalBinding) : RecyclerView.ViewHolder(binding.root)

    private lateinit var goalAchievedCount: HashMap<Int, Int>
    private val activity = fragment.requireActivity()
    private val db: MainDatabase = MainDatabase.getInstance(activity)
    private val colorNotAchieved = activity.attrColorStateList(R.attr.colorSurfaceContainer)
    private val colorAchieved = activity.attrColorStateList(R.attr.colorTertiary)

    init {
        // Load goal achieved counts and then update entries
        fragment.lifecycleScope.launch(Dispatchers.IO) {
            goalAchievedCount = db.shuffleTransactionDao.getAllCountsFromShuffle(shuffleId)
                .groupBy { it.goalId }
                .mapValues { it.value.single().count }
                .toMap(HashMap())

            activity.runOnUiThread {
                notifyItemRangeChanged(0, itemCount)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
        val binding = EntryCurrentGoalBinding.inflate(LayoutInflater.from(activity), parent, false)
        return MainViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
        val currentGoal = goals?.get(position) ?: return
        holder.binding.txtGoal.text = currentGoal.description

        val achievedCount = goalAchievedCount.getOrDefault(currentGoal.goalId, 0)
        holder.binding.imgAchievedCounterBackground.setImageTintList(if (achievedCount == 0) colorNotAchieved else colorAchieved)
        holder.binding.txtAchievedCounter.visibility = if (achievedCount == 0) View.GONE else View.VISIBLE
        holder.binding.txtAchievedCounterPlaceholder.visibility = if (achievedCount == 0) View.VISIBLE else View.GONE
        holder.binding.txtAchievedCounter.text = String.format(Locale.getDefault(), "%d", achievedCount)

        // Set click listener to increase the achieved count of the current goal by 1
        holder.binding.crdCurrentGoal.setOnClickListener {
            fragment.lifecycleScope.launch(Dispatchers.IO) {
                val transaction = ShuffleTransaction(shuffleId, currentGoal.goalId, Calendar.getInstance().timeInMillis)
                db.getShuffleTransactionDao().insert(transaction)
                val newCount = goalAchievedCount.getOrDefault(currentGoal.goalId, 0) + 1
                goalAchievedCount.put(currentGoal.goalId, newCount)

                activity.runOnUiThread {
                    // TODO: This could theoretically cause issues, but probably won't
                    //       in practice (Changing item that might have moved already)
                    //       This is to make it more responsive and prevent the change
                    //       animation in this scenario
                    holder.binding.txtAchievedCounter.text = String.format(Locale.getDefault(), "%d", newCount)
                    if (achievedCount == 0) {
                        notifyItemChanged(position)
                    }
                }
            }
        }

        // Set long click listener for showing editor of achieved timestamps and counts
        holder.binding.crdCurrentGoal.setOnLongClickListener {
            val bottomSheetDialog = BottomSheetDialog(activity, R.style.BottomSheetDialogStyle)
            val sBinding = SheetGoalTransactionsBinding.inflate(LayoutInflater.from(activity))
            bottomSheetDialog.setContentView(sBinding.root)

            // Load achieved goal times
            fragment.lifecycleScope.launch(Dispatchers.IO) {
                val transactions = db.getShuffleTransactionDao().getAllFromShuffleGoal(shuffleId, currentGoal.goalId)

                activity.runOnUiThread {
                    val manager = RecyclerViewAdapterGoalTransactions(fragment, transactions)
                    sBinding.rcvShuffleTransactions.setAdapter(manager)
                    sBinding.rcvShuffleTransactions.setLayoutManager(LinearLayoutManager(activity))
                    sBinding.txtGoal.text = currentGoal.description
                    sBinding.txtNoneAchieved.visibility = if (transactions.isEmpty()) View.VISIBLE else View.GONE
                    sBinding.rcvShuffleTransactions.visibility = if (transactions.isEmpty()) View.GONE else View.VISIBLE

                    // Set listener to refresh entry when an entry was removed
                    manager.onDeletedListener = {
                        val newCount = goalAchievedCount.getOrDefault(currentGoal.goalId, 0) - 1
                        goalAchievedCount.put(currentGoal.goalId, newCount)
                        notifyItemChanged(position)
                        if (transactions.isEmpty()) {
                            sBinding.txtNoneAchieved.visibility = View.VISIBLE
                        }
                    }

                    bottomSheetDialog.show()
                }
            }

            true
        }
    }

    override fun getItemCount(): Int {
        return goals?.size ?: 0
    }
}
