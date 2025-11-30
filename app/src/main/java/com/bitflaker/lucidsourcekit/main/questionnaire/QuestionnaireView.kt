package com.bitflaker.lucidsourcekit.main.questionnaire

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.DatePicker
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.database.MainDatabase
import com.bitflaker.lucidsourcekit.database.questionnaire.entities.CompletedQuestionnaire
import com.bitflaker.lucidsourcekit.database.questionnaire.entities.Question
import com.bitflaker.lucidsourcekit.database.questionnaire.entities.QuestionOptions
import com.bitflaker.lucidsourcekit.database.questionnaire.entities.Questionnaire
import com.bitflaker.lucidsourcekit.database.questionnaire.entities.QuestionnaireAnswer
import com.bitflaker.lucidsourcekit.database.questionnaire.entities.SelectedOptions
import com.bitflaker.lucidsourcekit.databinding.ActivityQuestionnaireBinding
import com.bitflaker.lucidsourcekit.main.questionnaire.options.ChoiceOptions
import com.bitflaker.lucidsourcekit.main.questionnaire.options.RangeOptions
import com.bitflaker.lucidsourcekit.main.questionnaire.results.ControlResult
import com.bitflaker.lucidsourcekit.main.questionnaire.results.ControlResultBool
import com.bitflaker.lucidsourcekit.main.questionnaire.results.ControlResultMultiSelect
import com.bitflaker.lucidsourcekit.main.questionnaire.results.ControlResultRange
import com.bitflaker.lucidsourcekit.main.questionnaire.results.ControlResultSingleSelect
import com.bitflaker.lucidsourcekit.main.questionnaire.results.ControlResultText
import com.bitflaker.lucidsourcekit.utils.Tools
import com.bitflaker.lucidsourcekit.utils.dpToPx
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.DateFormat
import java.util.Calendar

// TODO: Prompt to discard changes on phone back navigation press
// TODO: Make suspend and replace blocking actions

class QuestionnaireView : AppCompatActivity() {
    private lateinit var db: MainDatabase
    private lateinit var binding: ActivityQuestionnaireBinding
    private lateinit var questionnaire: Questionnaire
    private lateinit var questions: List<Question>
    private lateinit var adapter: RecyclerViewQuestionnaireControl
    private lateinit var results: Array<ControlResult?>
    private var specificDate: Long = 0
    private var fillOutStartTime: Long = 0
    private var questionIndex: Int = 0
        set(value) {
            field = value
            runOnUiThread {
                adapter.questionContext = showCurrentQuestion()
            }
        }

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

        specificDate = intent.getLongExtra("USE_SPECIFIC_DATE", 0)

        // Load all questionnaire data from the database
        db = MainDatabase.getInstance(this)
        binding.btnQuestionnaireClose.setOnClickListener { finish() }

        // Setup views for selecting questionnaire to fill out
        binding.spQuestionnaireProgress.visibility = View.GONE
        binding.svQuestionViewer.visibility = View.GONE
        binding.btnQuestionnaireNext.visibility = View.GONE
        binding.btnQuestionnaireBack.visibility = View.GONE
        binding.rcvQuestionnaires.visibility = View.VISIBLE
        binding.btnQuestionnaireDate.visibility = View.VISIBLE
        binding.txtQuestionnaireTitle.text = "Questionnaires"

        // Setup date picker for questionnaire timestamp
        val date = if (specificDate == 0L) Calendar.getInstance() else Tools.calendarFromMillis(specificDate)
        binding.btnQuestionnaireDate.text = DateFormat.getDateInstance(DateFormat.SHORT).format(date.time)
        binding.btnQuestionnaireDate.setOnClickListener {
            val currentDate = if (specificDate == 0L) Calendar.getInstance() else Tools.calendarFromMillis(specificDate)
            DatePickerDialog(this, { _: DatePicker?, year: Int, monthOfYear: Int, dayOfMonth: Int ->
                    currentDate[Calendar.YEAR] = year
                    currentDate[Calendar.MONTH] = monthOfYear
                    currentDate[Calendar.DAY_OF_MONTH] = dayOfMonth
                    specificDate = Tools.getMidnightTime(currentDate.timeInMillis)
                    binding.btnQuestionnaireDate.text = DateFormat.getDateInstance(DateFormat.SHORT).format(currentDate.time)
                },
                currentDate[Calendar.YEAR],
                currentDate[Calendar.MONTH],
                currentDate[Calendar.DAY_OF_MONTH]
            ).show()
        }

        // Configure recyclerview for selecting questionnaire to fill out
        val questionnaires = db.questionnaireDao.getAllDetails().blockingGet().toMutableList()
        binding.rcvQuestionnaires.layoutManager = LinearLayoutManager(this)
        val questionnaireAdapter = RecyclerViewQuestionnaireOverview(this, questionnaires)
        questionnaireAdapter.onQuestionnaireClickListener = { id ->
            binding.spQuestionnaireProgress.visibility = View.VISIBLE
            binding.svQuestionViewer.visibility = View.VISIBLE
            binding.btnQuestionnaireNext.visibility = View.VISIBLE
            binding.btnQuestionnaireBack.visibility = View.VISIBLE
            binding.rcvQuestionnaires.visibility = View.GONE
            binding.btnQuestionnaireDate.visibility = View.GONE
            showQuestionnaireEditor(id)
        }
        binding.rcvQuestionnaires.adapter = questionnaireAdapter
    }

    private fun showQuestionnaireEditor(questionnaireId: Int) {
        questionnaire = db.questionnaireDao.getById(questionnaireId).blockingGet()
        questions = db.questionDao.getAllForQuestionnaire(questionnaire.id).blockingGet()
        binding.txtQuestionnaireTitle.text = questionnaire.title
        binding.spQuestionnaireProgress.totalStepCount = questions.size
        results = Array(questions.size) { null }

        // Setup RecyclerView for options
        val ctx = showCurrentQuestion()
        adapter = RecyclerViewQuestionnaireControl(this, ctx)
        adapter.setResultListener { binding.btnQuestionnaireNext.isEnabled = it }
        binding.rcvQuestionControl.adapter = adapter
        binding.rcvQuestionControl.layoutManager = LinearLayoutManager(this)

        // Setup button listeners for Next, Back and close questionnaire
        binding.btnQuestionnaireClose.setOnClickListener { promptDiscardChanges() }
        binding.btnQuestionnaireBack.setOnClickListener {
            storeResult()
            questionIndex--
        }
        binding.btnQuestionnaireNext.setOnClickListener {
            storeResult()
            if (questionIndex == questions.size - 1) {
                val id = saveQuestionnaireToDB()
                val data = Intent()
                data.putExtra("COMPLETED_QUESTIONNAIRE_ID", id)
                setResult(RESULT_OK, data)
                finish()
            } else {
                questionIndex++
            }
        }

        // Save the start timestamp to measure time taken for questionnaire
        fillOutStartTime = Calendar.getInstance().timeInMillis
    }

    private fun storeResult() {
        val holder = binding.rcvQuestionControl.findViewHolderForAdapterPosition(0) as RecyclerViewQuestionnaireControl.MainViewHolder
        results[questionIndex] = holder.result
    }

    private fun saveQuestionnaireToDB(): Int {
        var completedTime = Calendar.getInstance().timeInMillis
        val duration = completedTime - fillOutStartTime
        if (specificDate > 0) {
            completedTime = specificDate + Tools.getTimeOfDayMillis(completedTime)
        }
        val completed = CompletedQuestionnaire(questionnaire.id, duration, completedTime)
        val id = db.completedQuestionnaireDao.insert(completed).blockingGet().toInt()
        for (i in questions.indices) {
            val question = questions[i]
            val result = results[i]

            // Get the value for the 'value' field in the questionnaire answer table
            val value = when (question.questionTypeId) {
                QuestionnaireControlType.Text.ordinal -> (result as? ControlResultText)?.result
                QuestionnaireControlType.Bool.ordinal -> (result as? ControlResultBool)?.result.toString()
                QuestionnaireControlType.Range.ordinal -> (result as? ControlResultRange)?.result.toString()
                else -> null
            }

            // Save the answer to the database
            val answer = QuestionnaireAnswer(id, question.id, value)
            db.questionnaireAnswerDao.insert(answer).blockingSubscribe()

            // In case the type was a selection type, store the selected value(s) to the database as well
            when (question.questionTypeId) {
                QuestionnaireControlType.SingleSelect.ordinal -> {
                    val selectedId = (result as? ControlResultSingleSelect)?.result ?: throw IllegalStateException("Single select must be set when saving")
                    val selected = SelectedOptions(id, question.id, selectedId)
                    db.selectedOptionsDao.insert(selected).blockingSubscribe()
                }
                QuestionnaireControlType.MultiSelect.ordinal -> {
                    val selectedIds = (result as? ControlResultMultiSelect)?.result ?: throw IllegalStateException("Multi select must be set when saving")
                    db.selectedOptionsDao.insertAll(selectedIds.map {
                        SelectedOptions(id, question.id, it)
                    }).blockingSubscribe()
                }
            }
        }
        return id
    }

    private fun showCurrentQuestion(): QuestionnaireControlContext {
        val current = questions[questionIndex]
        val result = results[questionIndex]

        // Show current question and update progress
        binding.spQuestionnaireProgress.currentStepCount = questionIndex + 1
        binding.txtQuestionHeading.text = "Question #%02d".format(questionIndex + 1)
        binding.txtQuestionTitle.text = current.question

        // Set the button states for the current question
        binding.btnQuestionnaireBack.isEnabled = questionIndex > 0
        binding.btnQuestionnaireNext.isEnabled = result != null || current.questionTypeId == 0 || current.questionTypeId == 4
        setNextButtonState()

        // Parse the question type
        val type = QuestionnaireControlType.entries.getOrNull(current.questionTypeId)
            ?: throw IllegalArgumentException("Unknown question type ${current.questionTypeId}")

        // Get the correct options for the current question type
        val options = when (type) {
            QuestionnaireControlType.Range -> RangeOptions(current.valueFrom!!, current.valueTo!!)
            QuestionnaireControlType.SingleSelect -> ChoiceOptions(getOptions(current.id))
            QuestionnaireControlType.MultiSelect -> ChoiceOptions(getOptions(current.id))
            else -> null
        }

        // Return the final context for the current question
        return QuestionnaireControlContext(type, options, result)
    }

    private fun setNextButtonState() {
        if (questionIndex == questions.size - 1) {
            binding.btnQuestionnaireNext.text = "Finish"
            binding.btnQuestionnaireNext.icon = ResourcesCompat.getDrawable(resources, R.drawable.rounded_check_18, theme)
            binding.btnQuestionnaireNext.iconGravity = MaterialButton.ICON_GRAVITY_START
            binding.btnQuestionnaireNext.updatePadding(
                right = 20.dpToPx,
                left = 16.dpToPx
            )
        } else {
            binding.btnQuestionnaireNext.text = "Next"
            binding.btnQuestionnaireNext.icon = ResourcesCompat.getDrawable(resources, R.drawable.rounded_chevron_right_24, theme)
            binding.btnQuestionnaireNext.iconGravity = MaterialButton.ICON_GRAVITY_END
            binding.btnQuestionnaireNext.updatePadding(
                right = 16.dpToPx,
                left = 28.dpToPx
            )
        }
    }

    private fun getOptions(questionId: Int): List<QuestionOptions> {
        return db.questionOptionsDao.getAllForQuestion(questionId).blockingGet()
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