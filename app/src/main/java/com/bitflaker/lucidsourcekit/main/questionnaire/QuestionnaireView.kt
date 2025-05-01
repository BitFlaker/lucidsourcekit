package com.bitflaker.lucidsourcekit.main.questionnaire

import android.content.DialogInterface
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.databinding.ActivityQuestionnaireBinding
import com.bitflaker.lucidsourcekit.main.questionnaire.options.RangeOptions
import com.bitflaker.lucidsourcekit.main.questionnaire.options.SelectOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class QuestionnaireView : AppCompatActivity() {
    private lateinit var binding: ActivityQuestionnaireBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityQuestionnaireBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // By default the 'Next' button is disabled and only after completing
        // the current question, the next button will be re-enabled
        binding.btnQuestionnaireNext.isEnabled = false

//        val ctx = QuestionnaireControlContext(QuestionnaireControlType.Bool, null)
//        val ctx = QuestionnaireControlContext(QuestionnaireControlType.Text, null)
//        val ctx = QuestionnaireControlContext(QuestionnaireControlType.Range, RangeOptions(1,5))
        val ctx = QuestionnaireControlContext(QuestionnaireControlType.SingleSelect, SelectOptions(
            arrayOf("Option 1", "Option 2", "Option 3", "None of the above")
        ))
//        val ctx = QuestionnaireControlContext(QuestionnaireControlType.MultiSelect, SelectOptions(
//            arrayOf("Option 1", "Option 2", "Option 3", "None of the above")
//        ))

        val adapter = RecyclerViewQuestionnaireControl(this, ctx)
        adapter.setResultListener { binding.btnQuestionnaireNext.isEnabled = it }
        binding.rcvQuestionControl.adapter = adapter
        binding.rcvQuestionControl.layoutManager = LinearLayoutManager(this)

        binding.btnQuestionnaireNext.setOnClickListener {
            val holder = binding.rcvQuestionControl.findViewHolderForAdapterPosition(0) as RecyclerViewQuestionnaireControl.MainViewHolder
            println(holder.result)
        }

        binding.btnQuestionnaireClose.setOnClickListener { promptDiscardChanges() }
    }

    private fun promptDiscardChanges() {
        MaterialAlertDialogBuilder(this, R.style.Theme_LucidSourceKit_ThemedDialog)
            .setTitle("Discard changes")
            .setMessage("Do you really want to discard all changes")
            .setPositiveButton(resources.getString(R.string.yes)) { _, _ -> finish() }
            .setNegativeButton(resources.getString(R.string.no), null)
            .show()
    }
}