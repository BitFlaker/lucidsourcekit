package com.bitflaker.lucidsourcekit.main.notification.views

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.NumberPicker
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.database.MainDatabase
import com.bitflaker.lucidsourcekit.database.notifications.entities.NotificationMessage
import com.bitflaker.lucidsourcekit.databinding.ActivityNotificationMessageEditorBinding
import com.bitflaker.lucidsourcekit.databinding.SheetNotificationMessageBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class NotificationManagerEditorView : AppCompatActivity() {
    private lateinit var binding: ActivityNotificationMessageEditorBinding
    private lateinit var db: MainDatabase
    private var customNotificationWeightValue = 0
    private lateinit var rcvaNotificationEditor: RecyclerViewAdapterNotificationEditor
    private lateinit var notificationCategoryId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationMessageEditorBinding.inflate(layoutInflater)
        db = MainDatabase.getInstance(this)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Ensure category was provided, otherwise return cancelled result
        val categoryId = intent.getStringExtra("CATEGORY_ID")
        if (categoryId.isNullOrEmpty()) {
            setResult(RESULT_CANCELED, Intent().apply {
                putExtra("CATEGORY_ID", "")
            })
            finish()
            return
        }

        // Set result to valid data
        notificationCategoryId = categoryId
        setResult(RESULT_OK, Intent().apply {
            putExtra("CATEGORY_ID", intent.getStringExtra("CATEGORY_ID"))
        })

        // Set recycler view message handler
        lifecycleScope.launch(Dispatchers.IO) {
            val notificationMessages = db.notificationMessageDao.getAllOfCategory(notificationCategoryId)
            runOnUiThread {
                rcvaNotificationEditor = RecyclerViewAdapterNotificationEditor(this@NotificationManagerEditorView, notificationMessages)
                rcvaNotificationEditor.messageClickedListener = ::createAndShowBottomSheetConfigurator

                // The following code is because of the issue of moving the first object causes a weird scroll
                // This workaround makes it look a little better but is not perfect still
                rcvaNotificationEditor.registerAdapterDataObserver(object : AdapterDataObserver() {
                    override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                        if (fromPosition == 0 || toPosition == 0) {
                            binding.rcvNotificationMessages.scrollToPosition(0)
                        }
                    }
                })
                binding.rcvNotificationMessages.setAdapter(rcvaNotificationEditor)
                binding.rcvNotificationMessages.setLayoutManager(LinearLayoutManager(this@NotificationManagerEditorView))
                binding.btnAddNotificationMessage.setOnClickListener {
                    createAndShowBottomSheetConfigurator(null)
                }
            }
        }
    }

    private fun getSelectedWeight(customNotificationWeightChips: Array<Chip>): Int {
        for (chip in customNotificationWeightChips) {
            if (chip.isChecked) {
                return chip.getText().toString().toInt()
            }
        }
        return 1
    }

    private fun createAndShowBottomSheetConfigurator(message: NotificationMessage?) {
        val sBinding = SheetNotificationMessageBinding.inflate(layoutInflater)
        val bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialogStyle)
        bottomSheetDialog.setContentView(sBinding.root)

        val customNotificationWeightChips = arrayOf(
            sBinding.chpNotificationWeight1,
            sBinding.chpNotificationWeight2,
            sBinding.chpNotificationWeight3,
            sBinding.chpNotificationWeight4
        )

        // Setting default values
        customNotificationWeightValue = 6

        // Set handler to show custom weight options
        sBinding.chkCustomNotificationWeight.setOnCheckedChangeListener { _, checked ->
            sBinding.chpGrpNotificationWeight.visibility = if (checked) View.VISIBLE else View.GONE
        }

        // Set handler for custom weight value option
        sBinding.chpNotificationWeightCustom.setOnClickListener { e: View? ->
            val numberPicker = NumberPicker(this).apply {
                setMaxValue(99)
                setMinValue(1)
                value = customNotificationWeightValue
            }

            MaterialAlertDialogBuilder(this)
                .setView(numberPicker)
                .setTitle("Daily notifications")
                .setMessage("Choose the amount of notifications to be sent daily")
                .setPositiveButton(getResources().getString(R.string.ok)) { _, _ ->
                    customNotificationWeightValue = numberPicker.value
                    sBinding.chpNotificationWeightCustom.text = String.format(Locale.getDefault(), "Custom (%d)", customNotificationWeightValue)
                }
                .setNegativeButton(getResources().getString(R.string.cancel), null)
                .create()
                .show()
        }

        // Set handlers for cancelling and saving
        sBinding.btnCancel.setOnClickListener { bottomSheetDialog.cancel() }
        sBinding.btnSave.setOnClickListener {
            val weight = if (sBinding.chpNotificationWeightCustom.isChecked) customNotificationWeightValue else getSelectedWeight(customNotificationWeightChips)
            val value = sBinding.txtCustomNotificationMessage.getText().toString()
            val isAdded = message == null
            val message = message ?: NotificationMessage(notificationCategoryId)

            lifecycleScope.launch(Dispatchers.IO) {
                message.weight = weight
                message.message = value

                if (isAdded) {
                    message.setId(db.notificationMessageDao.insert(message).toInt())
                    runOnUiThread { rcvaNotificationEditor.notifyMessageAdded(message) }
                } else {
                    db.notificationMessageDao.update(message)
                    runOnUiThread { rcvaNotificationEditor.notifyMessageChanged(message) }
                }

                runOnUiThread(bottomSheetDialog::dismiss)
            }
        }

        // Set the current values of an existing message, otherwise focus message text field
        if (message != null) {
            sBinding.txtCustomNotificationMessage.setText(message.message)
            when (message.weight) {
                1 -> sBinding.chkCustomNotificationWeight.setChecked(false)
                2, 3, 4, 5 -> {
                    sBinding.chkCustomNotificationWeight.setChecked(true)
                    customNotificationWeightChips[message.weight - 2].isChecked = true
                }
                else -> {
                    sBinding.chkCustomNotificationWeight.setChecked(true)
                    sBinding.chpNotificationWeightCustom.isChecked = true
                    customNotificationWeightValue = message.weight
                    sBinding.chpNotificationWeightCustom.text = String.format(Locale.getDefault(), "Custom (%d)", customNotificationWeightValue)
                }
            }
        } else {
            sBinding.txtCustomNotificationMessage.requestFocus()
        }

        // Fix issue with EditText hidden behind soft-keyboard
        bottomSheetDialog.setOnShowListener { dialogInterface ->
            Handler(Looper.getMainLooper()).postDelayed({
                val dialog = dialogInterface as BottomSheetDialog
                val bottomSheetBehavior = BottomSheetBehavior.from(dialog.findViewById(R.id.design_bottom_sheet))
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED)
            }, 0)
        }

        // Finally show the editor dialog
        bottomSheetDialog.show()
    }
}