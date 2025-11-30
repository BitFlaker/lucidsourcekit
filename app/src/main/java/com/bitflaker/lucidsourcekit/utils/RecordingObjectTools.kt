package com.bitflaker.lucidsourcekit.utils

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.AudioLocation
import com.bitflaker.lucidsourcekit.utils.Tools.calendarFromMillis
import java.text.DateFormat
import java.util.Locale

class RecordingObjectTools(private val context: Context) {
    fun generateDeleteButton(): ImageButton = ImageButton(context).apply {
        setBackgroundTintList(context.attrColorStateList(R.attr.colorSurfaceContainerHigh))
        setImageDrawable(context.resolveDrawable(R.drawable.ic_baseline_cross_24))
        background = context.resolveDrawable(R.drawable.rounded_spinner)
        layoutParams = LinearLayout.LayoutParams(48.dpToPx, 48.dpToPx).apply {
            setMargins(0, 0, 8.dpToPx, 0)
        }
    }

    fun generateDuration(recording: AudioLocation, extendedMarginEnd: Boolean): TextView {
        val seconds = (recording.getRecordingLength() / 1000).toInt()
        val sec = seconds % 60
        val min = (seconds / 60) % 60
        val hours = (seconds / 60) / 60
        val secS = String.format(Locale.ENGLISH, "%02d", sec)
        val minS = String.format(Locale.ENGLISH, "%02d", min)
        val hoursS = String.format(Locale.ENGLISH, "%02d", hours)

        return TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(context.attrColorStateList(R.attr.secondaryTextColor))
            text = if (hours > 0) "$hoursS:" else "$minS:$secS"
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                setMargins(
                    0,
                    0,
                    if (!extendedMarginEnd) 10.dpToPx else 25.dpToPx,
                    0
                )
            }
        }
    }

    fun generateTimestamp(recording: AudioLocation): TextView {
        val df = DateFormat.getDateInstance(DateFormat.SHORT)
        val tf = DateFormat.getTimeInstance(DateFormat.SHORT)
        val date = calendarFromMillis(recording.recordingTimestamp).getTime()

        return TextView(context).apply {
            setTextColor(context.attrColorStateList(R.attr.secondaryTextColor))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            text = String.format(Locale.getDefault(), "%s • %s", df.format(date), tf.format(date))
        }
    }

    fun generateHeading(): TextView = TextView(context).apply {
        setLayoutParams(LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        setTextColor(context.attrColorStateList(R.attr.primaryTextColor))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        setTypeface(null, Typeface.BOLD)
        text = "Recording"
    }

    fun generateLabelsContainer(): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT).apply {
            setMargins(10.dpToPx, 8.dpToPx, 0, 8.dpToPx)
            weight = 1f
        }
    }

    fun generateContainerLayout(): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = context.resolveDrawable(R.drawable.rounded_spinner)
        backgroundTintList = context.attrColorStateList(R.attr.colorSurfaceContainerHigh)
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            setMargins(20.dpToPx, 5.dpToPx, 20.dpToPx, 5.dpToPx)
        }
    }

    fun generatePlayButton(): ImageButton = ImageButton(context).apply {
        setImageDrawable(context.resolveDrawable(R.drawable.ic_baseline_play_arrow_24))
        backgroundTintList = context.attrColorStateList(R.attr.colorSurfaceContainerHigh)
        background = context.resolveDrawable(R.drawable.rounded_spinner)
        layoutParams = LinearLayout.LayoutParams(48.dpToPx, 48.dpToPx).apply {
            setMargins(8.dpToPx, 0, 0, 0)
        }
    }
}
