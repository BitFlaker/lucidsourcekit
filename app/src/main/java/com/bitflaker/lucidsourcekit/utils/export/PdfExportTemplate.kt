package com.bitflaker.lucidsourcekit.utils.export

import android.content.Context
import android.graphics.Bitmap
import com.bitflaker.lucidsourcekit.database.MainDatabase
import com.bitflaker.lucidsourcekit.main.export.templates.data.ExportData
import com.bitflaker.lucidsourcekit.main.export.templates.data.JournalEntryExportData
import com.bitflaker.lucidsourcekit.main.export.templates.data.QuestionExportValueBool
import com.bitflaker.lucidsourcekit.main.export.templates.data.QuestionExportValueMultipleChoice
import com.bitflaker.lucidsourcekit.main.export.templates.data.QuestionExportValueRating
import com.bitflaker.lucidsourcekit.main.export.templates.data.QuestionExportValueText
import com.bitflaker.lucidsourcekit.main.export.templates.data.QuestionnaireExportData
import com.bitflaker.lucidsourcekit.main.export.templates.data.QuestionnaireExportQuestion
import com.bitflaker.lucidsourcekit.main.questionnaire.QuestionnaireControlType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

abstract class PdfExportTemplate {
    companion object {
        private const val PLACEHOLDER_TEXT: String = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, " +
                "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim " +
                "veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat." +
                " Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu " +
                "fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa " +
                "qui officia deserunt mollit anim id est laborum."
        private val placeholderWords = PLACEHOLDER_TEXT.split(" ")

        fun generateText(wordCount: Int): String {
            return (0 until wordCount).joinToString(" ") {
                placeholderWords[it % placeholderWords.size]
            }
        }

        suspend fun loadExportData(
            context: Context,
            config: ExportConfiguration
        ): List<ExportData> = withContext(Dispatchers.IO) {
            val cal = Calendar.getInstance()
            val db = MainDatabase.getInstance(context)

            val ts1 = db.journalEntryDao.getTimestampsBetween(config.exportFromTS, config.exportToTS)
            val ts2 = db.completedQuestionnaireDao.getTimestampsBetween(config.exportFromTS, config.exportToTS)
            val timestamps = mergeToDistinctDays(ts1, ts2)

            val days = mutableListOf<ExportData>()
            for (timestamp in timestamps) {
                // Load journal and completed questionnaire data
                val journals = db.journalEntryDao.getEntriesInTimestampRange(timestamp, timestamp + 24 * 60 * 60 * 1000)
                val questionnaires = db.completedQuestionnaireDao.getEntriesInTimestampRange(timestamp, timestamp + 24 * 60 * 60 * 1000)

                // Get journal export data
                val journalEntries = journals.map {
                    JournalEntryExportData(
                        it.journalEntry.title,
                        it.journalEntry.description?.ifEmpty { "-" } ?: "-"
                        /*
                        DreamMood.valueOf(journal.journalEntry.moodId)
                        DreamClarity.valueOf(journal.journalEntry.clarityId)
                        SleepQuality.valueOf(journal.journalEntry.qualityId)
                        journal.journalEntryHasTypes.map { DreamTypes.getEnum(it.typeId) }.joinToString(", ").ifEmpty { "-" }
                        */
                    )
                }.toList()

                val questionnaireEntries = questionnaires.map { questionnaire ->
                    val questionnaireDetails = db.questionnaireDao.getById(questionnaire.questionnaireId)

                    // Get all questions for questionnaire and query options
                    val questions = db.questionnaireAnswerDao.getAll(questionnaire.id).map { answer ->
                        // Get question object for questionnaireAnswer
                        val question = db.questionDao.getById(answer.questionId).apply {
                            value = answer.value
                        }

                        // Get options in case of select answer type
                        if (question.questionTypeId == QuestionnaireControlType.SingleSelect.ordinal || question.questionTypeId == QuestionnaireControlType.MultiSelect.ordinal) {
                            // Get all selected options (and mark them as checked)
                            val options = db.selectedOptionsDao.getById(questionnaire.id, answer.questionId).map { option ->
                                db.questionOptionsDao.getById(answer.questionId, option.optionId).apply { isChecked = true }
                            }.toMutableList()

                            // Add all unselected options to the list
                            val optionIds = options.map { it.id }
                            options.addAll(db.questionOptionsDao.getAllForQuestion(answer.questionId).filter { q -> !optionIds.contains(q.id) })

                            // Sort the options and apply them to the question
                            options.sortBy { it.orderNr }
                            question.options = options
                        }

                        val value = if (question.value != null) {
                            when (question.questionTypeId) {
                                QuestionnaireControlType.Bool.ordinal -> QuestionExportValueBool(question.value.toBoolean())
                                QuestionnaireControlType.Text.ordinal -> QuestionExportValueText(question.value?.ifEmpty { "-" } ?: "-")
                                QuestionnaireControlType.Range.ordinal -> QuestionExportValueRating(question.valueFrom ?: 0, question.valueTo ?: 0, question.value?.toInt() ?: -1)
                                else -> throw IllegalStateException("Unknown question type")
                            }
                        } else if (question.options != null) {
                            val options = mutableListOf<String>()
                            val selectedIndices = mutableListOf<Int>()
                            for ((i, option) in question.options!!.withIndex()) {
                                if (option.isChecked) {
                                    selectedIndices.add(i)
                                }
                                options.add(option.text.ifEmpty { "-" })
                            }
                            QuestionExportValueMultipleChoice(
                                options.toTypedArray(),
                                selectedIndices.toIntArray()
                            )
                        } else {
                            throw IllegalStateException("Illegal question state")
                        }

                        QuestionnaireExportQuestion(question.question, value)
                    }.toMutableList()

                    QuestionnaireExportData(
                        questionnaireDetails.title,
                        questions
                    )
                }.toList()

                cal.timeInMillis = timestamp
                days.add(ExportData(
                    cal.time,
                    journalEntries,
                    questionnaireEntries
                ))
            }

            days
        }

        private fun mergeToDistinctDays(list1: List<Long>, list2: List<Long>): List<Long> {
            val merged = mutableListOf<Long>()
            val seenDays = mutableSetOf<String>()
            var i = 0
            var j = 0

            while (i < list1.size && j < list2.size) {
                val ts1 = list1[i]
                val ts2 = list2[j]

                val chosen = if (ts1 > ts2) {
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

    abstract suspend fun generatePreview(context: Context): Bitmap
    abstract suspend fun generatePDF(config: ExportConfiguration)
}
