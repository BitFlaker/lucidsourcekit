package com.bitflaker.lucidsourcekit.main.alarms

import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.bitflaker.lucidsourcekit.MainActivity
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.resulttables.DreamJournalEntry
import com.bitflaker.lucidsourcekit.utils.Tools
import com.bitflaker.lucidsourcekit.utils.attrColor
import com.bitflaker.lucidsourcekit.utils.attrColorStateList
import com.bitflaker.lucidsourcekit.utils.dpToPx
import com.google.android.material.button.MaterialButton

class JournalTypeDialog {
    companion object {
        fun generateContent(context: Context, listener: () -> Unit): View {
            val dp12 = 12.dpToPx
            val dp24 = 24.dpToPx
            val dp128 = 128.dpToPx

            return LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp24, dp12, dp24, dp24)
                addView(TextView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
                    setPadding(0, 0, 0, dp12)
                    text = "Select the dream journal entry type you would like to create below"
                    setTextColor(context.attrColor(R.attr.secondaryTextColor))
                })
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
                    dividerDrawable = ResourcesCompat.getDrawable(context.resources, R.drawable.divider_large, context.theme)
                    addView(MaterialButton(context).apply {
                        layoutParams = LinearLayout.LayoutParams(0, dp128).apply {
                            weight = 1.0f
                        }
                        text = "Text"
                        setTextColor(context.attrColor(R.attr.primaryTextColor))
                        icon = ResourcesCompat.getDrawable(context.resources, R.drawable.ic_baseline_text_fields_24, context.theme)
                        iconGravity = MaterialButton.ICON_GRAVITY_TEXT_TOP
                        iconTint = context.attrColorStateList(R.attr.primaryTextColor)
                        cornerRadius = dp12
                        backgroundTintList = context.attrColorStateList(R.attr.colorSurfaceContainerHighest)
                        rippleColor = context.attrColorStateList(R.attr.colorOutlineVariant)
                        setOnClickListener {
                            showEntry(context, DreamJournalEntry.EntryType.PLAIN_TEXT)
                            listener()
                        }
                    })
                    addView(MaterialButton(context).apply {
                        layoutParams = LinearLayout.LayoutParams(0, dp128).apply {
                            weight = 1.0f
                        }
                        text = "Form"
                        setTextColor(context.attrColor(R.attr.primaryTextColor))
                        icon = ResourcesCompat.getDrawable(context.resources, R.drawable.rounded_convert_to_text_24, context.theme)
                        iconGravity = MaterialButton.ICON_GRAVITY_TEXT_TOP
                        iconTint = context.attrColorStateList(R.attr.primaryTextColor)
                        cornerRadius = dp12
                        backgroundTintList = context.attrColorStateList(R.attr.colorSurfaceContainerHighest)
                        rippleColor = context.attrColorStateList(R.attr.colorOutlineVariant)
                        setOnClickListener {
                            showEntry(context, DreamJournalEntry.EntryType.FORMS_TEXT)
                            listener.invoke()
                        }
                    })
                })
            }
        }

        fun showEntry(context: Context, type: DreamJournalEntry.EntryType) {
            context.startActivity(Intent(context, MainActivity::class.java).apply {
                setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("DREAM_JOURNAL_TYPE", type.ordinal)
                putExtra("INITIAL_PAGE", "journal")
            })
        }
    }
}
