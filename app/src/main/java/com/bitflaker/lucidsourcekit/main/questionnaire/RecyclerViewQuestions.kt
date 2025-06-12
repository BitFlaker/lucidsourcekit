package com.bitflaker.lucidsourcekit.main.questionnaire

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.database.questionnaire.entities.Question
import com.bitflaker.lucidsourcekit.database.questionnaire.entities.QuestionOptions
import com.bitflaker.lucidsourcekit.database.questionnaire.entities.resulttables.QuestionnaireDetails
import com.bitflaker.lucidsourcekit.databinding.EntryQuestionEditorBinding
import com.bitflaker.lucidsourcekit.databinding.EntryQuestionnaireBinding
import java.util.Collections
import java.util.Locale

class RecyclerViewQuestions(
    val context: Context,
    private val items: MutableList<Question>
): RecyclerView.Adapter<RecyclerViewQuestions.MainViewHolder>() {
    class MainViewHolder(val binding: EntryQuestionEditorBinding) : ViewHolder(binding.root)

    var questionClickListener: ((Int) -> Unit)? = null
    val questions: List<Question>
        get() {
            items.forEachIndexed { i, item -> item.orderNr = i }
            return items
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
        val binding = EntryQuestionEditorBinding.inflate(LayoutInflater.from(context), parent, false)
        return MainViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
        val current = items[position]
        holder.binding.root.setOnClickListener {
            questionClickListener?.invoke(current.id)
        }
        holder.binding.txtQuestion.text = current.question
        holder.binding.imgQuestionTypeIcon.setImageResource(when (current.questionTypeId) {
            QuestionnaireControlType.Range.ordinal -> R.drawable.rounded_line_end_24
            QuestionnaireControlType.SingleSelect.ordinal -> R.drawable.rounded_rule_24
            QuestionnaireControlType.MultiSelect.ordinal -> R.drawable.rounded_checklist_rtl_24
            QuestionnaireControlType.Bool.ordinal -> R.drawable.rounded_toggle_on_24
            QuestionnaireControlType.Text.ordinal -> R.drawable.rounded_text_fields_24
            else -> throw IllegalStateException("Unknown question type ${current.questionTypeId}")
        })
        holder.binding.txtQuestionOptions.text = when (current.questionTypeId) {
            QuestionnaireControlType.Range.ordinal -> String.format(Locale.getDefault(), "From %d to %d", current.valueFrom, current.valueTo)
            QuestionnaireControlType.SingleSelect.ordinal -> current.options?.joinToString("  •  ") { it.text }
            QuestionnaireControlType.MultiSelect.ordinal -> current.options?.joinToString("  •  ") { it.text }
            QuestionnaireControlType.Bool.ordinal -> "True / False"
            QuestionnaireControlType.Text.ordinal -> "Free text"
            else -> throw IllegalStateException("Unknown question type ${current.questionTypeId}")
        }
    }

    fun addQuestion(question: Question) {
        items.add(question)
        notifyItemInserted(items.size - 1)
    }

    fun updateQuestion(question: Question) {
        val index = items.indexOfFirst { it.id == question.id }
        items[index] = question
        notifyItemChanged(index)
    }

    fun removeQuestion(questionId: Int) {
        val index = items.indexOfFirst { it.id == questionId }
        items.removeAt(index)
        notifyItemRemoved(index)
    }

    fun swap(from: Int, to: Int) {
        Collections.swap(items, from, to)
        notifyItemMoved(from, to)
    }
}