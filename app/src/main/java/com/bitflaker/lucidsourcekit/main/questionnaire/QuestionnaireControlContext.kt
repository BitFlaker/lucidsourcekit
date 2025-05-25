package com.bitflaker.lucidsourcekit.main.questionnaire

import com.bitflaker.lucidsourcekit.main.questionnaire.options.ControlOptions
import com.bitflaker.lucidsourcekit.main.questionnaire.results.ControlResult

data class QuestionnaireControlContext(val type: QuestionnaireControlType, val options: ControlOptions?, val result: ControlResult?)

