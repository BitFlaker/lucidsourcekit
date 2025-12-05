package com.bitflaker.lucidsourcekit.main.goals

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.database.MainDatabase
import com.bitflaker.lucidsourcekit.database.goals.entities.Goal
import com.bitflaker.lucidsourcekit.databinding.ActivityGoalsEditorBinding
import com.bitflaker.lucidsourcekit.databinding.SheetGoalsEditorBinding
import com.bitflaker.lucidsourcekit.utils.Tools
import com.bitflaker.lucidsourcekit.utils.attrColor
import com.bitflaker.lucidsourcekit.utils.attrColorStateList
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.newCoroutineContext
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class GoalsEditorView : AppCompatActivity() {
    private lateinit var binding: ActivityGoalsEditorBinding
    private lateinit var db: MainDatabase
    private lateinit var editGoalsAdapter: RecyclerViewAdapterEditGoals
    private var isInSelectionMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = MainDatabase.getInstance(this)
        binding = ActivityGoalsEditorBinding.inflate(layoutInflater)
        editGoalsAdapter = RecyclerViewAdapterEditGoals(this)
        setContentView(binding.root)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Set recycler view adapter
        binding.rcvEditGoals.setAdapter(editGoalsAdapter)
        binding.rcvEditGoals.setLayoutManager(LinearLayoutManager(this))

        // Turn `Add` button to a `Delete` button when multiple entries are being selected
        editGoalsAdapter.onMultiSelectEnterListener = {
            binding.btnAddGoal.backgroundTintList = attrColorStateList(R.attr.colorErrorContainer)
            binding.btnAddGoal.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_delete_24, theme))
            binding.btnAddGoal.imageTintList = attrColorStateList(R.attr.colorOnErrorContainer)
            isInSelectionMode = true
        }

        // Turn `Delete` button to an `Add` button when multiple entries are no longer being selected
        editGoalsAdapter.onMultiSelectExitListener = {
            binding.btnAddGoal.backgroundTintList = attrColorStateList(R.attr.colorPrimaryContainer)
            binding.btnAddGoal.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_round_add_24, theme))
            binding.btnAddGoal.imageTintList = attrColorStateList(R.attr.colorOnPrimaryContainer)
            isInSelectionMode = false
        }

        // Set click listener to show editor BottomSheet
        editGoalsAdapter.onEntryClickedListener = { goal, _ ->
            showGoalEditor(goal)
        }

        // Set flow to load and set entries for goals
        lifecycleScope.launch(Dispatchers.IO) {
            db.goalDao.getAll().collect {
                runOnUiThread {
                    editGoalsAdapter.setEntries(it)
                }
            }
        }

        // Set add goal / delete goals button handler
        binding.btnAddGoal.setOnClickListener {
            if (isInSelectionMode) {
                requestDeleteConfirmation()
            }
            else {
                showGoalEditor(null)
            }
        }
    }

    private fun requestDeleteConfirmation() {
        val selectedGoalIds = editGoalsAdapter.selectedGoalIds
        val selectedGoals = editGoalsAdapter.selectedGoals
        MaterialAlertDialogBuilder(this, R.style.Theme_LucidSourceKit_ThemedDialog)
            .setTitle(if (selectedGoalIds.size == 1) "Delete goal" else "Delete goals")
            .setMessage("Do you really want to delete " + (if (selectedGoalIds.size == 1) "this goal" else ("these " + selectedGoalIds.size + " selected goals")) + "?") // TODO: extract string resources
            .setPositiveButton(getResources().getString(R.string.yes)) { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    db.goalDao.deleteAll(selectedGoals)
                }
            }
            .setNegativeButton(getResources().getString(R.string.no), null)
            .show()
    }

    private fun showGoalEditor(goal: Goal?) {
        val isEditMode = goal != null
        val currentGoal = goal ?: Goal()
        val sheetBinding = SheetGoalsEditorBinding.inflate(layoutInflater)
        val editGoalSheet = BottomSheetDialog(this, R.style.BottomSheetDialogStyle)
        editGoalSheet.setContentView(sheetBinding.root)

        // Set goal details button
        sheetBinding.btnShowGoalDetails.visibility = if (isEditMode) View.VISIBLE else View.GONE
        sheetBinding.btnShowGoalDetails.setOnClickListener {
            if (goal == null) return@setOnClickListener
            lifecycleScope.launch(Dispatchers.IO) {
                val goalStats = db.shuffleHasGoalDao.getAchieveStatsOfGoal(goal.goalId)

                // Fill message template with statistics
                val message = if (goalStats.totalCount == 0) {
                    "This goal has not been on your schedule yet"
                } else {
                    val achieved = 100.0f * goalStats.achievedCount / goalStats.totalCount.toFloat()
                    resources.getString(R.string.goal_achievement_stats)
                        .replace("<COUNT>", goalStats.achievedCount.toString())
                        .replace("<TOTAL>", goalStats.totalCount.toString())
                        .replace("<PERCENTAGE>", String.format(Locale.ENGLISH, "%.1f", achieved))
                }

                // Display message box with the details
                runOnUiThread {
                    MaterialAlertDialogBuilder(this@GoalsEditorView, R.style.Theme_LucidSourceKit_ThemedDialog)
                        .setTitle("Goal details")
                        .setMessage(message)
                        .setPositiveButton(resources.getString(R.string.ok), null)
                        .show()
                }
            }
        }

        // Set correct visual values
        sheetBinding.txtGoalEditorTitle.text = if (isEditMode) "Edit goal" else "Add goal"
        sheetBinding.txtGoalDescription.setText(goal?.description)

        // Set slider handlers to show correct color
        changeSliderColor(sheetBinding.sldGoalDifficulty, sheetBinding.sldGoalDifficulty.value, false)
        sheetBinding.sldGoalDifficulty.addOnChangeListener(this::changeSliderColor)
        sheetBinding.sldGoalDifficulty.value = goal?.difficulty ?: 1f

        // Set handler for lock icon
        val isLocked = AtomicBoolean(goal?.difficultyLocked ?: false)
        setLockIcon(sheetBinding.btnToggleLockDifficulty, isLocked)
        sheetBinding.btnToggleLockDifficulty.setOnClickListener {
            isLocked.set(!isLocked.get())
            setLockIcon(sheetBinding.btnToggleLockDifficulty, isLocked)
        }

        // Set save goal listener
        sheetBinding.btnSaveGoal.setOnClickListener {
            if (sheetBinding.txtGoalDescription.text.isEmpty()) {
                return@setOnClickListener
            }
            currentGoal.description = sheetBinding.txtGoalDescription.text.toString()
            currentGoal.difficulty = sheetBinding.sldGoalDifficulty.value
            currentGoal.difficultyLocked = isLocked.get()
            lifecycleScope.launch(Dispatchers.IO) {
                if (isEditMode) {
                    db.goalDao.update(currentGoal)
                }
                else {
                    db.goalDao.insert(currentGoal)
                }
                runOnUiThread {
                    sheetBinding.sldGoalDifficulty.clearOnChangeListeners()
                    editGoalSheet.dismiss()
                }
            }
        }

        // Set delete goal handler
        sheetBinding.btnDeleteGoal.visibility = if (isEditMode) View.VISIBLE else View.GONE
        sheetBinding.btnDeleteGoal.setOnClickListener {
            if (goal == null) return@setOnClickListener
            MaterialAlertDialogBuilder(this@GoalsEditorView, R.style.Theme_LucidSourceKit_ThemedDialog)
                .setTitle("Delete goal")
                .setMessage("Do you really want to delete this goal?")
                .setPositiveButton(resources.getString(R.string.yes)) { _, _ ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        db.getGoalDao().delete(goal)
                        runOnUiThread {
                            editGoalSheet.dismiss()
                        }
                    }
                }
                .setNegativeButton(resources.getString(R.string.no), null)
                .show()
        }

        // Fix issue with EditText hidden behind soft-keyboard
        editGoalSheet.setOnShowListener { dialog ->
            Handler(Looper.getMainLooper()).postDelayed({
                val d = dialog as BottomSheetDialog
                val bottomSheetBehavior = BottomSheetBehavior.from(d.findViewById(R.id.design_bottom_sheet))
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED)
            }, 0)
        }

        editGoalSheet.show()
    }

    private fun setLockIcon(toggleLockDifficulty: ImageButton, isLocked: AtomicBoolean) {
        if (isLocked.get()) {
            toggleLockDifficulty.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_lock_24, theme))
            toggleLockDifficulty.imageTintList = attrColorStateList(R.attr.primaryTextColor)
            toggleLockDifficulty.backgroundTintList = attrColorStateList(R.attr.colorSurfaceContainerLow)
        } else {
            toggleLockDifficulty.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_lock_open_24, theme))
            toggleLockDifficulty.imageTintList = attrColorStateList(R.attr.secondaryTextColor)
            toggleLockDifficulty.backgroundTintList = attrColorStateList(R.attr.colorSurfaceContainerLow)
        }
    }

    fun changeSliderColor(slider: Slider, value: Float, @Suppress("unused") fromUser: Boolean) {
        @ColorInt val color = Tools.getColorAtGradientPosition(
            value,
            slider.valueFrom,
            slider.valueTo,
            attrColor(R.attr.colorSuccess),
            attrColor(R.attr.colorWarning),
            attrColor(R.attr.colorError)
        )

        // Turn color to active and inactive color state lists
        val activeTrackColor = ColorStateList.valueOf(color)
        val inactiveTrackColor = ColorStateList.valueOf(Tools.manipulateAlpha(color, 0.32f))

        // Set track and thumb color state lists
        slider.setTrackInactiveTintList(inactiveTrackColor)
        slider.setTrackActiveTintList(activeTrackColor)
        slider.setThumbTintList(activeTrackColor)
    }
}