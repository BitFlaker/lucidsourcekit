package com.bitflaker.lucidsourcekit.main.questionnaire

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.Visibility
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.database.MainDatabase
import com.bitflaker.lucidsourcekit.databinding.ActivityQuestionnaireBinding
import com.bitflaker.lucidsourcekit.databinding.ActivityQuestionnaireOverviewBinding

class QuestionnaireOverviewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityQuestionnaireOverviewBinding
    private lateinit var adapter: RecyclerViewQuestionnaireOverview
    private lateinit var db: MainDatabase

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
        val questionnaires = db.questionnaireDao.getAllMore().blockingGet()

        // Set visibility of no questionnaires found notice
        val visibilityNoEntries = if (questionnaires.isEmpty()) View.VISIBLE else View.GONE
        binding.txtNoQuestionnairesTitle.visibility = visibilityNoEntries
        binding.txtNoQuestionnairesSubTitle.visibility = visibilityNoEntries

        // Configure questionnaire recycler view
        binding.rcvQuestionnaires.layoutManager = LinearLayoutManager(this)
        adapter = RecyclerViewQuestionnaireOverview(this, questionnaires)
        binding.rcvQuestionnaires.adapter = adapter

        // Configure back button
        binding.btnQuestionnaireClose.setOnClickListener { finish() }
    }
}