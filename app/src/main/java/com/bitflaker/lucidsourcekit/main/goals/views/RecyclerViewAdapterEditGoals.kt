package com.bitflaker.lucidsourcekit.main.goals.views

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil.ItemCallback
import androidx.recyclerview.widget.RecyclerView
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.database.goals.entities.Goal
import com.bitflaker.lucidsourcekit.databinding.EntryGoalBinding
import com.bitflaker.lucidsourcekit.main.goals.views.RecyclerViewAdapterEditGoals.MainViewHolderGoals
import com.bitflaker.lucidsourcekit.utils.Tools
import com.bitflaker.lucidsourcekit.utils.attrColor
import com.bitflaker.lucidsourcekit.utils.attrColorStateList

class RecyclerViewAdapterEditGoals(private val context: Context) : RecyclerView.Adapter<MainViewHolderGoals>() {
    private lateinit var mRecyclerView: RecyclerView
    private var isInSelectionMode = false
    private var selectionCount = 0
    var onEntryClickedListener: ((Goal, Int) -> Unit)? = null
    var onMultiSelectEnterListener: (() -> Unit)? = null
    var onMultiSelectExitListener: (() -> Unit)? = null
    private val goals: AsyncListDiffer<Goal> = AsyncListDiffer(this, object : ItemCallback<Goal>() {
        override fun areItemsTheSame(oldItem: Goal, newItem: Goal): Boolean = oldItem.goalId == newItem.goalId
        override fun areContentsTheSame(oldItem: Goal, newItem: Goal): Boolean = oldItem == newItem
    })

    init {
        // TODO: Check if this listener is really necessary (could be to update position variables in listener after deleting / inserting new entry)
        goals.addListListener { previous, current ->
            notifyItemRangeChanged(0, goals.currentList.size)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolderGoals {
        val binding = EntryGoalBinding.inflate(LayoutInflater.from(context), parent, false)
        return MainViewHolderGoals(binding)
    }

    override fun onBindViewHolder(holder: MainViewHolderGoals, position: Int) {
        val currentGoal = goals.currentList[position]
        holder.binding.txtGoalText.text = currentGoal.description

        // Handle change in selected state
        if (currentGoal.isSelected && !holder.isSelected) {
            holder.select()
        } else if (!currentGoal.isSelected && holder.isSelected) {
            holder.deselect()
        }

        // Get the color gradient value for the current difficulty and apply it to the drawable
        val difficultyColor = ColorStateList.valueOf(Tools.getColorAtGradientPosition(
            currentGoal.difficulty,
            1f,
            3f,
            context.attrColor(R.attr.colorSuccess),
            context.attrColor(R.attr.colorWarning),
            context.attrColor(R.attr.colorError)
        ))
        TextViewCompat.setCompoundDrawableTintList(holder.binding.txtGoalText, difficultyColor)

        // Set click listener on goal entry
        holder.binding.clGoalContainer.setOnClickListener {
            if (!isInSelectionMode) {
                onEntryClickedListener?.invoke(currentGoal, position)
                return@setOnClickListener
            }

            // Handle selection change in selection mode
            currentGoal.isSelected = !holder.isSelected
            if (!holder.isSelected) {
                holder.select()
                selectionCount++
            }
            else {
                holder.deselect()
                selectionCount--
                if (selectionCount == 0) {
                    isInSelectionMode = false
                    onMultiSelectExitListener?.invoke()
                }
            }
        }

        // Set long click listener for starting multiselect
        holder.binding.clGoalContainer.setOnLongClickListener {
            if (!holder.isSelected) {
                isInSelectionMode = true
                currentGoal.isSelected = true
                holder.select()
                selectionCount++
                onMultiSelectEnterListener?.invoke()
            }

            true
        }

        // Show lock in case difficulty of entry is locked
        holder.binding.imgDifficultyLocked.setVisibility(if (currentGoal.difficultyLocked) View.VISIBLE else View.GONE)
    }

    override fun getItemCount(): Int {
        return goals.currentList.size
    }

    fun setEntries(goals: List<Goal>) {
        isInSelectionMode = false
        selectionCount = 0
        this.goals.submitList(goals)
        onMultiSelectExitListener?.invoke()
    }

    val selectedGoalIds: List<Int>
        get() = goals.currentList.filter { it.isSelected }.map { it.goalId }.toList()

    val selectedGoals: List<Goal>
        get() = goals.currentList.filter { it.isSelected }.toList()

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        mRecyclerView = recyclerView
    }

    inner class MainViewHolderGoals(var binding: EntryGoalBinding) : RecyclerView.ViewHolder(binding.root) {
        var isSelected: Boolean = false

        fun select() {
            isSelected = true
            binding.clGoalContainer.setBackgroundTintList(context.attrColorStateList(R.attr.colorSurfaceContainer))
            val icon = ResourcesCompat.getDrawable(context.resources, R.drawable.ic_baseline_check_24, context.theme)
            binding.txtGoalText.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null)
        }

        fun deselect() {
            isSelected = false
            binding.clGoalContainer.setBackgroundTintList(context.attrColorStateList(R.attr.colorSurface))
            val icon = ResourcesCompat.getDrawable(context.resources, R.drawable.ic_baseline_circle_24, context.theme)
            binding.txtGoalText.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null)
        }
    }
}
