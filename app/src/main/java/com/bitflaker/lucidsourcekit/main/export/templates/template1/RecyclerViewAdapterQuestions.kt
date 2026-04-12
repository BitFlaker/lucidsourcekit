package com.bitflaker.lucidsourcekit.main.export.templates.template1

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bitflaker.lucidsourcekit.databinding.EntryPdfExportQuestionBinding
import com.bitflaker.lucidsourcekit.main.export.templates.data.QuestionExportValueBool
import com.bitflaker.lucidsourcekit.main.export.templates.data.QuestionExportValueMultipleChoice
import com.bitflaker.lucidsourcekit.main.export.templates.data.QuestionExportValueRating
import com.bitflaker.lucidsourcekit.main.export.templates.data.QuestionExportValueText
import com.bitflaker.lucidsourcekit.main.export.templates.data.QuestionnaireExportQuestion
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textview.MaterialTextView

class RecyclerViewAdapterQuestions(
    private val context: Context,
    private val questions: List<QuestionnaireExportQuestion>
) : RecyclerView.Adapter<RecyclerViewAdapterQuestions.MainViewHolder>() {
    class MainViewHolder(var binding: EntryPdfExportQuestionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
        val binding = EntryPdfExportQuestionBinding.inflate(LayoutInflater.from(context), parent, false)
        return MainViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
        val current = questions[position]
        holder.binding.txtQuestion.text = current.question

        resetValues(holder)
        when (val value = current.value) {
            is QuestionExportValueRating -> setRatingContent(holder, value)
            is QuestionExportValueBool -> setBoolContent(holder, value)
            is QuestionExportValueMultipleChoice -> setMultipleChoiceContent(holder, value)
            is QuestionExportValueText -> setTextContent(holder, value)
        }
    }

    private fun resetValues(holder: MainViewHolder) {
        holder.binding.txtText.visibility = View.GONE
        holder.binding.llMultipleChoice.visibility = View.GONE
        holder.binding.root.tag = null
    }

    private fun setRatingContent(holder: MainViewHolder, value: QuestionExportValueRating) {
        holder.binding.txtText.visibility = View.VISIBLE
        holder.binding.txtText.text = "Selected ${value.value} in range ${value.minValue} to ${value.maxValue}"
    }

    private fun setBoolContent(holder: MainViewHolder, value: QuestionExportValueBool) {
        holder.binding.txtText.visibility = View.VISIBLE
        holder.binding.txtText.text = if (value.value) "Yes" else "No"
    }

    private fun setMultipleChoiceContent(holder: MainViewHolder, value: QuestionExportValueMultipleChoice) {
        holder.binding.llMultipleChoice.visibility = View.VISIBLE
        holder.binding.llMultipleChoice.removeAllViews()

        val options = value.options.mapIndexed { index, option ->
            MaterialTextView(context).apply {
                text = (if (value.selectedIndices.contains(index)) "X  " else "O  ") + option
                setTextSize(TypedValue.COMPLEX_UNIT_PX, 14f)
                setTextColor(Color.BLACK)
            }
        }

        for (option in options) {
            holder.binding.llMultipleChoice.addView(option)
        }
    }

    private fun setTextContent(holder: MainViewHolder, value: QuestionExportValueText) {
        holder.binding.txtText.visibility = View.VISIBLE
        holder.binding.txtText.text = value.text
        holder.binding.root.tag = "dynamic"
    }

    override fun getItemCount() = questions.size
}