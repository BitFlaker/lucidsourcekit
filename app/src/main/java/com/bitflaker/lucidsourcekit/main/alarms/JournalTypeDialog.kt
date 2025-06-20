package com.bitflaker.lucidsourcekit.main.alarms

import android.content.Context
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.resulttables.DreamJournalEntry
import com.bitflaker.lucidsourcekit.utils.Tools
import com.google.android.material.button.MaterialButton

fun generateContent(context: Context, listener: (DreamJournalEntry.EntryType) -> Unit): View {
    val dp12 = Tools.dpToPx(context, 12.0)
    val dp24 = Tools.dpToPx(context, 24.0)
    val dp128 = Tools.dpToPx(context, 128.0)

    return LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp24, dp12, dp24, dp24)
        addView(TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            setPadding(0, 0, 0, dp12)
            text = "Select the dream journal entry type you would like to create below"
            setTextColor(Tools.getAttrColor(R.attr.secondaryTextColor, context.theme))
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
                setTextColor(Tools.getAttrColor(R.attr.primaryTextColor, context.theme))
                icon = ResourcesCompat.getDrawable(context.resources, R.drawable.ic_baseline_text_fields_24, context.theme)
                iconGravity = MaterialButton.ICON_GRAVITY_TEXT_TOP
                iconTint = Tools.getAttrColorStateList(R.attr.primaryTextColor, context.theme)
                cornerRadius = dp12
                backgroundTintList = Tools.getAttrColorStateList(R.attr.colorSurfaceContainerHighest, context.theme)
                rippleColor = Tools.getAttrColorStateList(R.attr.colorOutlineVariant, context.theme)
                setOnClickListener { listener.invoke(DreamJournalEntry.EntryType.PLAIN_TEXT) }
            })
            addView(MaterialButton(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, dp128).apply {
                    weight = 1.0f
                }
                text = "Form"
                setTextColor(Tools.getAttrColor(R.attr.primaryTextColor, context.theme))
                icon = ResourcesCompat.getDrawable(context.resources, R.drawable.rounded_convert_to_text_24, context.theme)
                iconGravity = MaterialButton.ICON_GRAVITY_TEXT_TOP
                iconTint = Tools.getAttrColorStateList(R.attr.primaryTextColor, context.theme)
                cornerRadius = dp12
                backgroundTintList = Tools.getAttrColorStateList(R.attr.colorSurfaceContainerHighest, context.theme)
                rippleColor = Tools.getAttrColorStateList(R.attr.colorOutlineVariant, context.theme)
                setOnClickListener { listener.invoke(DreamJournalEntry.EntryType.FORMS_TEXT) }
            })
        })
    }
}