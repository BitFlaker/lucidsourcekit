package com.bitflaker.lucidsourcekit.main.questionnaire.views

import android.annotation.SuppressLint
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.GridLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.viewbinding.ViewBinding
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.databinding.EntryQuestionnaireControlBoolBinding
import com.bitflaker.lucidsourcekit.databinding.EntryQuestionnaireControlRangeBinding
import com.bitflaker.lucidsourcekit.databinding.EntryQuestionnaireControlSelectionBinding
import com.bitflaker.lucidsourcekit.databinding.EntryQuestionnaireControlTextBinding
import com.bitflaker.lucidsourcekit.main.questionnaire.QuestionnaireControlContext
import com.bitflaker.lucidsourcekit.main.questionnaire.QuestionnaireControlType
import com.bitflaker.lucidsourcekit.main.questionnaire.options.RangeOptions
import com.bitflaker.lucidsourcekit.main.questionnaire.options.ChoiceOptions
import com.bitflaker.lucidsourcekit.main.questionnaire.results.ControlResult
import com.bitflaker.lucidsourcekit.main.questionnaire.results.ControlResultBool
import com.bitflaker.lucidsourcekit.main.questionnaire.results.ControlResultMultiSelect
import com.bitflaker.lucidsourcekit.main.questionnaire.results.ControlResultRange
import com.bitflaker.lucidsourcekit.main.questionnaire.results.ControlResultSingleSelect
import com.bitflaker.lucidsourcekit.main.questionnaire.results.ControlResultText
import com.bitflaker.lucidsourcekit.utils.dpToPx
import com.google.android.material.button.MaterialButton
import java.util.Locale

class RecyclerViewQuestionnaireControl(
    val context: Context,
    private var control: QuestionnaireControlContext
) : RecyclerView.Adapter<RecyclerViewQuestionnaireControl.MainViewHolder>() {
    private var resultListener: ((Boolean) -> Unit)? = null
    var questionContext: QuestionnaireControlContext = control
        set(value) {
            field = value
            control = value

            // In this case notifying a dataset change should be fine as the
            // dataset only consists of a single entry anyways and this also
            // prevents any animations to play (entry changed animation)
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
        val inflater = LayoutInflater.from(context)
        return when (viewType) {
            QuestionnaireControlType.Bool.ordinal -> MainViewHolder(EntryQuestionnaireControlBoolBinding.inflate(inflater, parent, false), context)
            QuestionnaireControlType.Range.ordinal -> MainViewHolder(EntryQuestionnaireControlRangeBinding.inflate(inflater, parent, false), context)
            QuestionnaireControlType.SingleSelect.ordinal -> MainViewHolder(EntryQuestionnaireControlSelectionBinding.inflate(inflater, parent, false), context)
            QuestionnaireControlType.MultiSelect.ordinal -> MainViewHolder(EntryQuestionnaireControlSelectionBinding.inflate(inflater, parent, false), context)
            QuestionnaireControlType.Text.ordinal -> MainViewHolder(EntryQuestionnaireControlTextBinding.inflate(inflater, parent, false), context)
            else -> throw IllegalArgumentException("Unknown view type $viewType for questionnaire control")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return control.type.ordinal
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
        holder.bind(control, { resultListener?.invoke(it) })
    }

    override fun getItemCount(): Int {
        return 1
    }

    fun setResultListener(listener: ((Boolean) -> Unit)?) {
        resultListener = listener
    }

    class MainViewHolder(val binding: ViewBinding, val context: Context) : ViewHolder(binding.root) {
        var result: ControlResult? = null
        val maxTextLength = 500
        var resultListener: ((Boolean) -> Unit)? = null

        fun bind(
            control: QuestionnaireControlContext,
            resultListener: ((Boolean) -> Unit)?
        ) {
            this.result = control.result
            this.resultListener = resultListener
            when (control.type) {
                QuestionnaireControlType.Bool -> bindBoolControl()
                QuestionnaireControlType.Range -> bindRangeControl(control)
                QuestionnaireControlType.SingleSelect -> bindSingleSelectControl(control)
                QuestionnaireControlType.MultiSelect -> bindMultiSelectControl(control)
                QuestionnaireControlType.Text -> bindTextControl()
            }
        }

        private fun bindTextControl() {
            val cb = binding as EntryQuestionnaireControlTextBinding
            if (result == null) {
                result = ControlResultText("")
                resultListener?.invoke(true)
            }
            cb.etQcText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
                override fun afterTextChanged(s: Editable?) {
                    cb.txtCharCount.text = String.format(Locale.getDefault(), "%d / %d", s?.length ?: 0, maxTextLength)
                    val res = result as ControlResultText
                    res.result = s?.toString() ?: ""
                }
            })

            // Set correct state according to current result value
            val resultText = (result as? ControlResultText)?.result
            if (resultText != null) {
                cb.etQcText.setText(resultText)
            }
        }

        private fun bindMultiSelectControl(control: QuestionnaireControlContext) {
            val cb = binding as EntryQuestionnaireControlSelectionBinding
            val options = control.options as ChoiceOptions
            val opts = setOptions(cb, options, true)

            for (i in opts.indices) {
                val button = opts[i]
                button.setOnClickListener {
                    if (result == null) {
                        result = ControlResultMultiSelect(ArrayList())
                        resultListener?.invoke(true)
                    }
                    val res = result as ControlResultMultiSelect
                    if (button.isChecked) {
                        res.result.add(options.values[i].id)
                    }
                    else {
                        res.result.remove(options.values[i].id)
                        if (res.result.isEmpty()) {
                            result = null
                            resultListener?.invoke(false)
                        }
                    }
                }
            }

            // Set correct state according to current result value
            val selectedIds = (result as? ControlResultMultiSelect)?.result
            if (selectedIds != null) {
                for (i in opts.indices) {
                    if (selectedIds.contains(options.values[i].id)) {
                        opts[i].isChecked = true
                    }
                }
            }
        }

        private fun bindSingleSelectControl(control: QuestionnaireControlContext) {
            val cb = binding as EntryQuestionnaireControlSelectionBinding
            val options = control.options as ChoiceOptions
            val opts = setOptions(cb, options, false)

            // Configure click listeners (and ensure only a single item is selected)
            for (i in opts.indices) {
                opts[i].setOnClickListener {
                    for (b in opts) {
                        b.isChecked = b == opts[i]
                    }
                    val resultWasNull = result == null
                    result = ControlResultSingleSelect(options.values[i].id)
                    if (resultWasNull) {
                        resultListener?.invoke(true)
                    }
                }
            }

            // Set correct state according to current result value
            val selectedId = (result as? ControlResultSingleSelect)?.result
            if (selectedId != null) {
                val optionIndex = options.values.indexOfFirst { it.id ==  selectedId }
                opts[optionIndex].isChecked = true
            }
        }

        private fun bindRangeControl(control: QuestionnaireControlContext) {
            val cb = binding as EntryQuestionnaireControlRangeBinding
            val options = control.options as RangeOptions
            cb.txtQcValueFrom.text = String.format(Locale.getDefault(), "%d", options.from)
            cb.txtQcValueTo.text = String.format(Locale.getDefault(), "%d", options.to)
            cb.sldQcSlider.valueFrom = options.from.toFloat()
            cb.sldQcSlider.valueTo = options.to.toFloat()
            cb.sldQcSlider.addOnChangeListener { _, value, _ ->
                val resultWasNull = result == null
                result = ControlResultRange(value.toInt())
                if (resultWasNull) {
                    resultListener?.invoke(true)
                }
            }

            // Set correct state according to current result value
            cb.sldQcSlider.value = (result as? ControlResultRange)?.result?.toFloat() ?: options.from.toFloat()
            result = ControlResultRange(cb.sldQcSlider.value.toInt())
        }

        private fun bindBoolControl() {
            val cb = binding as EntryQuestionnaireControlBoolBinding

            // Configure click listeners
            cb.btnQcTrue.setOnClickListener {
                result = ControlResultBool(true)
                resultListener?.invoke(true)
                cb.btnQcTrue.isChecked = true
                cb.btnQcFalse.isChecked = false
            }
            cb.btnQcFalse.setOnClickListener {
                result = ControlResultBool(false)
                resultListener?.invoke(true)
                cb.btnQcTrue.isChecked = false
                cb.btnQcFalse.isChecked = true
            }

            // Set correct state according to current result value
            val isTrue = (result as? ControlResultBool)?.result
            cb.btnQcTrue.isChecked = isTrue ?: false
            cb.btnQcFalse.isChecked = isTrue != null && !isTrue
        }

        private fun setOptions(
            cb: EntryQuestionnaireControlSelectionBinding,
            options: ChoiceOptions,
            isMultiSelect: Boolean
        ): ArrayList<MaterialButton> {
            cb.glOptionContainer.removeAllViews()
            val opts = ArrayList<MaterialButton>(options.values.size)
            for (option in options.values) {
                val button = MaterialButton(context, null, R.attr.materialButtonOutlinedStyle)
                val lParams = GridLayout.LayoutParams()
                lParams.height = GridLayout.LayoutParams.WRAP_CONTENT
                lParams.width = GridLayout.LayoutParams.MATCH_PARENT
                button.layoutParams = lParams
                button.minHeight = 64.dpToPx
                button.isCheckable = true
                button.isToggleCheckedStateOnClick = isMultiSelect
                button.cornerRadius = context.resources.getDimension(R.dimen.cardRadius).toInt()
                button.text = option.text
                opts.add(button)
                cb.glOptionContainer.addView(button)
            }
            return opts
        }
    }
}
