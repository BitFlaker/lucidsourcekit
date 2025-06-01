package com.bitflaker.lucidsourcekit.main.questionnaire

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bitflaker.lucidsourcekit.database.questionnaire.entities.resulttables.QuestionnaireDetails
import com.bitflaker.lucidsourcekit.databinding.EntryQuestionnaireBinding

class RecyclerViewQuestionnaireOverview(
    val context: Context,
    private val items: List<QuestionnaireDetails>
): RecyclerView.Adapter<RecyclerViewQuestionnaireOverview.MainViewHolder>() {
    class MainViewHolder(val binding: EntryQuestionnaireBinding) : ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
        val binding = EntryQuestionnaireBinding.inflate(LayoutInflater.from(context), parent, false)
        return MainViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
        val current = items[position]
        holder.binding.crdQuestionnaire.setOnClickListener {
            val intent = Intent(context.applicationContext, QuestionnaireEditorActivity::class.java)
            intent.putExtra("QUESTIONNAIRE_ID", current.id)
            context.startActivity(intent)
        }
        holder.binding.txtQuestionnaireName.text = current.title
        holder.binding.txtQuestionnaireDescription.text = current.description
        holder.binding.txtBadgeQuestionCount.text = "${current.questionCount} questions"

        // Calculate average questionnaire fill out duration
        val avgSecs = current.averageDuration / 1000
        val avgTime = if (avgSecs < 60) "$avgSecs sec" else "${(avgSecs / 60.0f).toInt()} min"
        holder.binding.txtBadgeAvgDuration.text = "avg $avgTime"
    }
}