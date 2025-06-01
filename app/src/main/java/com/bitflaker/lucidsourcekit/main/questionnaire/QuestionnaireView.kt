package com.bitflaker.lucidsourcekit.main.questionnaire

import android.os.Bundle
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
import com.bitflaker.lucidsourcekit.main.questionnaire.options.RangeOptions
import com.bitflaker.lucidsourcekit.main.questionnaire.options.ChoiceOptions
import com.bitflaker.lucidsourcekit.main.questionnaire.results.ControlResult
import com.bitflaker.lucidsourcekit.main.questionnaire.results.ControlResultBool
import com.bitflaker.lucidsourcekit.main.questionnaire.results.ControlResultMultiSelect
import com.bitflaker.lucidsourcekit.main.questionnaire.results.ControlResultRange
import com.bitflaker.lucidsourcekit.main.questionnaire.results.ControlResultSingleSelect
import com.bitflaker.lucidsourcekit.main.questionnaire.results.ControlResultText
import com.bitflaker.lucidsourcekit.utils.Tools
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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

        // Load all questionnaire data from the database
        db = MainDatabase.getInstance(this)
        questionnaire = db.questionnaireDao.getById(1).blockingGet()
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
        binding.btnQuestionnaireBack.setOnClickListener { questionIndex-- }
        binding.btnQuestionnaireNext.setOnClickListener {
            val holder = binding.rcvQuestionControl.findViewHolderForAdapterPosition(0) as RecyclerViewQuestionnaireControl.MainViewHolder
            results[questionIndex] = holder.result
            if (questionIndex == questions.size - 1) {
                saveQuestionnaireToDB()
                finish()
            }
            else {
                questionIndex++
            }
        }

        // Save the start timestamp to measure time taken for questionnaire
        fillOutStartTime = Calendar.getInstance().timeInMillis
    }

    private fun saveQuestionnaireToDB() {
        val duration = Calendar.getInstance().timeInMillis - fillOutStartTime
        val completed = CompletedQuestionnaire(questionnaire.id, duration, Calendar.getInstance().timeInMillis)
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
                right = Tools.dpToPx(this, 20.0),
                left = Tools.dpToPx(this, 16.0)
            )
        } else {
            binding.btnQuestionnaireNext.text = "Next"
            binding.btnQuestionnaireNext.icon = ResourcesCompat.getDrawable(resources, R.drawable.rounded_chevron_right_24, theme)
            binding.btnQuestionnaireNext.iconGravity = MaterialButton.ICON_GRAVITY_END
            binding.btnQuestionnaireNext.updatePadding(
                right = Tools.dpToPx(this, 16.0),
                left = Tools.dpToPx(this, 28.0)
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