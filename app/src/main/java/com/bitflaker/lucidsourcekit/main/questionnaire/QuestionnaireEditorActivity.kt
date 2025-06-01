package com.bitflaker.lucidsourcekit.main.questionnaire

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.databinding.ActivityQuestionnaireEditorBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class QuestionnaireEditorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityQuestionnaireEditorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuestionnaireEditorBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnClose.setOnClickListener { promptDiscardChanges() }
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