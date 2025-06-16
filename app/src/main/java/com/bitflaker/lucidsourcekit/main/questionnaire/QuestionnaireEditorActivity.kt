package com.bitflaker.lucidsourcekit.main.questionnaire

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.database.MainDatabase
import com.bitflaker.lucidsourcekit.database.questionnaire.entities.CompletedQuestionnaire
import com.bitflaker.lucidsourcekit.database.questionnaire.entities.Question
import com.bitflaker.lucidsourcekit.database.questionnaire.entities.QuestionOptions
import com.bitflaker.lucidsourcekit.database.questionnaire.entities.Questionnaire
import com.bitflaker.lucidsourcekit.databinding.ActivityQuestionnaireEditorBinding
import com.bitflaker.lucidsourcekit.databinding.SheetColorPickerBinding
import com.bitflaker.lucidsourcekit.databinding.SheetQuestionEditorBinding
import com.bitflaker.lucidsourcekit.utils.Tools
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale

// TODO: Prompt to discard changes on phone back navigation press
// TODO: Make suspend and replace blocking actions for all questionnaire related db interactions

class QuestionnaireEditorActivity : AppCompatActivity(), SeekBar.OnSeekBarChangeListener {
    private lateinit var binding: ActivityQuestionnaireEditorBinding
    private var sBinding: SheetColorPickerBinding? = null
    private lateinit var db: MainDatabase
    private lateinit var adapter: RecyclerViewQuestions
    private lateinit var questionnaire: Questionnaire
    private lateinit var completedQuestionnaire: CompletedQuestionnaire
    private var completedQuestionnaireId: Int = -1
    private var questionnaireId: Int = -1
    private val colorPresets: List<Int> = listOf(
        "#449966".toColorInt(),
        "#4499aa".toColorInt(),
        "#3388cc".toColorInt(),
        "#aa77cc".toColorInt(),
        "#cc6677".toColorInt(),
        Color.TRANSPARENT,
    )
    private var lastSelectedCustomColor: Int = -1
    private var selectedColor: Int = -1

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
        db = MainDatabase.getInstance(this)

        // Try to get the questionnaire id from the intent extras, otherwise create empty questionnaire, then query all questions
        questionnaireId = intent.getIntExtra("QUESTIONNAIRE_ID", -1)
        completedQuestionnaireId = intent.getIntExtra("COMPLETED_QUESTIONNAIRE_ID", -1)
        val isViewer = completedQuestionnaireId != -1
        if (isViewer) {
            completedQuestionnaire = db.completedQuestionnaireDao.getById(completedQuestionnaireId).blockingGet()
            questionnaireId = completedQuestionnaire.questionnaireId
        }
        questionnaire = if (questionnaireId == -1) Questionnaire() else db.questionnaireDao.getById(questionnaireId).blockingGet()
        val allQuestions = if (completedQuestionnaireId == -1) { getEditorQuestions() } else { getViewerQuestions() }

        // Set general data
        binding.txtTitle.text = questionnaire.title
        binding.txtQuestionnaireName.setText(questionnaire.title)
        binding.txtQuestionnaireDescription.setText(questionnaire.description)

        // Configure recyclerview containing all questions for the current questionnaire
        adapter = RecyclerViewQuestions(this, allQuestions, isViewer)
        binding.rcvQuestions.layoutManager = LinearLayoutManager(this)
        binding.rcvQuestions.adapter = adapter

        // Configure for editor or viewer state
        if (isViewer) {
            binding.txtTitle.visibility = View.VISIBLE
            val color = questionnaire.colorCode
            if (color != null) {
                binding.imgSeparator.imageTintList = ColorStateList.valueOf(color.toColorInt())
                binding.vwColorDivider.visibility = View.VISIBLE
            }
            binding.btnAddQuestion.visibility = View.GONE
            binding.btnSaveQuestionnaire.visibility = View.GONE
            binding.llColorSelect.visibility = View.GONE
            binding.btnDeleteQuestionnaire.visibility = View.GONE
            binding.txtQuestionnaireName.visibility = View.GONE
            binding.txtQuestionnaireDescription.isEnabled = false
            binding.btnClose.setOnClickListener { finish() }
        }
        else {
            setupEditor()
        }
    }

    private fun getViewerQuestions(): MutableList<Question> {
        return db.questionnaireAnswerDao.getAll(completedQuestionnaireId).blockingGet().map { answer ->
            val question = db.questionDao.getById(answer.questionId).blockingGet().apply {
                value = answer.value
            }
            if (question.questionTypeId == QuestionnaireControlType.SingleSelect.ordinal || question.questionTypeId == QuestionnaireControlType.MultiSelect.ordinal) {
                question.options = db.selectedOptionsDao.getById(completedQuestionnaireId, answer.questionId).blockingGet().map {
                    db.questionOptionsDao.getById(answer.questionId, it.optionId).blockingGet()
                }.toMutableList()
            }
            question
        }.toMutableList()
    }

    private fun getEditorQuestions(): MutableList<Question> {
        // Get all the questions for the selected questionnaire and in case the question has
        // options, get all the options and save them in the question
        return if (questionnaireId == -1) mutableListOf() else db.questionDao.getAllForQuestionnaire(questionnaireId).blockingGet().map {
            if (it.questionTypeId == QuestionnaireControlType.SingleSelect.ordinal || it.questionTypeId == QuestionnaireControlType.MultiSelect.ordinal) {
                it.options = db.questionOptionsDao.getAllForQuestion(it.id).blockingGet().toMutableList()
            }
            it
        }.toMutableList()
    }

    private fun setupEditor() {
        touchHelperQuestions.attachToRecyclerView(binding.rcvQuestions)
        adapter.questionClickListener = { openQuestionEditor(it) }

        configurePresetColors()

        // Configure button to add question
        binding.btnAddQuestion.setOnClickListener {
            openQuestionEditor(-1)
        }

        // Configure button to save questionnaire
        binding.btnSaveQuestionnaire.setOnClickListener {
            if (binding.txtQuestionnaireName.text.isNotBlank() && binding.txtQuestionnaireDescription.text.isNotBlank() && adapter.questions.isNotEmpty()) {
                saveQuestionnaire()
            } else {
                showInfoDialog(
                    "Missing fields",
                    "Set a questionnaire title, description and add at least one question to save.\n(The color coding is optional)"
                )
            }
        }

        // Configure button to delete questionnaire
        binding.btnDeleteQuestionnaire.visibility = if (questionnaireId == -1) View.GONE else View.VISIBLE
        binding.btnDeleteQuestionnaire.setOnClickListener {
            MaterialAlertDialogBuilder(this, R.style.Theme_LucidSourceKit_ThemedDialog)
                .setTitle("Delete Questionnaire")
                .setMessage("Do you really want to delete this questionnaire? This cannot be undone")
                .setPositiveButton(resources.getString(R.string.yes)) { _, _ ->
                    // Delete question / Mark questions as hidden
                    val existingQuestions = db.questionDao.getAllForQuestionnaire(questionnaireId).blockingGet()
                    val questionIds = existingQuestions.filter { !it.isHidden }.map { it.id }
                    for (id in questionIds) {
                        val isReferenced = db.questionDao.isQuestionReferenced(id).blockingGet()
                        val hiddenQuestion = existingQuestions.single { it.id == id }
                        if (isReferenced) {
                            hiddenQuestion.isHidden = true
                            db.questionDao.update(hiddenQuestion).blockingSubscribe()
                        } else {
                            db.questionDao.delete(hiddenQuestion).blockingSubscribe()
                        }
                    }

                    // Delete questionnaire / Mark questionnaire as hidden
                    val isQuestionnaireReferenced =
                        db.questionnaireDao.isReferenced(questionnaireId).blockingGet()
                    if (isQuestionnaireReferenced) {
                        questionnaire.isHidden = true
                        db.questionnaireDao.update(questionnaire).blockingSubscribe()
                    } else {
                        db.questionnaireDao.delete(questionnaire).blockingSubscribe()
                    }

                    // Close editor window and notify about deleted questionnaire
                    val data = Intent()
                    data.putExtra("QUESTIONNAIRE_ID", questionnaireId)
                    data.putExtra("IS_DELETED", true)
                    setResult(RESULT_OK, data)
                    finish()
                }
                .setNegativeButton(resources.getString(R.string.no), null)
                .show()
        }

        // Configure button to discard changes and close current questionnaire
        binding.btnClose.setOnClickListener { promptDiscardChanges() }
    }

    private fun saveQuestionnaire() {
        // Save the questionnaire itself
        val isCreateMode = questionnaire.id == 0
        questionnaire.title = binding.txtQuestionnaireName.text.toString()
        questionnaire.description = binding.txtQuestionnaireDescription.text.toString()
        questionnaire.colorCode = if (selectedColor == -1) null else String.format("#%06X", (0xFFFFFF and selectedColor))
        if (isCreateMode) {
            questionnaireId = db.questionnaireDao.insert(questionnaire).blockingGet().toInt()
        }
        else {
            db.questionnaireDao.update(questionnaire).blockingSubscribe()
        }

        // Save the questions of the questionnaire
        val ids = mutableListOf<Int>()
        for (question in adapter.questions) {
            if (question.id > 0) {
                db.questionDao.update(question).blockingSubscribe()

                // Handle the editing of options (Preventing filled out questionnaires to change the option values)
                val options = question.options
                if (options != null) {
                    val optionIds = mutableListOf<Int>()
                    for ((i, option) in options.withIndex()) {
                        if (option.id == -1) {
                            option.questionId = question.id
                            option.id = db.questionOptionsDao.getNextId(question.id).blockingGet()
                            option.orderNr = i
                            db.questionOptionsDao.insert(option).blockingSubscribe()
                        }
                        else {
                            val originalOption = db.questionOptionsDao.getById(question.id, option.id).blockingGet()
                            if (originalOption.text == option.text) {
                                option.questionId = question.id
                                option.orderNr = i
                                db.questionOptionsDao.update(option).blockingSubscribe()
                            }
                            else {
                                val isReferenced = db.questionOptionsDao.isReferenced(question.id, option.id).blockingGet()
                                if (isReferenced) {
                                    originalOption.isHidden = true
                                    db.questionOptionsDao.update(option).blockingSubscribe()
                                }
                                else {
                                    db.questionOptionsDao.delete(originalOption).blockingSubscribe()
                                }
                                option.questionId = question.id
                                option.id = db.questionOptionsDao.getNextId(question.id).blockingGet()
                                option.orderNr = i
                                db.questionOptionsDao.insert(option).blockingSubscribe()
                            }
                        }
                        optionIds.add(option.id)
                    }

                    // Delete options / Mark options as hidden in case they have been removed
                    val existingOptions = db.questionOptionsDao.getAllForQuestion(question.id).blockingGet()
                    val removedOptions = existingOptions.filter { !it.isHidden }.map { it.id }.filter { !optionIds.contains(it) }
                    for (removedId in removedOptions) {
                        val isReferenced = db.questionOptionsDao.isReferenced(question.id, removedId).blockingGet()
                        val hiddenOption = existingOptions.single { it.id == removedId }
                        if (isReferenced) {
                            hiddenOption.isHidden = true
                            db.questionOptionsDao.update(hiddenOption).blockingSubscribe()
                        }
                        else {
                            db.questionOptionsDao.delete(hiddenOption).blockingSubscribe()
                        }
                    }
                }
            }
            else {
                question.id = 0
                question.questionnaireId = questionnaireId
                val id = db.questionDao.insert(question).blockingGet()
                question.id = id.toInt()

                // Add all options in case there are any
                val options = question.options
                if (options != null) {
                    for ((i, option) in options.withIndex()) {
                        option.questionId = question.id
                        option.id = i + 1
                        option.orderNr = i
                        db.questionOptionsDao.insert(option).blockingSubscribe()
                    }
                }
            }
            ids.add(question.id)
        }

        // Delete question / Mark questions as hidden in case they have been removed
        val existingQuestions = db.questionDao.getAllForQuestionnaire(questionnaireId).blockingGet()
        val removedQuestions = existingQuestions.filter { !it.isHidden }.map { it.id }.filter { !ids.contains(it) }
        for (removedId in removedQuestions) {
            val isReferenced = db.questionDao.isQuestionReferenced(removedId).blockingGet()
            val hiddenQuestion = existingQuestions.single { it.id == removedId }
            if (isReferenced) {
                hiddenQuestion.isHidden = true
                db.questionDao.update(hiddenQuestion).blockingSubscribe()
            }
            else {
                db.questionDao.delete(hiddenQuestion).blockingSubscribe()
            }
        }

        // Finish activity successfully
        val data = Intent()
        data.putExtra("QUESTIONNAIRE_ID", questionnaireId)
        data.putExtra("IS_CREATE_MODE", isCreateMode)
        setResult(RESULT_OK, data)
        finish()
    }

    private fun configurePresetColors() {
        binding.llColorSelect.removeAllViews()
        var selectedCard: MaterialCardView? = null
        var selectedColor: Int? = null
        val savedColor = questionnaire.colorCode
        for ((i, color) in colorPresets.withIndex()) {
            val card = generatePresetColor(color)
            binding.llColorSelect.addView(card)
            if (savedColor == String.format("#%06X", (0xFFFFFF and color))) {
                selectedCard = card
                selectedColor = color
            }
            if (i == colorPresets.size - 1 && selectedColor == null && savedColor != null) {
                selectedCard = card
                selectedColor = savedColor.toColorInt()
                lastSelectedCustomColor = selectedColor
            }
        }

        if (selectedColor != null) {
            this.selectedColor = selectedColor
            handleColorSelection(selectedCard, selectedColor, true)
        }
    }

    private fun generatePresetColor(color: Int): MaterialCardView {
        val dp32 = Tools.dpToPx(this, 32.0)
        val dp24 = Tools.dpToPx(this, 24.0)
        val dp22 = Tools.dpToPx(this, 18.0)
        val dp14 = Tools.dpToPx(this, 14.0)
        val isPicker = color == Color.TRANSPARENT
        val iconDrawable = if (isPicker) R.drawable.rounded_colorize_24 else R.drawable.ic_baseline_cross_24
        val iconSize = if (isPicker) dp22 else dp14
        val iconVisible = if (isPicker) View.VISIBLE else View.GONE
        val iconTint = Tools.getAttrColorStateList(if (isPicker) R.attr.colorOutline else R.attr.colorSurface, theme)

        val card = MaterialCardView(this)
        card.layoutParams = LinearLayout.LayoutParams(dp32, dp32)
        card.setCardBackgroundColor(color)
        card.radius = 999f
        card.strokeColor = card.cardBackgroundColor.defaultColor
        card.strokeWidth = 0

        // Create the color indicator for selected color
        val colorDot = View(this)
        colorDot.layoutParams = FrameLayout.LayoutParams(dp24, dp24).apply {
            gravity = Gravity.CENTER
        }
        colorDot.setBackgroundResource(R.drawable.rounded_rectangle_medium)
        colorDot.backgroundTintList = ColorStateList.valueOf(color)
        colorDot.visibility = View.GONE
        card.addView(colorDot)

        // Create the clear selection indicator for selected color
        val icon = ImageView(this)
        icon.layoutParams = FrameLayout.LayoutParams(iconSize, iconSize).apply {
            gravity = Gravity.CENTER
        }
        icon.setImageResource(iconDrawable)
        icon.imageTintList = iconTint
        icon.visibility = iconVisible
        card.addView(icon)

        // Create click listener to handle selection and deselection of color
        card.setOnClickListener {
            handleColorSelection(card, color)
        }

        return card
    }

    private fun handleColorSelection(card: MaterialCardView?, color: Int, suppressColorPicker: Boolean = false) {
        val iconTintSelected = Tools.getAttrColorStateList(R.attr.colorSurface, theme)
        val dp1 = Tools.dpToPx(this, 1.0)
        var anySelection = false
        for ((i, currentCard) in binding.llColorSelect.children.withIndex()) {
            if (currentCard !is MaterialCardView) continue
            val wasUnselected = currentCard.cardBackgroundColor == currentCard.strokeColorStateList
            val isChildPicker = i == binding.llColorSelect.childCount - 1
            val select = currentCard == card && (wasUnselected || isChildPicker)
            if (currentCard == card && isChildPicker) {
                currentCard.setStrokeColor(ColorStateList.valueOf(color))
                currentCard.children.filter { it !is ImageView }.single().backgroundTintList = ColorStateList.valueOf(color)
                if (!suppressColorPicker) {
                    openColorPickerDialog()
                }
            }

            val colorSurface = Tools.getAttrColor(R.attr.colorSurface, theme)
            currentCard.strokeWidth = if (select) dp1 else 0
            currentCard.setCardBackgroundColor(
                if (select) colorSurface else if (isChildPicker) colorSurface else currentCard.strokeColorStateList?.defaultColor ?: colorSurface
            )
            for (child in currentCard.children) {
                if (child is ImageView) {
                    child.imageTintList = if (select) iconTintSelected else if (isChildPicker) Tools.getAttrColorStateList(R.attr.colorOutline, theme) else iconTintSelected
                    if (isChildPicker) continue
                }
                child.visibility = if (select) View.VISIBLE else View.GONE
            }
            anySelection = anySelection || select
        }

        if (anySelection && color != Color.TRANSPARENT) {
            selectedColor = color
        } else if (!anySelection) {
            selectedColor = -1
        }
    }

    private fun openColorPickerDialog() {
        val bsd = BottomSheetDialog(this, R.style.BottomSheetDialogStyle)
        val sBinding = SheetColorPickerBinding.inflate(layoutInflater)
        bsd.setContentView(sBinding.root)

        sBinding.sbHue.progressDrawable = getHorizontalGradient(stops = generateHueColors(30f))
        sBinding.sbSaturation.setOnSeekBarChangeListener(this)
        sBinding.sbValue.setOnSeekBarChangeListener(this)
        sBinding.sbHue.setOnSeekBarChangeListener(this)
        sBinding.btnClearColor.setOnClickListener {
            handleColorSelection(null, -1)
            bsd.dismiss()
        }

        this.sBinding = sBinding
        if (lastSelectedCustomColor == -1) {
            sBinding.sbHue.progress = 13
            sBinding.sbSaturation.progress = 65
            sBinding.sbValue.progress = 85
        }
        else {
            val hsv = FloatArray(3)
            Color.colorToHSV(lastSelectedCustomColor, hsv)
            sBinding.sbHue.progress = (hsv[0] / 360f * 100).toInt()
            sBinding.sbSaturation.progress = (hsv[1] * 100).toInt()
            sBinding.sbValue.progress = (hsv[2] * 100).toInt()
        }

        bsd.show()
    }

    private fun generateHueColors(stepDegrees: Float): IntArray {
        return IntArray((360f / stepDegrees).toInt() + 1) { i ->
            Color.HSVToColor(floatArrayOf((i * stepDegrees) % 360f, 1f, 1f))
        }
    }

    private fun getHorizontalGradient(vararg stops: Int): GradientDrawable {
        val dp24 = Tools.dpToPx(baseContext, 24.0)
        return GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            stops
        ).apply {
            cornerRadius = dp24.toFloat()
            setSize(ViewGroup.LayoutParams.MATCH_PARENT, dp24)
        }
    }

    private fun openQuestionEditor(questionId: Int) {
        val bsd = BottomSheetDialog(this, R.style.BottomSheetDialogStyle)
        val sQBinding = SheetQuestionEditorBinding.inflate(layoutInflater)
        bsd.setContentView(sQBinding.root)
        val bs: FrameLayout = bsd.findViewById(com.google.android.material.R.id.design_bottom_sheet)!!

        // Configure click listeners for all questionnaire type buttons
        sQBinding.glQuestionTypes.children.filter { it is MaterialButton }.forEach { tbtn ->
            tbtn.setOnClickListener {
                sQBinding.glQuestionTypes.children.filter { btn ->
                    btn is MaterialButton
                }.forEach {
                    btn -> (btn as MaterialButton).isChecked = btn == it
                }
                loadOptions(tbtn, sQBinding, questionId) {
                    BottomSheetBehavior.from(bs).setState(BottomSheetBehavior.STATE_EXPANDED)
                }
            }
        }

        // Load and set the stored data from the database
        if (questionId != -1) {
            configureQuestionData(sQBinding, adapter.questions.single { it.id == questionId })
        }

        // Create listener to save question modification in memory
        sQBinding.btnSaveQuestion.setOnClickListener {
            if (saveQuestion(questionId, sQBinding)) {
                bsd.dismiss()
                return@setOnClickListener
            }
            showInfoDialog("Missing fields", "Fill out the question title, select a question type and add some options in case of a single or multi select type")
        }

        // Create listener to save question modification in memory
        sQBinding.btnDeleteQuestion.setOnClickListener {
            adapter.removeQuestion(questionId)
            bsd.dismiss()
        }

        // Load the correct options states depending on the selected question type
        val checkedType = sQBinding.glQuestionTypes.children.filter { (it is MaterialButton && it.isChecked) }.singleOrNull()
        loadOptions(checkedType, sQBinding, questionId) {
            BottomSheetBehavior.from(bs).setState(BottomSheetBehavior.STATE_EXPANDED)
        }

        bsd.show()

        // Focus question field and open soft keyboard
        if (questionId == -1) {
            sQBinding.txtQuestion.requestFocus()
            val imm = ContextCompat.getSystemService(this, InputMethodManager::class.java)
            Handler(Looper.getMainLooper()).postDelayed(kotlinx.coroutines.Runnable {
                imm!!.showSoftInput(sQBinding.txtQuestion, 0)
            }, 50)
        }
    }

    private fun saveQuestion(questionId: Int, sQBinding: SheetQuestionEditorBinding): Boolean {
        // Get the question text
        val question = sQBinding.txtQuestion.text.toString()
        if (question.isEmpty()) return false

        // Get the selected question type
        val checkedType = sQBinding.glQuestionTypes.children.filter { (it is MaterialButton && it.isChecked) }.singleOrNull() ?: return false
        val typeId = (checkedType.tag as String).toInt()

        // Get the correct adapter values based on the selected type
        val isSelect = typeId == QuestionnaireControlType.SingleSelect.ordinal || typeId == QuestionnaireControlType.MultiSelect.ordinal
        val isRange = typeId == QuestionnaireControlType.Range.ordinal
        val rangeAdapter = if (isRange && sQBinding.rcvQuestionOptions.adapter is RecyclerViewQuestionRange) sQBinding.rcvQuestionOptions.adapter as RecyclerViewQuestionRange else null
        val optionsAdapter = if (isSelect && sQBinding.rcvQuestionOptions.adapter is RecyclerViewQuestionOptions) sQBinding.rcvQuestionOptions.adapter as RecyclerViewQuestionOptions else null

        // Get the values
        val valueFrom = rangeAdapter?.item?.valueFrom
        val valueTo = rangeAdapter?.item?.valueTo
        val allOptions = optionsAdapter?.options ?: listOf()

        // Ensure options have been added when select type is used
        if (isSelect && allOptions.isEmpty()) return false

        // Store question in memory to be persisted later
        val id = if (questionId != -1) questionId else (adapter.questions.map { it.id }.filter { it < 0 }.minOrNull() ?: -1) - 1
        val questionToAdd = Question(id, question, typeId, questionnaireId, 0, valueFrom, valueTo, false, false).apply {
            options = allOptions.map { QuestionOptions(-1, it.id, it.text, 0, false, it.description) }.toMutableList()
        }

        // Add or update the question in the recyclerview
        if (adapter.questions.indexOfFirst { it.id == id } == -1) {
            adapter.addQuestion(questionToAdd)
        }
        else {
            adapter.updateQuestion(questionToAdd)
        }

        return true
    }

    private fun configureQuestionData(sQBinding: SheetQuestionEditorBinding, question: Question) {
        sQBinding.txtQuestion.setText(question.question)
        val selected = sQBinding.glQuestionTypes.children.single {
            (it.tag as String?)?.toInt() == question.questionTypeId
        } as MaterialButton
        selected.isChecked = true
    }

    private fun loadOptions(selectedType: View?, sQBinding: SheetQuestionEditorBinding, questionId: Int, optionFocusHandler: () -> Unit) {
        val tag = (selectedType?.tag as String?)?.toInt()
        val isSelect = tag == QuestionnaireControlType.SingleSelect.ordinal || tag == QuestionnaireControlType.MultiSelect.ordinal
        val hasOptions = tag == QuestionnaireControlType.Range.ordinal || isSelect
        sQBinding.txtQuestionOptions.visibility = if (hasOptions) View.VISIBLE else View.GONE
        sQBinding.rcvQuestionOptions.visibility = if (hasOptions) View.VISIBLE else View.GONE
        sQBinding.btnAddEntry.visibility = if (isSelect) View.VISIBLE else View.GONE
        if (isSelect) {
            runOnUiThread {
                // TODO: Maybe cache already configured but not stored options in memory so the
                //       progress won't be lost when switching between types
                val options = if (questionId == -1) mutableListOf() else adapter.questions.single { it.id == questionId }.options ?: mutableListOf()
                val adapter = RecyclerViewQuestionOptions(this, options)
                adapter.optionEditFocused = optionFocusHandler
                sQBinding.rcvQuestionOptions.itemAnimator = null
                sQBinding.rcvQuestionOptions.layoutManager = LinearLayoutManager(this)
                sQBinding.rcvQuestionOptions.adapter = adapter
                touchHelperOptions.attachToRecyclerView(sQBinding.rcvQuestionOptions)
                sQBinding.btnAddEntry.setOnClickListener {
                    adapter.addOption(QuestionOptions(questionId.coerceAtLeast(-1)))
                    sQBinding.nsvContent.fullScroll(View.FOCUS_DOWN)
                    sQBinding.nsvContent.scrollBy(0, 300)
                }
            }
        } else if (hasOptions) {
            runOnUiThread {
                val range = if (questionId == -1) Question("", QuestionnaireControlType.Range.ordinal, questionnaireId, 0, 10, false) else adapter.questions.single { it.id == questionId }
                val adapter = RecyclerViewQuestionRange(this, range)
                sQBinding.rcvQuestionOptions.itemAnimator = null
                sQBinding.rcvQuestionOptions.layoutManager = LinearLayoutManager(this)
                sQBinding.rcvQuestionOptions.adapter = adapter
                touchHelperOptions.attachToRecyclerView(null)
            }
        }
    }

    private val touchHelperOptions = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.START or ItemTouchHelper.END,
        0
    ) {
        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) { }
        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            val fromPosition: Int = viewHolder.bindingAdapterPosition
            val toPosition: Int = target.bindingAdapterPosition
            val adapter = recyclerView.adapter as RecyclerViewQuestionOptions
            adapter.swap(fromPosition, toPosition)
            return false
        }
    })

    private val touchHelperQuestions = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.START or ItemTouchHelper.END,
        0
    ) {
        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) { }
        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            val fromPosition: Int = viewHolder.bindingAdapterPosition
            val toPosition: Int = target.bindingAdapterPosition
            val adapter = recyclerView.adapter as RecyclerViewQuestions
            adapter.swap(fromPosition, toPosition)
            return false
        }
    })

    private fun promptDiscardChanges() {
        MaterialAlertDialogBuilder(this, R.style.Theme_LucidSourceKit_ThemedDialog)
            .setTitle("Discard changes")
            .setMessage("Do you really want to discard all changes")
            .setPositiveButton(resources.getString(R.string.yes)) { _, _ -> finish() }
            .setNegativeButton(resources.getString(R.string.no), null)
            .show()
    }

    private fun showInfoDialog(title: String, message: String) {
        MaterialAlertDialogBuilder(this, R.style.Theme_LucidSourceKit_ThemedDialog)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(resources.getString(R.string.ok), null)
            .show()
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) { }
    override fun onStopTrackingTouch(seekBar: SeekBar?) { }
    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        val sBinding = sBinding ?: return
        val degrees = 360 * (sBinding.sbHue.progress / 100.0f)

        // Generate saturation progress background
        val startSaturation = Color.HSVToColor(floatArrayOf(degrees, 0f, sBinding.sbValue.progress / 100f))
        val endSaturation = Color.HSVToColor(floatArrayOf(degrees, 1f, sBinding.sbValue.progress / 100f))
        sBinding.sbSaturation.progressDrawable = getHorizontalGradient(startSaturation, endSaturation)

        // Generate brightness progress background
        val startValue = Color.HSVToColor(floatArrayOf(degrees, sBinding.sbSaturation.progress / 100f, 0f))
        val endValue = Color.HSVToColor(floatArrayOf(degrees, sBinding.sbSaturation.progress / 100f, 1f))
        sBinding.sbValue.progressDrawable = getHorizontalGradient(startValue, endValue)

        // Update preview values
        updatePreview()
    }

    private fun updatePreview() {
        val sBinding = sBinding ?: return
        val degrees = 360 * (sBinding.sbHue.progress / 100.0f)
        val currentColor = Color.HSVToColor(
            floatArrayOf(
                degrees,
                sBinding.sbSaturation.progress / 100.0f,
                sBinding.sbValue.progress / 100.0f
            )
        )
        lastSelectedCustomColor = currentColor
        selectedColor = lastSelectedCustomColor
        sBinding.vwColorPreview.backgroundTintList = ColorStateList.valueOf(currentColor)
        sBinding.txtValueHex.text = String.format("#%06X", (0xFFFFFF and currentColor))
        sBinding.txtValueRgb.text = String.format(
            Locale.getDefault(),
            "%d, %d, %d",
            Color.red(currentColor),
            Color.green(currentColor),
            Color.blue(currentColor)
        )

        val picker = binding.llColorSelect.children.last()
        if (picker !is MaterialCardView) return
        picker.setStrokeColor(ColorStateList.valueOf(currentColor))
        picker.children.filter { it !is ImageView }.single().backgroundTintList = ColorStateList.valueOf(currentColor)
    }
}