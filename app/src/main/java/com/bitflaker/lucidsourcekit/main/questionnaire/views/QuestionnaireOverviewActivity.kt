package com.bitflaker.lucidsourcekit.main.questionnaire.views

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bitflaker.lucidsourcekit.database.MainDatabase
import com.bitflaker.lucidsourcekit.databinding.ActivityQuestionnaireOverviewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuestionnaireOverviewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityQuestionnaireOverviewBinding
    private lateinit var adapter: RecyclerViewQuestionnaireOverview
    private lateinit var db: MainDatabase
    private val editorLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val id = result.data?.getIntExtra("QUESTIONNAIRE_ID", -1) ?: -1
        val isCreateMode = result.data?.getBooleanExtra("IS_CREATE_MODE", false) ?: false
        val isDeleted = result.data?.getBooleanExtra("IS_DELETED", false) ?: false
        if (isDeleted) {
            adapter.removeQuestionnaire(id)
        }
        else if (result.resultCode == RESULT_OK && id != -1) {
            lifecycleScope.launch(Dispatchers.IO) {
                reloadEntryData(id, isCreateMode)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityQuestionnaireOverviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Get all questionnaires
        db = MainDatabase.getInstance(this)

        val context = this
        lifecycleScope.launch {
            val questionnaires = withContext(Dispatchers.IO) {
                db.questionnaireDao.getAllDetails().toMutableList()
            }

            // Set visibility of no questionnaires found notice
            val visibilityNoEntries = if (questionnaires.isEmpty()) View.VISIBLE else View.GONE
            binding.txtNoQuestionnairesTitle.visibility = visibilityNoEntries
            binding.txtNoQuestionnairesSubTitle.visibility = visibilityNoEntries

            // Configure questionnaire recycler view
            binding.rcvQuestionnaires.layoutManager = LinearLayoutManager(context)
            adapter = RecyclerViewQuestionnaireOverview(context, questionnaires)
            adapter.onQuestionnaireClickListener = { id ->
                val intent = Intent(context, QuestionnaireEditorActivity::class.java)
                intent.putExtra("QUESTIONNAIRE_ID", id)
                editorLauncher.launch(intent)
            }
            binding.rcvQuestionnaires.adapter = adapter

            // Configure create questionnaire button
            binding.btnAddQuestionnaire.setOnClickListener {
                val intent = Intent(context, QuestionnaireEditorActivity::class.java)
                intent.putExtra("QUESTIONNAIRE_ID", -1)
                editorLauncher.launch(intent)
            }

            // Configure back button
            binding.btnQuestionnaireClose.setOnClickListener { finish() }
        }
    }

    private suspend fun reloadEntryData(questionnaireId: Int, isCreateMode: Boolean) {
        // TODO: When reordering is implemented, add checks for changes in ordering as well
        val questionnaire = db.questionnaireDao.getDetailsById(questionnaireId)
        withContext(Dispatchers.Main) {
            if (isCreateMode) {
                adapter.addQuestionnaire(questionnaire)
            }
            else {
                adapter.updateQuestionnaire(questionnaire)
            }
        }
    }
}