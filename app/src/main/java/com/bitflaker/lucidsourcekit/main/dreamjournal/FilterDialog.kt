package com.bitflaker.lucidsourcekit.main.dreamjournal

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.RadioGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.children
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.DialogFragment
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.data.enums.journalratings.DreamClarity
import com.bitflaker.lucidsourcekit.data.enums.journalratings.DreamMoods
import com.bitflaker.lucidsourcekit.data.enums.journalratings.SleepQuality
import com.bitflaker.lucidsourcekit.data.records.AppliedFilter
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.DreamType
import com.bitflaker.lucidsourcekit.databinding.DialogFilterBinding
import com.bitflaker.lucidsourcekit.utils.Tools
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.radiobutton.MaterialRadioButton
import java.util.Locale
import androidx.core.view.isVisible
import com.bitflaker.lucidsourcekit.utils.attrColorStateList
import com.bitflaker.lucidsourcekit.utils.dpToPx

class FilterDialog(
    private val context: Context,
    private val tags: Array<String>,
    private val currentFilter: AppliedFilter
) : DialogFragment(), DialogInterface.OnClickListener {
    private lateinit var binding: DialogFilterBinding
    private val filterTagsList = ArrayList<String>()
    private val filterDreamTypes = currentFilter.dreamTypes
    private var optionsEntryMap = HashMap<MaterialButton, View>()
    private var okClickListener: DialogInterface.OnClickListener? = null
    private lateinit var titleButtonMap: HashMap<LinearLayout, MaterialButton>
    override fun onClick(dialog: DialogInterface, which: Int) = dismiss()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogFilterBinding.inflate(layoutInflater)
        titleButtonMap = hashMapOf(
            Pair(binding.llContentDt, binding.imgTitleImageDreamType),
            Pair(binding.llContentDm, binding.imgTitleImageDreamMood),
            Pair(binding.llContentDc, binding.imgTitleImageDreamClarity),
            Pair(binding.llContentSq, binding.imgTitleImageSleepQuality)
        )
        val builder = MaterialAlertDialogBuilder(requireActivity())
            .setView(binding.root)
            .setPositiveButton(R.string.ok, okClickListener)
            .setNegativeButton(R.string.cancel, this)

        // Set default style tag filter
        binding.chpGrpFilterTags.removeAllViews()
        binding.txtTagFilterInfo.visibility = View.GONE
        TextViewCompat.setCompoundDrawableTintList(binding.btnFilterEntryTags, context.attrColorStateList(R.attr.colorOutlineVariant))

        // Set map from title buttons to categories and set handlers to open the correct
        // category when clicking on a title button
        optionsEntryMap = titleButtonMap.keys
            .groupBy { titleButtonMap[it]!!.apply { setOnClickListener(::openFilterFromTitle) } }
            .mapValues { it.value.single() }
            .toMap(HashMap())

        // Set handlers for opening and closing categories
        binding.btnFilterDreamMood.setOnClickListener {
            toggleExpander(binding.llContentDm)
        }
        binding.btnFilterDreamClarity.setOnClickListener {
            toggleExpander(binding.llContentDc)
        }
        binding.btnFilterSleepQuality.setOnClickListener {
            toggleExpander(binding.llContentSq)
        }
        binding.btnFilterDreamType.setOnClickListener {
            toggleExpander(binding.llContentDt)
        }
        binding.btnFilterEntryTags.setOnClickListener {
            toggleExpander(binding.llContentTags)
        }

        // Set selected values for radio button groups
        (binding.rdgDm.getChildAt((currentFilter.dreamMood.ordinal + 1) % DreamMoods.entries.size) as MaterialRadioButton).setChecked(true)
        (binding.rdgDc.getChildAt((currentFilter.dreamClarity.ordinal + 1) % DreamClarity.entries.size) as MaterialRadioButton).setChecked(true)
        (binding.rdgSq.getChildAt((currentFilter.sleepQuality.ordinal + 1) % SleepQuality.entries.size) as MaterialRadioButton).setChecked(true)

        // TODO: The order of the checkboxes can get out of sync with the dreamTypes list order when the checkbox order changes! This should not be possible.
        // Set selected dream types and set click handler
        val dreamTypes = DreamType.defaultData
        for (i in 0..<binding.llDt.childCount) {
            val chk = binding.llDt.getChildAt(i) as CheckBox
            val dreamType = dreamTypes[i].typeId
            chk.setChecked(filterDreamTypes.contains(dreamType))
            chk.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    filterDreamTypes.add(dreamType)
                } else {
                    filterDreamTypes.remove(dreamType)
                }
                updateIconsCheckboxSelect(binding.btnFilterDreamType, binding.llDt, binding.llContentDt)
            }
        }

        // Set autocomplete handlers for tag search
        binding.actvFilterTags.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, tags))
        binding.actvFilterTags.onItemClickListener = OnItemClickListener { _, _, _, _ -> addTagToFilter() }
        binding.actvFilterTags.setOnEditorActionListener { _, i, _ ->
            if (i == EditorInfo.IME_ACTION_DONE) addTagToFilter()
            true
        }

        // Add tag filter values to selected list
        for (filterTag in currentFilter.filterTagsList) {
            addTagFilterEntry(filterTag)
        }

        // Setup radio selection change listeners
        setupRadioSelectionListeners(binding.rdgDm, binding.btnFilterDreamMood, binding.llContentDm)
        setupRadioSelectionListeners(binding.rdgDc, binding.btnFilterDreamClarity, binding.llContentDc)
        setupRadioSelectionListeners(binding.rdgSq, binding.btnFilterSleepQuality, binding.llContentSq)

        // Update stats as soon as layout was loaded
        val viewTreeObserver = binding.root.getViewTreeObserver()
        viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                println("ADASD")
                binding.root.getViewTreeObserver().removeOnGlobalLayoutListener(this)
                updateIconsCheckboxSelect(binding.btnFilterDreamType, binding.llDt, binding.llContentDt)
                radioSelectionChanged(binding.btnFilterDreamMood, binding.rdgDm, binding.llContentDm)
                radioSelectionChanged(binding.btnFilterDreamClarity, binding.rdgDc, binding.llContentDc)
                radioSelectionChanged(binding.btnFilterSleepQuality, binding.rdgSq, binding.llContentSq)
            }
        })

        return builder.create()
    }

    private fun openFilterFromTitle(view: View) {
        optionsEntryMap.values.forEach { it.visibility = View.GONE }
        optionsEntryMap[view]?.visibility = View.VISIBLE
    }

    private fun setupRadioSelectionListeners(group: RadioGroup, category: MaterialButton, content: LinearLayout) {
        group.children.forEach {
            (it as MaterialRadioButton).setOnCheckedChangeListener { _, isSelected ->
                if (isSelected) {
                    updateIconsRadioSelect(category, group, content, it)
                }
            }
        }
    }

    private fun addTagToFilter() {
        val enteredTag = binding.actvFilterTags.getText().toString()
        if (tags.contains(enteredTag) && !filterTagsList.contains(enteredTag)) {
            addTagFilterEntry(enteredTag)
        }
    }

    private fun radioSelectionChanged(categoryButton: MaterialButton, radioGroup: RadioGroup, container: LinearLayout) {
        val selected = binding.root.findViewById<MaterialRadioButton>(radioGroup.checkedRadioButtonId)
        updateIconsRadioSelect(categoryButton, radioGroup, container, selected)
    }

    private fun updateIconsRadioSelect(categoryButton: MaterialButton, radioGroup: RadioGroup, container: LinearLayout, selected: MaterialRadioButton) {
        binding.actvFilterTags.clearFocus()
        val doNotFilterOption = radioGroup.getChildAt(0) as MaterialRadioButton
        val filteredSelected = if (selected.isChecked && selected === doNotFilterOption) listOf() else listOf(selected)
        setIcons(categoryButton, container, filteredSelected)
    }

    private fun updateIconsCheckboxSelect(categoryButton: MaterialButton, container: LinearLayout, content: LinearLayout) {
        binding.actvFilterTags.clearFocus()
        val checkedItems: List<MaterialCheckBox> = container.children
            .filterIsInstance<MaterialCheckBox>()
            .filter { it.isChecked }
            .toList()
        setIcons(categoryButton, content, checkedItems)
    }

    private fun <E> setIcons(categoryButton: MaterialButton, container: View, selected: List<E>) {
        lateinit var icon: Drawable
        lateinit var titleIcon: Drawable
        var colorAttrIcon: Int
        var colorAttrDrawable: Int
        var colorAttrBackground: Int

        // Get icons and colors based on whether anything was selected
        if (selected.isEmpty()) {
            icon = ResourcesCompat.getDrawable(context.resources, R.drawable.rounded_add_24, context.theme)!!
            icon.setBounds(0, 0, 24.dpToPx, 24.dpToPx)
            titleIcon = ResourcesCompat.getDrawable(context.resources, R.drawable.ic_baseline_horizontal_rule_24, context.theme)!!
            colorAttrIcon = R.attr.colorOutline
            colorAttrDrawable = R.attr.colorOutlineVariant
            colorAttrBackground = android.R.color.transparent

        } else {
            val drawables = getSelectedCompoundDrawables<E>(selected)
            icon = combineDrawables(drawables)
            titleIcon = getSingleDrawable(drawables)
            colorAttrIcon = R.attr.colorOnSecondary
            colorAttrDrawable = R.attr.colorOnSurface
            colorAttrBackground = R.attr.colorSecondary
        }

        // Set the icon on the category
        TextViewCompat.setCompoundDrawableTintList(categoryButton, context.attrColorStateList(colorAttrDrawable))
        categoryButton.setCompoundDrawablesWithIntrinsicBounds(null, null, icon, null)

        // Set the icon and button color of the title button
        titleButtonMap[container]?.apply {
            backgroundTintList = context.attrColorStateList(colorAttrBackground)
            setIconTint(context.attrColorStateList(colorAttrIcon))
            setIcon(titleIcon)
        }
    }

    private fun <E> getSelectedCompoundDrawables(checkedItems: List<E?>): Array<Drawable> {
        return checkedItems
            .filterIsInstance<CompoundButton>()
            .map { Tools.cloneDrawable(it.compoundDrawables[2]) }
            .toTypedArray()
    }

    private fun combineDrawables(drawables: Array<Drawable>): Drawable {
        if (drawables.size == 1) {
            return drawables[0]
        }
        val layered = LayerDrawable(drawables)
        for (i in drawables.indices) {
            layered.setLayerGravity(i, Gravity.START)
            layered.setLayerInsetEnd(i, drawables.drop(i + 1).sumOf { it.intrinsicWidth })
            layered.setLayerInsetStart(i, drawables.take(i).sumOf { it.intrinsicWidth })
        }
        return layered
    }

    private fun getSingleDrawable(drawables: Array<Drawable>): Drawable {
        if (drawables.size == 1) {
            return Tools.cloneDrawable(drawables[0])
        }
        return ResourcesCompat.getDrawable(context.resources, R.drawable.rounded_more_horiz_24, context.theme)!!
    }

    private fun toggleExpander(expanderContent: View) {
        binding.actvFilterTags.clearFocus()
        expanderContent.visibility = if (expanderContent.isVisible) View.GONE else View.VISIBLE
    }

    private fun addTagFilterEntry(enteredTag: String) {
        binding.chpGrpFilterTags.addView(generateTagFilterChip(enteredTag, true))
        filterTagsList.add(enteredTag)
        binding.actvFilterTags.setText("")
        binding.txtNoTagsFiltered.visibility = View.GONE
        updateTagFilterCount()
    }

    private fun generateTagFilterChip(enteredTag: String, isTitle: Boolean): Chip {
        val colorAttrBackground = if (isTitle) R.attr.colorSecondary else R.attr.colorSurfaceContainer
        val colorAttrForeground = if (isTitle) R.attr.colorOnSecondary else R.attr.colorOnSurface
        val colorBackground = context.attrColorStateList(colorAttrBackground)
        val colorForeground = context.attrColorStateList(colorAttrForeground)

        return Chip(context).apply {
            text = enteredTag
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            isCheckable = false
            isCloseIconVisible = true
            chipStrokeWidth = 0f
            setTextColor(colorForeground)
            chipBackgroundColor = colorBackground
            closeIconTint = colorForeground
            setOnClickListener {
                filterTagsList.remove(enteredTag)
                (it.parent as ViewGroup).removeView(it)
                if (filterTagsList.isEmpty()) {
                    binding.txtNoTagsFiltered.visibility = View.VISIBLE
                }
                updateTagFilterCount()
            }
        }
    }

    private fun updateTagFilterCount() {
        val count = filterTagsList.size
        binding.txtFilterTagCount.text = String.format(Locale.getDefault(), "%d %s", count, (if (count == 1) "tag" else "tags"))
        binding.txtTagFilterInfo.text = String.format(Locale.getDefault(), "%d %s in filter", count, (if (count == 1) "tag" else "tags"))

        var icon: Drawable? = null
        var visibility = View.VISIBLE
        if (count == 0) {
            icon = ResourcesCompat.getDrawable(context.resources, R.drawable.rounded_add_24, context.theme)
            icon?.setBounds(0, 0, 24.dpToPx, 24.dpToPx)
            visibility = View.GONE
        }
        binding.btnFilterEntryTags.setCompoundDrawablesWithIntrinsicBounds(null, null, icon, null)
        binding.txtFilterTagCount.visibility = visibility
        binding.txtTagFilterInfo.visibility = visibility
    }

    val filters: AppliedFilter
        get() {
            val moodIndex = getSelectedRadio(binding.rdgDm) - 1
            val clarityIndex = getSelectedRadio(binding.rdgDc) - 1
            val qualityIndex = getSelectedRadio(binding.rdgSq) - 1
            val mood = if (moodIndex >= 0) DreamMoods.entries[moodIndex] else DreamMoods.None
            val clarity = if (clarityIndex >= 0) DreamClarity.entries[clarityIndex] else DreamClarity.None
            val quality = if (qualityIndex >= 0) SleepQuality.entries[qualityIndex] else SleepQuality.None
            return AppliedFilter(filterTagsList, filterDreamTypes, mood, clarity, quality)
        }

    private fun getSelectedRadio(group: RadioGroup): Int {
        return group.children.indexOfFirst { it is MaterialRadioButton && it.isChecked }
    }

    fun setOnClickPositiveButton(listener: DialogInterface.OnClickListener?) {
        okClickListener = listener
    }
}
