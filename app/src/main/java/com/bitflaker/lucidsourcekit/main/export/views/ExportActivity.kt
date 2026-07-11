package com.bitflaker.lucidsourcekit.main.export.views

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.database.MainDatabase
import com.bitflaker.lucidsourcekit.databinding.ActivityExportBinding
import com.bitflaker.lucidsourcekit.main.export.templates.template1.ExportTemplate1
import com.bitflaker.lucidsourcekit.utils.attrColor
import com.bitflaker.lucidsourcekit.utils.dpToPx
import com.bitflaker.lucidsourcekit.utils.export.DefaultActivityLauncher
import com.bitflaker.lucidsourcekit.utils.export.ExportConfiguration
import com.bitflaker.lucidsourcekit.utils.generateFileName
import com.bitflaker.lucidsourcekit.utils.insetDefault
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.StreamCorruptedException
import java.text.DateFormat
import java.util.Calendar
import kotlin.coroutines.resume
import kotlin.math.min

// TODO: When checking/unchecking include xxx (e.g. journal entries), update previews with new settings

class ExportActivity : AppCompatActivity() {
    private lateinit var binding: ActivityExportBinding
    private lateinit var exportAdapter: RecyclerViewAdapterExportTemplates
    private lateinit var db: MainDatabase
    private var exportStartTS: Long = 0
    private var exportEndTS: Long = 0
    private val df = DateFormat.getDateInstance(DateFormat.MEDIUM)
    private val pathResolver = DefaultActivityLauncher(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityExportBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.main.insetDefault()

        db = MainDatabase.getInstance(this)
        binding.btnExportClose.setOnClickListener {
            finish()
        }

        // Configure the export template selector
        exportAdapter = RecyclerViewAdapterExportTemplates(this, arrayOf(
            ExportTemplate1(this),
            ExportTemplate1(this),
            ExportTemplate1(this),
        ))
        binding.rcvExportTemplates.adapter = exportAdapter
        binding.rcvExportTemplates.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )

        val context = this
        lifecycleScope.launch(Dispatchers.IO) {
            val cal = Calendar.getInstance()
            val journalTS = db.journalEntryDao.getOldestTime()
            val questionnaireTS = db.completedQuestionnaireDao.getOldestTime()

            // Get preselected timeframe
            exportEndTS = cal.timeInMillis
            exportStartTS = when {
                journalTS == -1L && questionnaireTS == -1L -> cal.timeInMillis
                journalTS == -1L -> questionnaireTS
                questionnaireTS == -1L -> journalTS
                else -> min(journalTS.toDouble(), questionnaireTS.toDouble()).toLong()
            }

            runOnUiThread {
                binding.txtDateStart.text = df.format(exportStartTS)
                binding.crdDateStart.setOnClickListener {
                    showDatePicker(context, exportStartTS) {
                        exportStartTS = it
                        binding.txtDateStart.text = df.format(exportStartTS)
                    }
                }
                binding.txtDateEnd.text = df.format(exportEndTS)
                binding.crdDateEnd.setOnClickListener {
                    showDatePicker(context, exportEndTS) {
                        exportEndTS = it
                        binding.txtDateEnd.text = df.format(exportEndTS)
                    }
                }
            }
        }

        binding.btnSaveExport.setOnClickListener {
            lifecycleScope.launch {
                val destination = withContext(Dispatchers.IO) {
                    getSaveFilePath()
                }

                if (destination == null) {
                    Toast.makeText(context, "Failed to get export file path", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val outputStream = contentResolver.openOutputStream(destination)
                    ?: throw StreamCorruptedException("Failed to open export stream")

                val dialog = showExportingDialog(context)

                withContext(Dispatchers.IO) {
                    exportAdapter.exporter.generatePDF(ExportConfiguration(
                        exportStartTS,
                        exportEndTS,
                        outputStream
                    ))
                }

                Toast.makeText(context, "Successfully exported to PDF", Toast.LENGTH_LONG).show()
                dialog.dismiss()
                finish()
            }
        }
    }

    private fun showExportingDialog(context: Activity): AlertDialog {
        val dp24 = 24.dpToPx
        return MaterialAlertDialogBuilder(context, R.style.Theme_LucidSourceKit_ThemedDialog)
                .setTitle("Exporting data")
                .setView(LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(dp24, dp24, dp24, dp24)
                    addView(CircularProgressIndicator(context).apply {
                        isIndeterminate = true
                    })
                    addView(TextView(context).apply {
                        text = "Exporting dream journal and questionnaire data, please be patient..."
                        setTextColor(context.attrColor(R.attr.secondaryTextColor))
                        gravity = Gravity.CENTER_VERTICAL
                        updatePadding(left = dp24)
                    })
                })
                .show()
    }

    private fun showDatePicker(context: Context, initialTimeStamp: Long, onDateSelected: ((Long) -> Unit)) {
        val cal = Calendar.getInstance()
        cal.timeInMillis = initialTimeStamp
        DatePickerDialog(context, { _, year, monthOfYear, dayOfMonth ->
                cal[Calendar.YEAR] = year
                cal[Calendar.MONTH] = monthOfYear
                cal[Calendar.DAY_OF_MONTH] = dayOfMonth
                onDateSelected.invoke(cal.timeInMillis)
            },
            cal[Calendar.YEAR],
            cal[Calendar.MONTH],
            cal[Calendar.DAY_OF_MONTH]
        ).show()
    }

    suspend fun getSaveFilePath(): Uri? {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            setType("application/pdf")
            putExtra(Intent.EXTRA_TITLE, generateFileName("LucidSourceKit_Export", "pdf"))
        }

        return suspendCancellableCoroutine { continuation ->
            pathResolver.launch(intent) { result ->
                continuation.resume(if (result.resultCode == RESULT_OK) {
                    result.data?.data
                } else {
                    null
                })
            }
        }
    }
}