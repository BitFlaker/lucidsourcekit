package com.bitflaker.lucidsourcekit.main;

import static android.view.inputmethod.EditorInfo.IME_ACTION_DONE;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.DialogFragment;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.general.JournalTypes;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.bitflaker.lucidsourcekit.general.database.values.DreamClarity;
import com.bitflaker.lucidsourcekit.general.database.values.DreamMoods;
import com.bitflaker.lucidsourcekit.general.database.values.SleepQuality;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.radiobutton.MaterialRadioButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class FilterDialog extends DialogFragment implements DialogInterface.OnClickListener {
    private MaterialButton filterDreamMoodCat, filterDreamClarityCat, filterSleepQualityCat, filterDreamTypeCat, filterEntryTagsCat;
    private AutoCompleteTextView filterTags;
    private final String[] tags;
    private DialogInterface.OnClickListener okClickListener;
    private List<String> filterTagsList;
    private boolean[] filterDreamTypes;
    private AppliedFilter currentFilter;
    private RadioGroup filterDreamMoodRadioGroup, filterDreamClarityRadioGroup, filterSleepQualityRadioGroup;
    private LinearLayout filterDreamType, filterEntryTag;
    private Context context;
    private HashMap<View, MaterialButton> titleButtonMap = new HashMap<>();
    private HashMap<MaterialButton, View> optionsEntryMap = new HashMap<>();
    private ChipGroup tagFilterGroupTitle;
    private TextView noTagsInFilter, filterTagCount, tagFilterInfo;
    private LinearLayout contentDreamMood, contentDreamType, contentDreamClarity, contentSleepQuality, dreamTypeCheckboxContainer;
    private MaterialButton titleDreamType, titleDreamMood, titleDreamClarity, titleSleepQuality;

    public FilterDialog(Context context, String[] tags, AppliedFilter currentFilter) {
        this.tags = tags;
        this.currentFilter = currentFilter;
        this.context = context;
        filterDreamTypes = currentFilter.getFilterDreamTypes();
        filterTagsList = new ArrayList<>();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = getLayoutInflater().inflate(R.layout.dialog_filter, null);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity());
        builder.setView(view);
        builder.setPositiveButton(R.string.ok, okClickListener).setNegativeButton(R.string.cancel, this);
        setData(view);

        titleButtonMap.put(contentDreamType, titleDreamType);
        titleButtonMap.put(contentDreamMood, titleDreamMood);
        titleButtonMap.put(contentDreamClarity, titleDreamClarity);
        titleButtonMap.put(contentSleepQuality, titleSleepQuality);
        optionsEntryMap = new HashMap<>(titleButtonMap.keySet().stream().collect(Collectors.toMap(k -> titleButtonMap.get(k), k -> k)));
        optionsEntryMap.keySet().forEach(b -> b.setOnClickListener(this::openFilterFromTitle));

        setupSimpleCategoryListeners();
        setupTagsCategoryListener();
        for (String filterTag : currentFilter.getFilterTagsList()) { addTagFilterEntry(filterTag); }
        setupRadioChangedListeners();
        updateStatesWhenLoaded(view);

        return builder.create();
    }

    private void openFilterFromTitle(View view) {
        optionsEntryMap.values().forEach(v -> v.setVisibility(View.GONE));
        optionsEntryMap.get(view).setVisibility(View.VISIBLE);
    }

    private void setCurrentFilterCategoryStates(View view) {
        checkboxSelectionChanged(filterDreamTypeCat, dreamTypeCheckboxContainer, contentDreamType);
        radioSelectionChanged(view.findViewById(filterDreamMoodRadioGroup.getCheckedRadioButtonId()), filterDreamMoodCat, filterDreamMoodRadioGroup, contentDreamMood);
        radioSelectionChanged(view.findViewById(filterDreamClarityRadioGroup.getCheckedRadioButtonId()), filterDreamClarityCat, filterDreamClarityRadioGroup, contentDreamClarity);
        radioSelectionChanged(view.findViewById(filterSleepQualityRadioGroup.getCheckedRadioButtonId()), filterSleepQualityCat, filterSleepQualityRadioGroup, contentSleepQuality);
    }

    private void setupRadioChangedListeners() {
        setupRadioSelectionChangedListeners(filterDreamMoodRadioGroup, filterDreamMoodCat, contentDreamMood);
        setupRadioSelectionChangedListeners(filterDreamClarityRadioGroup, filterDreamClarityCat, contentDreamClarity);
        setupRadioSelectionChangedListeners(filterSleepQualityRadioGroup, filterSleepQualityCat, contentSleepQuality);
    }

    private void setupRadioSelectionChangedListeners(RadioGroup group, MaterialButton category, LinearLayout content) {
        for (int i = 0; i < group.getChildCount(); i++) {
            if (group.getChildAt(i) instanceof MaterialRadioButton button) {
                button.setOnCheckedChangeListener((compoundButton, isSelected) -> {
                    if (isSelected) {
                        radioSelectionChanged(compoundButton, category, group, content);
                    }
                });
            }
        }
    }

    private void setupTagsCategoryListener() {
        filterTags.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, tags));
        filterTags.setOnItemClickListener((adapterView, view, i, l) -> addTagToFilter());
        filterTags.setOnEditorActionListener((textView, i, keyEvent) -> {
            if(i == IME_ACTION_DONE) addTagToFilter();
            return true;
        });
    }

    private void addTagToFilter() {
        String enteredTag = filterTags.getText().toString();
        if(Arrays.asList(tags).contains(enteredTag) && !filterTagsList.contains(enteredTag)){
            addTagFilterEntry(enteredTag);
        }
    }

    private void setupSimpleCategoryListeners() {
        filterDreamMoodCat.setOnClickListener(e -> openCloseExpander(contentDreamMood));
        filterDreamClarityCat.setOnClickListener(e -> openCloseExpander(contentDreamClarity));
        filterSleepQualityCat.setOnClickListener(e -> openCloseExpander(contentSleepQuality));
        filterDreamTypeCat.setOnClickListener(e -> openCloseExpander(contentDreamType));
        filterEntryTagsCat.setOnClickListener(e -> openCloseExpander(filterEntryTag));

        // Setting default values
        ((MaterialRadioButton) filterDreamMoodRadioGroup.getChildAt((currentFilter.getDreamMood().ordinal() + 1) % DreamMoods.values().length)).setChecked(true);
        ((MaterialRadioButton) filterDreamClarityRadioGroup.getChildAt((currentFilter.getDreamClarity().ordinal() + 1) % DreamClarity.values().length)).setChecked(true);
        ((MaterialRadioButton) filterSleepQualityRadioGroup.getChildAt((currentFilter.getSleepQuality().ordinal() + 1) % SleepQuality.values().length)).setChecked(true);
        setCheckedStateInList(dreamTypeCheckboxContainer, currentFilter.getFilterDreamTypes());
    }

    private void setData(View view) {
        tagFilterInfo = view.findViewById(R.id.txt_tag_filter_info);
        filterTagCount = view.findViewById(R.id.txt_filter_tag_count);
        noTagsInFilter = view.findViewById(R.id.txt_no_tags_filtered);
        filterTags = view.findViewById(R.id.actv_filter_tags);
        tagFilterGroupTitle = view.findViewById(R.id.chp_grp_filter_tags);
        filterDreamMoodCat = view.findViewById(R.id.btn_filter_dream_mood);
        filterDreamMoodRadioGroup = view.findViewById(R.id.rdg_dm);
        filterDreamClarityCat = view.findViewById(R.id.btn_filter_dream_clarity);
        filterDreamClarityRadioGroup = view.findViewById(R.id.rdg_dc);
        filterSleepQualityCat = view.findViewById(R.id.btn_filter_sleep_quality);
        filterSleepQualityRadioGroup = view.findViewById(R.id.rdg_sq);
        filterDreamTypeCat = view.findViewById(R.id.btn_filter_dream_type);
        filterDreamType = view.findViewById(R.id.ll_content_dt);
        filterEntryTag = view.findViewById(R.id.ll_content_tags);
        filterEntryTagsCat = view.findViewById(R.id.btn_filter_entry_tags);
        dreamTypeCheckboxContainer = view.findViewById(R.id.ll_dt);

        contentDreamType = view.findViewById(R.id.ll_content_dt);
        titleDreamType = view.findViewById(R.id.img_title_image_dream_type);
        contentDreamMood = view.findViewById(R.id.ll_content_dm);
        titleDreamMood = view.findViewById(R.id.img_title_image_dream_mood);
        contentDreamClarity = view.findViewById(R.id.ll_content_dc);
        titleDreamClarity = view.findViewById(R.id.img_title_image_dream_clarity);
        contentSleepQuality = view.findViewById(R.id.ll_content_sq);
        titleSleepQuality = view.findViewById(R.id.img_title_image_sleep_quality);

        tagFilterGroupTitle.removeAllViews();
        tagFilterInfo.setVisibility(View.GONE);
        TextViewCompat.setCompoundDrawableTintList(filterEntryTagsCat, Tools.getAttrColorStateList(R.attr.colorOutlineVariant, context.getTheme()));
    }

    private void radioSelectionChanged(View checked, MaterialButton categoryButton, RadioGroup radioGroup, LinearLayout container) {
        filterTags.clearFocus();
        MaterialRadioButton doNotFilterOption = (MaterialRadioButton) radioGroup.getChildAt(0);
        MaterialRadioButton selected = (MaterialRadioButton) checked;
        setIcons(categoryButton, container, List.of(selected), selected.isChecked() && selected == doNotFilterOption);
    }

    private <E> void setIcons(MaterialButton categoryButton, View container, List<E> selected, boolean hasValue) {
        if(hasValue) {
            Drawable icon = ResourcesCompat.getDrawable(context.getResources(), R.drawable.rounded_add_24, context.getTheme());
            icon.setBounds(0, 0, Tools.dpToPx(context, 24), Tools.dpToPx(context, 24));
            categoryButton.setCompoundDrawablesWithIntrinsicBounds(null, null, icon, null);
            TextViewCompat.setCompoundDrawableTintList(categoryButton, Tools.getAttrColorStateList(R.attr.colorOutlineVariant, context.getTheme()));

            // Set the title button
            Drawable titleIcon = ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_baseline_horizontal_rule_24, context.getTheme());
            MaterialButton titleButton = titleButtonMap.get(container);
            titleButton.setBackgroundTintList(Tools.getAttrColorStateList(android.R.color.transparent, context.getTheme()));
            titleButton.setIconTint(Tools.getAttrColorStateList(R.attr.colorOutline, context.getTheme()));
            titleButton.setIcon(titleIcon);
        }
        else {
            Drawable[] drawables = getSelectedCompoundDrawables(selected);
            Drawable icon = combineDrawables(drawables);
            categoryButton.setCompoundDrawablesWithIntrinsicBounds(null, null, icon, null);
            TextViewCompat.setCompoundDrawableTintList(categoryButton, Tools.getAttrColorStateList(R.attr.colorOnSurface, context.getTheme()));

            // Set the title button
            MaterialButton titleButton = titleButtonMap.get(container);
            titleButton.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.colorSecondary, context.getTheme()));
            titleButton.setIconTint(Tools.getAttrColorStateList(R.attr.colorOnSecondary, context.getTheme()));
            titleButton.setIcon(getSingleDrawable(drawables));
        }
    }

    private void checkboxSelectionChanged(MaterialButton categoryButton, LinearLayout container, LinearLayout content) {
        filterTags.clearFocus();
        List<MaterialCheckBox> checkedItems = new ArrayList<>();
        for (int i = 0; i < container.getChildCount(); i++) {
            if (container.getChildAt(i) instanceof MaterialCheckBox chk && chk.isChecked()){
                checkedItems.add(chk);
            }
        }
        setIcons(categoryButton, content, checkedItems, checkedItems.isEmpty());
    }

    private static <E> Drawable[] getSelectedCompoundDrawables(List<E> checkedItems) {
        Drawable[] drawables = new Drawable[checkedItems.size()];
        for (int i = 0; i < checkedItems.size(); i++) {
            if (checkedItems.get(i) instanceof CompoundButton compoundButton) {
                drawables[i] = Tools.cloneDrawable(compoundButton.getCompoundDrawables()[2]);
            }
        }
        return drawables;
    }

    private Drawable getSingleDrawable(Drawable[] drawables) {
        if (drawables.length == 1) {
            return Tools.cloneDrawable(drawables[0]);
        }
        return ResourcesCompat.getDrawable(context.getResources(), R.drawable.rounded_more_horiz_24, context.getTheme());
    }

    private static Drawable combineDrawables(Drawable[] drawables) {
        if (drawables.length == 1) {
            return drawables[0];
        }
        LayerDrawable finalDrawable = new LayerDrawable(drawables);
        for (int i = 0; i < drawables.length; i++) {
            finalDrawable.setLayerGravity(i, Gravity.START);
            finalDrawable.setLayerInsetEnd(i, Arrays.stream(drawables).skip(i + 1).mapToInt(Drawable::getIntrinsicWidth).sum());
            finalDrawable.setLayerInsetStart(i, Arrays.stream(drawables).limit(i).mapToInt(Drawable::getIntrinsicWidth).sum());
        }
        return finalDrawable;
    }

    private void setCheckedStateInList(LinearLayout container, boolean[] values) {
        for (int i = 0; i < container.getChildCount(); i++){
            CheckBox chk = (CheckBox) container.getChildAt(i);
            if(values[i]) { chk.setChecked(true); }
            chk.setOnCheckedChangeListener((compoundButton, b) -> {
                filterDreamTypes[getIdInContainer(container, chk)] = b;
                checkboxSelectionChanged(filterDreamTypeCat, dreamTypeCheckboxContainer, contentDreamType);
            });
        }
    }

    private int getIdInContainer(LinearLayout container, CheckBox item) {
        for (int i = 0; i < container.getChildCount(); i++){
            if(container.getChildAt(i) == item){
                return i;
            }
        }
        return -1;
    }

    private void openCloseExpander(View expanderContent) {
        filterTags.clearFocus();
        if (expanderContent.getVisibility() == View.GONE) {
            expanderContent.setVisibility(View.VISIBLE);
        } else {
            expanderContent.setVisibility(View.GONE);
        }
    }

    private void addTagFilterEntry(String enteredTag) {
        Chip titleChip = generateTagFilterChip(enteredTag, true);
        tagFilterGroupTitle.addView(titleChip);
        filterTagsList.add(enteredTag);
        filterTags.setText("");
        if (noTagsInFilter.getVisibility() == View.VISIBLE) {
            noTagsInFilter.setVisibility(View.GONE);
        }
        updateTagFilterCount();
    }

    private Chip generateTagFilterChip(String enteredTag, boolean isTitle) {
        Chip chip = new Chip(context);
        chip.setText(enteredTag);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        chip.setCheckable(false);
        chip.setCloseIconVisible(true);
        chip.setChipStrokeWidth(0);
        int colorAttrBackground = isTitle ? R.attr.colorSecondary : R.attr.colorSurfaceContainer;
        int colorAttrForeground = isTitle ? R.attr.colorOnSecondary : R.attr.colorOnSurface;
        ColorStateList colorBackground = Tools.getAttrColorStateList(colorAttrBackground, context.getTheme());
        ColorStateList colorForeground = Tools.getAttrColorStateList(colorAttrForeground, context.getTheme());
        chip.setTextColor(colorForeground);
        chip.setChipBackgroundColor(colorBackground);
        chip.setCloseIconTint(colorForeground);
        chip.setOnClickListener(e -> {
            filterTagsList.remove(enteredTag);
            ((ViewGroup) e.getParent()).removeView(e);
            if (filterTagsList.isEmpty()) {
                noTagsInFilter.setVisibility(View.VISIBLE);
            }
            updateTagFilterCount();
        });
        return chip;
    }

    private void updateTagFilterCount() {
        int tagFilterCount = filterTagsList.size();
        filterTagCount.setText(String.format(Locale.getDefault(), "%d %s", tagFilterCount, (tagFilterCount == 1 ? "tag" : "tags")));
        tagFilterInfo.setText(String.format(Locale.getDefault(), "%d %s in filter", tagFilterCount, (tagFilterCount == 1 ? "tag" : "tags")));
        if (tagFilterCount == 0) {
            Drawable icon = ResourcesCompat.getDrawable(context.getResources(), R.drawable.rounded_add_24, context.getTheme());
            icon.setBounds(0, 0, Tools.dpToPx(context, 24), Tools.dpToPx(context, 24));
            filterEntryTagsCat.setCompoundDrawablesWithIntrinsicBounds(null, null, icon, null);
            filterTagCount.setVisibility(View.GONE);
            tagFilterInfo.setVisibility(View.GONE);
        }
        else {
            filterEntryTagsCat.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
            filterTagCount.setVisibility(View.VISIBLE);
            tagFilterInfo.setVisibility(View.VISIBLE);
        }
    }

    public AppliedFilter getFilters() {
        int moodIndex = getSelectedRadio(filterDreamMoodRadioGroup)-1;
        int clarityIndex = getSelectedRadio(filterDreamClarityRadioGroup)-1;
        int qualityIndex = getSelectedRadio(filterSleepQualityRadioGroup)-1;
        DreamMoods mood = moodIndex >= 0 ? DreamMoods.values()[moodIndex] : DreamMoods.None;
        DreamClarity clarity = clarityIndex >= 0 ? DreamClarity.values()[clarityIndex] : DreamClarity.None;
        SleepQuality quality = qualityIndex >= 0 ? SleepQuality.values()[qualityIndex] : SleepQuality.None;
        return new AppliedFilter(filterTagsList, filterDreamTypes, JournalTypes.None, mood, clarity, quality);
    }

    private int getSelectedRadio(RadioGroup radioGroup) {
        for (int i = 0; i < radioGroup.getChildCount(); i++) {
            if(((MaterialRadioButton) radioGroup.getChildAt(i)).isChecked()){
                return i;
            }
        }
        return -1;
    }

    public void setOnClickPositiveButton(DialogInterface.OnClickListener listener) {
        okClickListener = listener;
    }

    private void updateStatesWhenLoaded(View view) {
        ViewTreeObserver viewTreeObserver = view.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    setCurrentFilterCategoryStates(view);
                }
            });
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        dismiss();
    }
}
