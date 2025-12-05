package com.bitflaker.lucidsourcekit.utils.export

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.view.Gravity
import android.webkit.WebView
import android.widget.DatePicker
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.updatePadding
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.main.dreamjournal.rating.DreamTypes
import com.bitflaker.lucidsourcekit.database.MainDatabase
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.DreamClarity
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.DreamMood
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.SleepQuality
import com.bitflaker.lucidsourcekit.database.questionnaire.entities.QuestionOptions
import com.bitflaker.lucidsourcekit.main.questionnaire.QuestionnaireControlType
import com.bitflaker.lucidsourcekit.utils.attrColor
import com.bitflaker.lucidsourcekit.utils.await
import com.bitflaker.lucidsourcekit.utils.dpToPx
import com.bitflaker.lucidsourcekit.utils.generateFileName
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import java.text.DateFormat
import java.util.Calendar
import kotlin.math.min

class ExportDialog {
    companion object {
        private val df = DateFormat.getDateInstance(DateFormat.MEDIUM)
        private val dfShort = DateFormat.getDateInstance(DateFormat.SHORT)
        private val tfShort = DateFormat.getTimeInstance(DateFormat.SHORT)

        suspend fun promptExportData(context: Activity, launcher: SimpleActivityLauncher) {
            if (context !is LifecycleOwner) throw IllegalArgumentException("context has to implement LifecycleOwner for exporting data")
            val cal = Calendar.getInstance()
            val dp24 = 24.dpToPx
            val dp12 = 12.dpToPx

            val db = MainDatabase.getInstance(context)
            val journalTS = db.journalEntryDao.getOldestTime()
            val questionnaireTS = db.completedQuestionnaireDao.getOldestTime().blockingGet()

            var fromTS = when {
                journalTS == -1L && questionnaireTS == -1L -> cal.timeInMillis
                journalTS == -1L -> questionnaireTS
                questionnaireTS == -1L -> journalTS
                else -> min(journalTS.toDouble(), questionnaireTS.toDouble()).toLong()
            }
            var toTS = cal.timeInMillis

            // Create container
            val contentContainer = LinearLayout(context)
            contentContainer.setPadding(dp24, dp12, dp24, dp12)
            contentContainer.orientation = LinearLayout.VERTICAL

            // Configure message view
            val message = TextView(context)
            message.setTextColor(context.attrColor(R.attr.secondaryTextColor))
            message.setPadding(0, 0, 0, dp24)
            message.text = "Export dream journals and questionnaires to an external file. To export to PDF, use the default Android PDF printer"
            contentContainer.addView(message)

            // Configure date picker for `Date from`
            val dateFrom = MaterialButton(context)
            dateFrom.text = "From " + df.format(fromTS)
            dateFrom.setOnClickListener {
                showDatePicker(context, fromTS) {
                    fromTS = it
                    dateFrom.text = "From " + df.format(fromTS)
                }
            }
            contentContainer.addView(dateFrom)

            // Configure date picker for `Date to`
            val dateTo = MaterialButton(context)
            dateTo.text = "To " + df.format(toTS)
            dateTo.setOnClickListener {
                showDatePicker(context, toTS) {
                    toTS = it
                    dateTo.text = "To " + df.format(toTS)
                }
            }
            contentContainer.addView(dateTo)

            // Configure drop down for export type
            val exportType = Spinner(context)
            exportType.setPadding(0, dp12, 0, 0)
            exportType.setPopupBackgroundResource(R.drawable.popup_menu_background_dark)
            val adapter = ExportTypeAdapter(context)
            exportType.adapter = adapter
            exportType.setSelection(0)
            contentContainer.addView(exportType)

            // Build and show dialog
            context.runOnUiThread {
                MaterialAlertDialogBuilder(context, R.style.Theme_LucidSourceKit_ThemedDialog)
                    .setTitle("Export data")
                    .setView(contentContainer)
                    .setPositiveButton("Export") { _: DialogInterface?, _: Int ->
                        val exportingDialog = showExportingDialog(context)
                        context.lifecycleScope.launch(Dispatchers.IO) {
                            async {
                                performExport(context, launcher, fromTS, toTS, adapter.getItem(exportType.selectedItemPosition))
                            }.await()
                            exportingDialog.dismiss()
                        }
                    }
                    .setNegativeButton(context.resources.getString(R.string.cancel), null)
                    .show()
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

        private suspend fun performExport(context: Activity, launcher: SimpleActivityLauncher, fromTimestamp: Long, toTimestamp: Long, exportType: ExportTypes?) {
            if (exportType == null) return

            // Load all data from db and generate Markdown from that data
            val markDown = buildMarkDown(context, fromTimestamp, toTimestamp)

            // In case of option Markdown, stop here and write data to file
            if (exportType == ExportTypes.Markdown) {
                // TODO: Clean placeholders
                saveFile(launcher, context, generateFileName("LucidSourceKit_DataExport", "md"), "text/markdown", markDown)
                return
            }

            // Parse Markdown to HTML
            val flavour = GFMFlavourDescriptor()
            val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(markDown)
            val html = HtmlGenerator(markDown, parsedTree, flavour).generateHtml()
            val finalHtml = MarkDownEscaper.finalizeEscapedHtml(html)

            // Prepend some custom HTML for general optimizations (especially for printing optimizations)
            val printOptimizedHtml = """<html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        @media print {
                            @page {
                                margin: 14mm 20mm;
                                counter-increment: page;
                                
                                @bottom-right {
                                    content: ${"\"Page \" counter(page) \" of \" counter(pages)"};
                                }
                            }
                        }
                        h1, h2, h3, h4, hr {
                            page-break-after: avoid;
                        }
                        h4 {
                            margin-bottom: -6px;
                        }
                        table {
                            margin-bottom: 32px;
                            border: 1px solid black;
                            border-collapse: collapse;
                        }
                        td:not(:first-child), th:not(:first-child) {
                            border-left: 1px solid rgb(144, 148, 151);
                        }
                        th {
                            padding: 12px 12px;
                        }
                        td {
                            padding: 8px 12px;
                        }
                    </style>
                </head>
                $finalHtml
            </html>""".trimIndent()

            // In case of option HTML, stop here and write data to file
            if (exportType == ExportTypes.HTML) {
                saveFile(launcher, context, generateFileName("LucidSourceKit_DataExport", "html"), "text/html", printOptimizedHtml)
                return
            }

            // Render generated HTML in web view and request print job for displayed content
            context.runOnUiThread {
                 val wv = WebView(context).apply {
                    loadData(printOptimizedHtml, "text/html", null)
                }
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val printAdapter = wv.createPrintDocumentAdapter("LucidSourceKitExport")
                val printAttributes = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setResolution(PrintAttributes.Resolution("pdf", "pdf", 600, 600))
                    .setMinMargins(PrintAttributes.Margins(20, 30, 20, 30))
                    .build()
                val printJob = printManager.print("LucidSourceKitExport", printAdapter, printAttributes)
            }
        }

        private fun saveFile(launcher: SimpleActivityLauncher, context: Activity, filename: String, mime: String, content: String) {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.setType(mime)
            intent.putExtra(Intent.EXTRA_TITLE, filename)
            launcher.launch(intent) { result ->
                val uri = result.data?.data
                if (result.resultCode != Activity.RESULT_OK || uri == null) {
                    context.runOnUiThread {
                        Toast.makeText(context, "Failed to get export file path", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { out -> out.write(content) }
                }
            }
        }

        private suspend fun buildMarkDown(context: Context, fromTimestamp: Long, toTimestamp: Long): String {
            val cal = Calendar.getInstance()
            val db = MainDatabase.getInstance(context)

            val ts1 = db.journalEntryDao.getTimestampsBetween(fromTimestamp, toTimestamp)
            val ts2 = db.completedQuestionnaireDao.getTimestampsBetween(fromTimestamp, toTimestamp).await()
            val timestamps = mergeToDistinctDays(ts1, ts2)

            var journalEntryCount = 0
            var questionnaireCount = 0
            val markDownContent = StringBuilder()

            for (timestamp in timestamps) {
                markDownContent.append("## ")
                    .appendLine(dfShort.format(timestamp))
                    .append("\n---\n")

                // Load journal and completed questionnaire data
                val journals = db.journalEntryDao.getEntriesInTimestampRange(timestamp, timestamp + 24 * 60 * 60 * 1000)
                val questionnaires = db.completedQuestionnaireDao.getEntriesInTimestampRange(timestamp, timestamp + 24 * 60 * 60 * 1000).await()

                // Increase counters
                journalEntryCount += journals.size
                questionnaireCount += questionnaires.size

                // Generate Markdown for journal entries
                for ((i, journal) in journals.withIndex()) {
                    markDownContent.append("### ")
                        .appendLine(MarkDownEscaper.escape(journal.journalEntry.title).ifEmpty { "-" })
                        .append("```")
                        .append(MarkDownEscaper.blockEscape(journal.journalEntry.description).ifEmpty { "-" })
                        .appendLine("```")
                        .appendLine()
                        .append("* _Dream mood:_&nbsp; **")
                        .append(DreamMood.valueOf(journal.journalEntry.moodId))
                        .appendLine("**")
                        .append("* _Dream clarity:_&nbsp; **")
                        .append(DreamClarity.valueOf(journal.journalEntry.clarityId))
                        .appendLine("**")
                        .append("* _Sleep quality:_&nbsp; **")
                        .append(SleepQuality.valueOf(journal.journalEntry.qualityId))
                        .appendLine("**")
                        .append("* _Special dream:_&nbsp; **")
                        .append(journal.journalEntryHasTypes.map {
                                DreamTypes.getEnum(it.typeId)
                            }
                            .joinToString(", ")
                            .ifEmpty { "-" }
                        )
                        .appendLine("**")
                        .appendLine()

                    // Add additional spacing between journal entries
                    if (i < journals.size - 1) {
                        markDownContent.appendLine("\u00B6\n")
                    }
                }

                // Add Section for questionnaires if any
                if (questionnaires.isNotEmpty()) {
                    markDownContent.appendLine("---\n### Completed Questionnaires")
                }

                // Generate Markdown for completed questionnaires
                for ((qi, questionnaire) in questionnaires.withIndex()) {
                    val questionnaireDetails = db.questionnaireDao.getById(questionnaire.questionnaireId).await()

                    // Get all questions for questionnaire and query options
                    val questions = db.questionnaireAnswerDao.getAll(questionnaire.id).await().map { answer ->
                            // Get question object for questionnaireAnswer
                            val question = db.questionDao.getById(answer.questionId).await().apply {
                                value = answer.value
                            }

                            // Get options in case of select answer type
                            if (question.questionTypeId == QuestionnaireControlType.SingleSelect.ordinal || question.questionTypeId == QuestionnaireControlType.MultiSelect.ordinal) {
                                // Get all selected options (and mark them as checked)
                                val options = db.selectedOptionsDao.getById(questionnaire.id, answer.questionId).await().map {
                                    db.questionOptionsDao.getById(answer.questionId, it.optionId).await().apply { isChecked = true }
                                }.toMutableList()

                                // Add all unselected options to the list
                                val optionIds = options.map { it.id }
                                options.addAll(db.questionOptionsDao.getAllForQuestion(answer.questionId).await().filter { q -> !optionIds.contains(q.id) })

                                // Sort the options and apply them to the question
                                options.sortBy { it.orderNr }
                                question.options = options
                            }
                            question
                        }.toMutableList()

                    // Start questionnaire table with title
                    markDownContent.append("| ")
                        .append(questionnaireDetails.title)
                        .appendLine(" |\n|-|")

                    // Generate Markdown for all questions of current completed questionnaire
                    for ((i, question) in questions.withIndex()) {
                        markDownContent.append("|")
                            .append(i + 1)
                            .append(". ")
                            .append(MarkDownEscaper.escape(question.question).ifEmpty { "-" })

                        // Generate Markdown for answer
                        val options: List<QuestionOptions>? = question.options
                        if (question.value != null) {
                            // Generate plain text answer
                            markDownContent.append("\u00B6&emsp;`").append(when (question.questionTypeId) {
                                    QuestionnaireControlType.Bool.ordinal -> if (question.value.toBoolean()) "True" else "False"
                                    QuestionnaireControlType.Text.ordinal -> if (question.value.isNullOrEmpty()) "-" else MarkDownEscaper.blockEscape(question.value).ifEmpty { "-" }
                                    else -> MarkDownEscaper.blockEscape(question.value).ifEmpty { "-" }
                                })
                                .appendLine("`|")
                        } else if (options != null) {
                            // Generate option answer
                            for (option in options) {
                                markDownContent.append("\u00B6&emsp;`[")
                                    .append(if (option.isChecked) 'x' else ' ')
                                    .append("] ")
                                    .append(MarkDownEscaper.blockEscape(option.text).ifEmpty { "-" })
                                    .append('`')
                            }
                            markDownContent.appendLine('|')
                        }
                    }

                    // Generate spacing between questionnaires and add extra spacing after questionnaire section
                    markDownContent.appendLine().append('\u00B6')
                    if (qi == questionnaires.size - 1) {
                        markDownContent.append("\u00B6")
                    }
                    markDownContent.appendLine().appendLine()
                }
            }

            return """
                # Data Export
                The document contains dream journals and questionnaires created between ${dfShort.format(fromTimestamp)} and ${dfShort.format(toTimestamp)}. 
                This export was created on ${dfShort.format(cal.timeInMillis)} at ${tfShort.format(cal.timeInMillis)}. A total of $journalEntryCount dream journal 
                entries have been recorded and $questionnaireCount questionnaires have been completed.
                
                
            """.trimIndent() + markDownContent.toString()
        }

        private fun showDatePicker(context: Context, initialTimeStamp: Long, onDateSelected: ((Long) -> Unit)) {
            val cal = Calendar.getInstance()
            cal.timeInMillis = initialTimeStamp
            DatePickerDialog(context, { _: DatePicker?, year: Int, monthOfYear: Int, dayOfMonth: Int ->
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

        private fun mergeToDistinctDays(list1: List<Long>, list2: List<Long>): List<Long> {
            val merged = mutableListOf<Long>()
            val seenDays = mutableSetOf<String>()
            var i = 0
            var j = 0

            while (i < list1.size && j < list2.size) {
                val ts1 = list1[i]
                val ts2 = list2[j]

                val chosen = if (ts1 <= ts2) {
                    i++
                    ts1
                } else {
                    j++
                    ts2
                }

                val (dayKey, dayStart) = getDayKey(chosen)
                if (dayKey !in seenDays) {
                    merged.add(dayStart)
                    seenDays.add(dayKey)
                }
            }

            fun handleRemainder(list: List<Long>, start: Int) {
                for (k in start until list.size) {
                    val ts = list[k]
                    val (dayKey, dayStart) = getDayKey(ts)
                    if (dayKey !in seenDays) {
                        merged.add(dayStart)
                        seenDays.add(dayKey)
                    }
                }
            }
            handleRemainder(list1, i)
            handleRemainder(list2, j)
            return merged
        }

        private fun getDayKey(timestamp: Long): Pair<String, Long> {
            val cal = Calendar.getInstance()
            cal.timeInMillis = timestamp
            val year = cal[Calendar.YEAR]
            val month = cal[Calendar.MONTH] + 1
            val day = cal[Calendar.DAY_OF_MONTH]
            cal[Calendar.HOUR_OF_DAY] = 0
            cal[Calendar.MINUTE] = 0
            cal[Calendar.SECOND] = 0
            cal[Calendar.MILLISECOND] = 0
            return Pair("$year-$month-$day", cal.timeInMillis)
        }
    }
}