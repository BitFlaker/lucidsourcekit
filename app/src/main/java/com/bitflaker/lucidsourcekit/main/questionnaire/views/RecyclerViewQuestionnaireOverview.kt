package com.bitflaker.lucidsourcekit.main.questionnaire.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bitflaker.lucidsourcekit.database.questionnaire.entities.results.QuestionnaireDetails
import com.bitflaker.lucidsourcekit.databinding.EntryQuestionnaireBinding
import com.bitflaker.lucidsourcekit.utils.dpToPx

class RecyclerViewQuestionnaireOverview(
    val context: Context,
    private val items: MutableList<QuestionnaireDetails>
): RecyclerView.Adapter<RecyclerViewQuestionnaireOverview.MainViewHolder>() {
    class MainViewHolder(val binding: EntryQuestionnaireBinding) : ViewHolder(binding.root)

    var onQuestionnaireClickListener: ((Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
        val binding = EntryQuestionnaireBinding.inflate(LayoutInflater.from(context), parent, false)
        return MainViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
        val current = items[position]
        holder.binding.crdQuestionnaire.setOnClickListener { onQuestionnaireClickListener?.invoke(current.id) }
        holder.binding.txtQuestionnaireName.text = current.title
        holder.binding.txtQuestionnaireDescription.text = current.description
        holder.binding.txtBadgeQuestionCount.text = "${current.questionCount} questions"
        val color = current.colorCode?.toColorInt() ?: Color.TRANSPARENT
        holder.binding.vwColorIndicator.backgroundTintList = ColorStateList.valueOf(color)
        holder.binding.txtQuestionnaireName.updatePadding(left = if (color == Color.TRANSPARENT) 0 else 22.dpToPx)

        // Calculate average questionnaire fill out duration
        val avgSecs = current.averageDuration / 1000
        val avgTime = if (avgSecs < 60) "$avgSecs sec" else "${(avgSecs / 60.0f).toInt()} min"
        holder.binding.txtBadgeAvgDuration.text = "avg $avgTime"
    }

    fun updateQuestionnaire(questionnaire: QuestionnaireDetails) {
        val index = items.indexOfFirst { it.id == questionnaire.id }
        if (index == -1) return
        items[index] = questionnaire
        notifyItemChanged(index)
    }

    fun addQuestionnaire(questionnaire: QuestionnaireDetails) {
        items.add(questionnaire)
        notifyItemInserted(items.size - 1)
    }

    fun removeQuestionnaire(questionnaireId: Int) {
        val index = items.indexOfFirst { it.id == questionnaireId }
        if (index == -1) return
        items.removeAt(index)
        notifyItemRemoved(index)
    }
}