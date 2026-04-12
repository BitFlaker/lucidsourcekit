package com.bitflaker.lucidsourcekit.main.export.templates.template1

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bitflaker.lucidsourcekit.databinding.ActivityExportTemplate1Binding
import com.bitflaker.lucidsourcekit.databinding.PdfExportOverlayBrandingBinding
import com.bitflaker.lucidsourcekit.databinding.PdfExportOverlayPageNumbersBinding
import com.bitflaker.lucidsourcekit.main.export.templates.data.ExportData
import com.bitflaker.lucidsourcekit.main.export.templates.data.JournalEntryExportData
import com.bitflaker.lucidsourcekit.main.export.templates.data.QuestionExportValueBool
import com.bitflaker.lucidsourcekit.main.export.templates.data.QuestionExportValueMultipleChoice
import com.bitflaker.lucidsourcekit.main.export.templates.data.QuestionExportValueRating
import com.bitflaker.lucidsourcekit.main.export.templates.data.QuestionExportValueText
import com.bitflaker.lucidsourcekit.main.export.templates.data.QuestionnaireExportData
import com.bitflaker.lucidsourcekit.main.export.templates.data.QuestionnaireExportQuestion
import com.bitflaker.lucidsourcekit.utils.export.ExportConfiguration
import com.bitflaker.lucidsourcekit.utils.export.PdfExportTemplate
import com.bitflaker.lucidsourcekit.utils.pdf.PdfGenerator
import com.bitflaker.lucidsourcekit.utils.pdf.layout.DocumentLayout
import com.bitflaker.lucidsourcekit.utils.pdf.layout.LayoutContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExportTemplate1(val activity: ComponentActivity) : PdfExportTemplate() {
    private val dateFormatter: DateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)
    private val weekdayFormatter = SimpleDateFormat("EEEE", Locale.getDefault())

    override suspend fun generatePreview(context: Context): Bitmap = withContext(Dispatchers.IO) {
        val layoutInflater = LayoutInflater.from(context)
        PdfGenerator.A4
            .layouts(generatePreviewData()) { data, layout ->
                loadLayout(context, layoutInflater, layout, data)
            }
            .overlay(PdfExportOverlayBrandingBinding.inflate(layoutInflater).root)
            .overlay(PdfExportOverlayPageNumbersBinding.inflate(layoutInflater).root)
            .generatePreview(context, 1)
    }

    override suspend fun generatePDF(config: ExportConfiguration) {
        val layoutInflater = LayoutInflater.from(activity)
        PdfGenerator.A4
            .layouts(loadExportData(activity, config)) { data, layout -> loadLayout(activity, layoutInflater, layout, data) }
            .overlay(PdfExportOverlayBrandingBinding.inflate(layoutInflater).root)
            .overlay(PdfExportOverlayPageNumbersBinding.inflate(layoutInflater).root)
            .generate(config.outputStream)
    }

    private fun loadLayout(
        context: Context,
        layoutInflater: LayoutInflater,
        layout: DocumentLayout,
        data: ExportData
    ): ViewGroup {
        val layoutContext = loadLayoutContext(context, layoutInflater, layout, data)
        val baseLayer = generateLayout(context, layoutInflater, layout, data, layoutContext.spanSizeLookupGrid)
        return baseLayer.root
    }

    fun generateLayout(
        context: Context,
        layoutInflater: LayoutInflater,
        layout: DocumentLayout,
        data: ExportData,
        gridSpanContext: Array<IntArray>? = null
    ): ActivityExportTemplate1Binding {
        val binding = ActivityExportTemplate1Binding.inflate(layoutInflater)

        binding.txtDate.text = dateFormatter.format(data.date)
        binding.txtSubDate.text = weekdayFormatter.format(data.date)

        binding.rcvJournalEntries.layoutManager = LinearLayoutManager(context)
        binding.rcvJournalEntries.adapter = RecyclerViewAdapterJournal(context, data.entries)

        binding.rcvQuestionnaires.layoutManager = LinearLayoutManager(context)
        binding.rcvQuestionnaires.adapter = RecyclerViewAdapterQuestionnaires(context, data.questionnaires, gridSpanContext, layout)

        return binding
    }

    private fun generatePreviewData(): List<ExportData> = listOf(
        ExportData(
            Date(),
            listOf(
                JournalEntryExportData(generateText(5), generateText(64)),
                JournalEntryExportData(generateText(4), generateText(40)),
            ),
            listOf(
                QuestionnaireExportData(
                    generateText(4),
                    listOf(
                        QuestionnaireExportQuestion(
                            generateText(3) + "?",
                            QuestionExportValueText(generateText(32))
                        ),
                        QuestionnaireExportQuestion(
                            generateText(2) + "?",
                            QuestionExportValueRating(0, 10, 6)
                        ),
                        QuestionnaireExportQuestion(
                            generateText(3) + "?",
                            QuestionExportValueBool(true)
                        ),
                        QuestionnaireExportQuestion(
                            generateText(2) + "?",
                            QuestionExportValueMultipleChoice(
                                arrayOf(
                                    "Apple",
                                    "Banana",
                                    "Pear",
                                    "Orange"
                                ), intArrayOf(0)
                            )
                        ),
                        QuestionnaireExportQuestion(
                            generateText(2) + "?",
                            QuestionExportValueMultipleChoice(
                                arrayOf(
                                    "Apple",
                                    "Banana",
                                    "Pear",
                                    "Orange"
                                ), intArrayOf(1, 3)
                            )
                        ),
                    )
                ),
                QuestionnaireExportData(
                    generateText(4),
                    listOf(
                        QuestionnaireExportQuestion(
                            generateText(4) + "?",
                            QuestionExportValueText(generateText(25))
                        ),
                        QuestionnaireExportQuestion(
                            generateText(6) + "?",
                            QuestionExportValueRating(0, 10, 6)
                        ),
                        QuestionnaireExportQuestion(
                            generateText(3) + "?",
                            QuestionExportValueBool(true)
                        ),
                        QuestionnaireExportQuestion(
                            generateText(5) + "?",
                            QuestionExportValueMultipleChoice(
                                arrayOf(
                                    "Apple",
                                    "Banana",
                                    "Pear",
                                    "Orange"
                                ), intArrayOf(0)
                            )
                        ),
                        QuestionnaireExportQuestion(
                            generateText(2) + "?",
                            QuestionExportValueMultipleChoice(
                                arrayOf(
                                    "Apple",
                                    "Banana",
                                    "Pear",
                                    "Orange"
                                ), intArrayOf(1, 3)
                            )
                        ),
                    )
                )
            )
        )
    )

    private fun loadLayoutContext(
        context: Context,
        layoutInflater: LayoutInflater,
        layout: DocumentLayout,
        data: ExportData
    ): LayoutContext {
        val baseLayer = generateLayout(context, layoutInflater, layout, data) // For some reason, the layer has to be measured and laid out separately from the final layout as otherwise it would simply ignore the span size lookup
        val layoutContext = LayoutContext()

        (context as Activity).runOnUiThread {
            layout.measureLayout(baseLayer.root)
        }

        layoutContext.spanSizeLookupGrid = (baseLayer.rcvQuestionnaires.adapter as RecyclerViewAdapterQuestionnaires).getGridSpanContext()

        return layoutContext
    }
}