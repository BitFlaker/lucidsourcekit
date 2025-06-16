package com.bitflaker.lucidsourcekit.main.questionnaire

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bitflaker.lucidsourcekit.database.MainDatabase
import com.bitflaker.lucidsourcekit.databinding.ActivityCompletedQuestionnaireViewerBinding

class CompletedQuestionnaireViewerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCompletedQuestionnaireViewerBinding
    private lateinit var db: MainDatabase
    private lateinit var adapter: RecyclerViewAllFilledOutQuestionnaires

    private val editorLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val intent = result.data
        if (intent != null) {
            val id = intent.getIntExtra("COMPLETED_QUESTIONNAIRE_ID", -1)
            if (result.resultCode == RESULT_OK && id != -1) {
                val completed = db.completedQuestionnaireDao.getDetailsById(id).blockingGet()
                adapter.addCompletedQuestionnaire(completed)
                runOnUiThread {
                    binding.txtNoQuestionnairesTitle.visibility = View.GONE
                    binding.txtNoQuestionnairesSubTitle.visibility = View.GONE
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCompletedQuestionnaireViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = MainDatabase.getInstance(this)

        // Add null entries to for headings
        val items = db.completedQuestionnaireDao.getAllDetails().blockingGet()

        // Set visibility for indicator of no entries found
        val noneVisibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        binding.txtNoQuestionnairesTitle.visibility = noneVisibility
        binding.txtNoQuestionnairesSubTitle.visibility = noneVisibility

        // Configure recycler view for list of all questionnaires
        adapter = RecyclerViewAllFilledOutQuestionnaires(this, items)
        adapter.onQuestionnaireClickListener = { id ->
            val intent = Intent(this, QuestionnaireEditorActivity::class.java)
            intent.putExtra("COMPLETED_QUESTIONNAIRE_ID", id)
            startActivity(intent)
        }
        binding.rcvCompletedQuestionnaires.layoutManager = LinearLayoutManager(this)
        binding.rcvCompletedQuestionnaires.adapter = adapter

        // Configure questionnaire fill out launcher
        binding.btnFillOutQuestionnaire.setOnClickListener {
            val intent = Intent(this, QuestionnaireView::class.java)
            editorLauncher.launch(intent)
        }

        // Configure back button
        binding.btnQuestionnaireClose.setOnClickListener { finish() }
    }
}