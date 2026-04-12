package com.bitflaker.lucidsourcekit.main.export.templates.template1

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.doOnLayout
import androidx.core.view.doOnNextLayout
import androidx.core.view.doOnPreDraw
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bitflaker.lucidsourcekit.databinding.EntryPdfExportQuestionnaireBinding
import com.bitflaker.lucidsourcekit.main.export.templates.data.QuestionnaireExportData
import com.bitflaker.lucidsourcekit.utils.pdf.layout.DocumentLayout

class RecyclerViewAdapterQuestionnaires(
    private val context: Context,
    private val questionnaires: List<QuestionnaireExportData>,
    private var gridSpanContext: Array<IntArray>? = null,
    private var layout: DocumentLayout
) : RecyclerView.Adapter<RecyclerViewAdapterQuestionnaires.MainViewHolder>() {
    class MainViewHolder(var binding: EntryPdfExportQuestionnaireBinding) : RecyclerView.ViewHolder(binding.root)

    private val isCalculate = gridSpanContext == null

    init {
        if (gridSpanContext == null) {
            gridSpanContext = Array(itemCount) { IntArray(questionnaires[it].questions.size) { 1 } }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
        val binding = EntryPdfExportQuestionnaireBinding.inflate(LayoutInflater.from(context), parent, false)
        return MainViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
        val current = questionnaires[position]
        holder.binding.txtQuestionnaireTitle.text = current.name

        val manager = GridLayoutManager(context, 3)
        holder.binding.rcvQuestionnaire.layoutManager = manager
        holder.binding.rcvQuestionnaire.adapter = RecyclerViewAdapterQuestions(context, current.questions)

        if (isCalculate) {
            holder.binding.rcvQuestionnaire.doOnNextLayout {
                gridSpanContext!![holder.bindingAdapterPosition] = DocumentLayout.autoSpanGridLayout(holder.binding.rcvQuestionnaire, 128)
            }
        }
        else {
            manager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(innerPosition: Int): Int {
                    return gridSpanContext!![holder.bindingAdapterPosition].getOrNull(innerPosition) ?: 1
                }
            }
        }
    }

    override fun getItemCount() = questionnaires.size

    fun getGridSpanContext() = gridSpanContext ?: arrayOf()
}