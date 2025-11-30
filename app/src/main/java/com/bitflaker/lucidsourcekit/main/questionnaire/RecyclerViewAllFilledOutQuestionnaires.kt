package com.bitflaker.lucidsourcekit.main.questionnaire

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.database.questionnaire.entities.resulttables.CompletedQuestionnaireDetails
import com.bitflaker.lucidsourcekit.databinding.EntryDateBinding
import com.bitflaker.lucidsourcekit.databinding.EntryQuestionnaireBinding
import com.bitflaker.lucidsourcekit.utils.Tools
import com.bitflaker.lucidsourcekit.utils.attrColor
import com.bitflaker.lucidsourcekit.utils.dpToPx
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import kotlin.math.absoluteValue

class RecyclerViewAllFilledOutQuestionnaires(
    val context: Context,
    completed: List<CompletedQuestionnaireDetails>
): RecyclerView.Adapter<ViewHolder>() {
    class MainViewHolderQuestionnaire(val binding: EntryQuestionnaireBinding) : ViewHolder(binding.root)
    class MainViewHolderHeading(val binding: EntryDateBinding) : ViewHolder(binding.root)

    private val items: MutableList<CompletedQuestionnaireDetails?>

    init {
        var added = 0
        items = completed.toMutableList()
        for (i in 0..<items.size) {
            if (isFirstDayEntry(i + added, items)) {
                items.add(i + added, null)
                added++
            }
        }
    }

    private val dateFormat = DateFormat.getDateInstance(DateFormat.SHORT)
    private val dayOfWeekFormat = SimpleDateFormat("EEEE")
    var onQuestionnaireClickListener: ((Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(context)
        return if (viewType == 0) {
            MainViewHolderQuestionnaire(EntryQuestionnaireBinding.inflate(inflater, parent, false))
        }
        else {
            MainViewHolderHeading(EntryDateBinding.inflate(inflater, parent, false))
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position] == null) 1 else 0
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val current = items[position]
        if (holder is MainViewHolderQuestionnaire && current != null) {
            holder.binding.crdQuestionnaire.radius = 14.dpToPx.toFloat()
            holder.binding.crdQuestionnaire.setCardBackgroundColor(context.attrColor(R.attr.colorSurfaceContainer))
            holder.binding.crdQuestionnaire.updateLayoutParams<RecyclerView.LayoutParams> {
                val dp24 = 12.dpToPx
                val dp4 = 4.dpToPx
                setMargins(dp24, dp4, dp24, dp4)
            }

            val dp4 = 16.dpToPx
            val dp8 = 16.dpToPx
            holder.binding.clQuestionnaire.setPadding(dp8, dp4, dp8, dp4)

            holder.binding.crdQuestionnaire.setOnClickListener { onQuestionnaireClickListener?.invoke(current.id) }
            holder.binding.txtQuestionnaireName.text = current.title
            holder.binding.txtQuestionnaireDescription.text = current.description
            holder.binding.llQuestionnaireStats.visibility = View.GONE
            val color = current.colorCode?.toColorInt() ?: Color.TRANSPARENT
            holder.binding.vwColorIndicator.backgroundTintList = ColorStateList.valueOf(color)
            holder.binding.txtQuestionnaireName.updatePadding(left = if (color == Color.TRANSPARENT) 0 else 22.dpToPx)
        }
        else if (holder is MainViewHolderHeading) {
            if (position + 1 >= items.size) return
            val next = items[position + 1] ?: return
            holder.binding.root.updatePadding(top = if (position == 0) 8.dpToPx else 16.dpToPx)
            holder.binding.txtDate.text = dateFormat.format(next.timestamp)
            holder.binding.txtDayOfWeek.text = dayOfWeekFormat.format(next.timestamp)
        }
    }

    private fun isFirstDayEntry(position: Int, all: List<CompletedQuestionnaireDetails?>): Boolean {
        if (position == 0 || (position == 1 && all[0] == null)) return true
        val currentItem = all[position] ?: return false
        val previousItem = all[position - 1] ?: return true
        return !isSameDay(currentItem.timestamp, previousItem.timestamp)
    }

    private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        val currentCalendar = Tools.calendarFromMillis(timestamp1)
        val currentYear = currentCalendar[Calendar.YEAR]
        val currentDayOfYear = currentCalendar[Calendar.DAY_OF_YEAR]

        val previousCalendar = Tools.calendarFromMillis(timestamp2)
        val previousYear = previousCalendar[Calendar.YEAR]
        val previousDayOfYear = previousCalendar[Calendar.DAY_OF_YEAR]

        return currentDayOfYear == previousDayOfYear && currentYear == previousYear
    }

    fun addCompletedQuestionnaire(completed: CompletedQuestionnaireDetails) {
        val insertIndex = items.filterNotNull().binarySearch {
            it.timestamp.compareTo(completed.timestamp) * -1
        }.absoluteValue - 1

        // Find position beneath heading to insert
        var index = items.size
        var nonNullCount = 0
        for (i in items.indices) {
            if (items[i] != null) {
                if (nonNullCount == insertIndex) {
                    index = i
                    break
                }
                nonNullCount++
            }
            else if (nonNullCount == insertIndex && i + 1 < items.size && items[i + 1] != null && !isSameDay(completed.timestamp, items[i + 1]!!.timestamp)) {
                index = i
                break
            }
        }

        // Check if should actually be placed beneath next heading (this probably is redundant as the loop above should guarantee to be be beneath the correct heading)
        if (index + 2 < items.size && items[index + 1] == null && items[index + 2] != null && isSameDay(completed.timestamp, items[index + 2]!!.timestamp)) {
            index++
        }

        // Insert item and add heading if required
        items.add(index, completed)
        if (isFirstDayEntry(index, items) && (index == 0 || items[index - 1] != null)) {
            items.add(index, null)
            notifyItemRangeInserted(index, 2)
        }
        else {
            notifyItemInserted(index)
        }
    }
}