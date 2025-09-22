package com.bitflaker.lucidsourcekit.main.questionnaire

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat.getSystemService
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bitflaker.lucidsourcekit.database.questionnaire.entities.QuestionOptions
import com.bitflaker.lucidsourcekit.databinding.EntryQuestionOptionEditorBinding
import java.util.Collections

class RecyclerViewQuestionOptions(
    val context: Context,
    private val items: MutableList<QuestionOptions>
): RecyclerView.Adapter<RecyclerViewQuestionOptions.MainViewHolder>() {
    class MainViewHolder(val binding: EntryQuestionOptionEditorBinding) : ViewHolder(binding.root) {
        var textWatcher: TextWatcher? = null
    }
    private var pendingFocusLast: Boolean = false

    var optionEditFocused: (() -> Unit)? = null
    val options: List<QuestionOptions>
        get() {
            return items.filter { it.text.isNotEmpty() }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
        val binding = EntryQuestionOptionEditorBinding.inflate(LayoutInflater.from(context), parent, false)
        return MainViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
        val current = items[position]
        holder.binding.txtQuestionOption.setText(current.text)
        holder.binding.btnDeleteOption.setOnClickListener {
            items.removeAt(holder.bindingAdapterPosition)

            // NOTE: Without clearFocus() an `IllegalArgumentException: parameter must be a descendant of this view`
            // will be thrown and for some reason and there seems to not be a reliable way to fix this other than simply catching
            // the exception and ignoring it (clearing the focus before notifying about the removed item is not working here)
            holder.binding.txtQuestionOption.clearFocus()

            notifyItemRemoved(holder.bindingAdapterPosition)
        }
        holder.binding.txtQuestionOption.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) optionEditFocused?.invoke()
        }

        // Handle the case to focus and show the keyboard for the last item if requested (after appending a new entry)
        if (pendingFocusLast && position == itemCount - 1) {
            pendingFocusLast = false
            holder.binding.txtQuestionOption.requestFocus()

            // Show the Soft-Keyboard after a short delay after which the focus has probably been
            // received (otherwise the keyboard will not show up when the field has not yet received the focus)
            val imm = getSystemService(context, InputMethodManager::class.java)
            Handler(Looper.getMainLooper()).postDelayed({
                imm!!.showSoftInput(holder.binding.txtQuestionOption, 0)
            }, 50)
        }

        // Remove old text watcher if present
        holder.textWatcher?.let {
            holder.binding.txtQuestionOption.removeTextChangedListener(it)
        }

        // Create new text watcher to update the value of the option
        holder.textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                items[holder.bindingAdapterPosition].text = s.toString()
            }
        }

        // Register the text watcher
        holder.binding.txtQuestionOption.addTextChangedListener(holder.textWatcher)
    }

    fun swap(from: Int, to: Int) {
        Collections.swap(items, from, to)
        notifyItemMoved(from, to)

        // TODO: Sometimes it might happen for a focused EditText which was moved to retain the I-Beam indicator
        //       this then causes multiple I-Beams to be displayed at the same time. Find a way to mitigate this
    }

    fun addOption(option: QuestionOptions) {
        items.add(option)
        pendingFocusLast = true
        notifyItemInserted(items.size - 1)
    }
}