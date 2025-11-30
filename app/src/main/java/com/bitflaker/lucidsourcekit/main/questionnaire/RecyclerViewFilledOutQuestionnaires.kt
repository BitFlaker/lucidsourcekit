package com.bitflaker.lucidsourcekit.main.questionnaire

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.database.questionnaire.entities.resulttables.CompletedQuestionnaireDetails
import com.bitflaker.lucidsourcekit.databinding.EntryQuestionnaireBinding
import com.bitflaker.lucidsourcekit.utils.Tools
import com.bitflaker.lucidsourcekit.utils.attrColor
import com.bitflaker.lucidsourcekit.utils.dpToPx

class RecyclerViewFilledOutQuestionnaires(
    val context: Context,
    private val items: MutableList<CompletedQuestionnaireDetails>
): RecyclerView.Adapter<RecyclerViewFilledOutQuestionnaires.MainViewHolder>() {
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
        holder.binding.crdQuestionnaire.setCardBackgroundColor(context.attrColor(R.attr.colorSurfaceContainerLow))
        holder.binding.crdQuestionnaire.setOnClickListener { onQuestionnaireClickListener?.invoke(current.id) }
        holder.binding.txtQuestionnaireName.text = current.title
        holder.binding.txtQuestionnaireDescription.text = current.description
        holder.binding.llQuestionnaireStats.visibility = View.GONE
        val color = current.colorCode?.toColorInt() ?: Color.TRANSPARENT
        holder.binding.vwColorIndicator.backgroundTintList = ColorStateList.valueOf(color)
        holder.binding.txtQuestionnaireName.updatePadding(left = if (color == Color.TRANSPARENT) 0 else 22.dpToPx)
    }

    fun addCompletedQuestionnaire(completed: CompletedQuestionnaireDetails) {
        items.add(0, completed)
        notifyItemInserted(0)
    }
}