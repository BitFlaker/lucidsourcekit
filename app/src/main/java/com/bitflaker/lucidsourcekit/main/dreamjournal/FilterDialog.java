package com.bitflaker.lucidsourcekit.main.dreamjournal;

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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.DialogFragment;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.data.enums.journalratings.DreamClarity;
import com.bitflaker.lucidsourcekit.data.enums.journalratings.DreamMoods;
import com.bitflaker.lucidsourcekit.data.enums.journalratings.SleepQuality;
import com.bitflaker.lucidsourcekit.data.records.AppliedFilter;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.DreamType;
import com.bitflaker.lucidsourcekit.databinding.DialogFilterBinding;
import com.bitflaker.lucidsourcekit.utils.Tools;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.radiobutton.MaterialRadioButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class FilterDialog extends DialogFragment implements DialogInterface.OnClickListener {
    private final String[] tags;
    private final List<String> filterTagsList;
    private final List<String> filterDreamTypes;
    private final AppliedFilter currentFilter;
    private final Context context;
    private final HashMap<View, MaterialButton> titleButtonMap = new HashMap<>();
    private HashMap<MaterialButton, View> optionsEntryMap = new HashMap<>();
    private DialogInterface.OnClickListener okClickListener;
    private DialogFilterBinding binding;

    public FilterDialog(Context context, String[] tags, AppliedFilter currentFilter) {
        this.tags = tags;
        this.currentFilter = currentFilter;
        this.context = context;
        filterDreamTypes = currentFilter.dreamTypes();
        filterTagsList = new ArrayList<>();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = DialogFilterBinding.inflate(getLayoutInflater());

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity());
        builder.setView(binding.getRoot());
        builder.setPositiveButton(R.string.ok, okClickListener).setNegativeButton(R.string.cancel, this);

        binding.chpGrpFilterTags.removeAllViews();
        binding.txtTagFilterInfo.setVisibility(View.GONE);
        TextViewCompat.setCompoundDrawableTintList(binding.btnFilterEntryTags, Tools.getAttrColorStateList(R.attr.colorOutlineVariant, context.getTheme()));

        titleButtonMap.put(binding.llContentDt, binding.imgTitleImageDreamType);
        titleButtonMap.put(binding.llContentDm, binding.imgTitleImageDreamMood);
        titleButtonMap.put(binding.llContentDc, binding.imgTitleImageDreamClarity);
        titleButtonMap.put(binding.llContentSq, binding.imgTitleImageSleepQuality);
        optionsEntryMap = new HashMap<>(titleButtonMap.keySet().stream().collect(Collectors.toMap(titleButtonMap::get, k -> k)));
        optionsEntryMap.keySet().forEach(b -> b.setOnClickListener(this::openFilterFromTitle));

        setupSimpleCategoryListeners();
        setupTagsCategoryListener();
        for (String filterTag : currentFilter.filterTagsList()) { addTagFilterEntry(filterTag); }
        setupRadioChangedListeners();
        updateStatesWhenLoaded();

        return builder.create();
    }

    private void openFilterFromTitle(View view) {
        optionsEntryMap.values().forEach(v -> v.setVisibility(View.GONE));
        optionsEntryMap.get(view).setVisibility(View.VISIBLE);
    }

    private void setCurrentFilterCategoryStates() {
        checkboxSelectionChanged(binding.btnFilterDreamType, binding.llDt, binding.llContentDt);
        radioSelectionChanged(binding.btnFilterDreamMood, binding.rdgDm, binding.llContentDm);
        radioSelectionChanged(binding.btnFilterDreamClarity, binding.rdgDc, binding.llContentDc);
        radioSelectionChanged(binding.btnFilterSleepQuality, binding.rdgSq, binding.llContentSq);
    }

    private void setupRadioChangedListeners() {
        setupRadioSelectionChangedListeners(binding.rdgDm, binding.btnFilterDreamMood, binding.llContentDm);
        setupRadioSelectionChangedListeners(binding.rdgDc, binding.btnFilterDreamClarity, binding.llContentDc);
        setupRadioSelectionChangedListeners(binding.rdgSq, binding.btnFilterSleepQuality, binding.llContentSq);
    }

    private void setupRadioSelectionChangedListeners(RadioGroup group, MaterialButton category, LinearLayout content) {
        for (int i = 0; i < group.getChildCount(); i++) {
            if (group.getChildAt(i) instanceof MaterialRadioButton button) {
                button.setOnCheckedChangeListener((compoundButton, isSelected) -> {
                    if (isSelected) {
                        radioSelectionChanged(category, group, content);
                    }
                });
            }
        }
    }

    private void setupTagsCategoryListener() {
        binding.actvFilterTags.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, tags));
        binding.actvFilterTags.setOnItemClickListener((adapterView, view, i, l) -> addTagToFilter());
        binding.actvFilterTags.setOnEditorActionListener((textView, i, keyEvent) -> {
            if(i == IME_ACTION_DONE) addTagToFilter();
            return true;
        });
    }

    private void addTagToFilter() {
        String enteredTag = binding.actvFilterTags.getText().toString();
        if(Arrays.asList(tags).contains(enteredTag) && !filterTagsList.contains(enteredTag)){
            addTagFilterEntry(enteredTag);
        }
    }

    private void setupSimpleCategoryListeners() {
        binding.btnFilterDreamMood.setOnClickListener(e -> openCloseExpander(binding.llContentDm));
        binding.btnFilterDreamClarity.setOnClickListener(e -> openCloseExpander(binding.llContentDc));
        binding.btnFilterSleepQuality.setOnClickListener(e -> openCloseExpander(binding.llContentSq));
        binding.btnFilterDreamType.setOnClickListener(e -> openCloseExpander(binding.llContentDt));
        binding.btnFilterEntryTags.setOnClickListener(e -> openCloseExpander(binding.llContentTags));

        // Setting default values
        ((MaterialRadioButton) binding.rdgDm.getChildAt((currentFilter.dreamMood().ordinal() + 1) % DreamMoods.values().length)).setChecked(true);
        ((MaterialRadioButton) binding.rdgDc.getChildAt((currentFilter.dreamClarity().ordinal() + 1) % DreamClarity.values().length)).setChecked(true);
        ((MaterialRadioButton) binding.rdgSq.getChildAt((currentFilter.sleepQuality().ordinal() + 1) % SleepQuality.values().length)).setChecked(true);
        setCheckedStateInList(binding.llDt, Arrays.stream(DreamType.populateData()).map(x -> x.typeId).collect(Collectors.toList()));
    }

    private void radioSelectionChanged(MaterialButton categoryButton, RadioGroup radioGroup, LinearLayout container) {
        binding.actvFilterTags.clearFocus();
        MaterialRadioButton doNotFilterOption = (MaterialRadioButton) radioGroup.getChildAt(0);
        MaterialRadioButton selected = binding.getRoot().findViewById(radioGroup.getCheckedRadioButtonId());
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
        binding.actvFilterTags.clearFocus();
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

    // TODO: The order of the checkboxes can get out of sync with the dreamTypes list order when the checkbox order changes! This should not be possible.
    private void setCheckedStateInList(LinearLayout container, List<String> dreamTypes) {
        for (int i = 0; i < container.getChildCount(); i++) {
            CheckBox chk = (CheckBox) container.getChildAt(i);
            if(filterDreamTypes.contains(dreamTypes.get(i))) { chk.setChecked(true); }
            int finalI = i;
            chk.setOnCheckedChangeListener((compoundButton, b) -> {
                String dreamTypeId = dreamTypes.get(finalI);
                if (b) {
                    filterDreamTypes.add(dreamTypeId);
                }
                else {
                    filterDreamTypes.remove(dreamTypeId);
                }
                checkboxSelectionChanged(binding.btnFilterDreamType, binding.llDt, binding.llContentDt);
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
        binding.actvFilterTags.clearFocus();
        if (expanderContent.getVisibility() == View.GONE) {
            expanderContent.setVisibility(View.VISIBLE);
        } else {
            expanderContent.setVisibility(View.GONE);
        }
    }

    private void addTagFilterEntry(String enteredTag) {
        Chip titleChip = generateTagFilterChip(enteredTag, true);
        binding.chpGrpFilterTags.addView(titleChip);
        filterTagsList.add(enteredTag);
        binding.actvFilterTags.setText("");
        if (binding.txtNoTagsFiltered.getVisibility() == View.VISIBLE) {
            binding.txtNoTagsFiltered.setVisibility(View.GONE);
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
                binding.txtNoTagsFiltered.setVisibility(View.VISIBLE);
            }
            updateTagFilterCount();
        });
        return chip;
    }

    private void updateTagFilterCount() {
        int tagFilterCount = filterTagsList.size();
        binding.txtFilterTagCount.setText(String.format(Locale.getDefault(), "%d %s", tagFilterCount, (tagFilterCount == 1 ? "tag" : "tags")));
        binding.txtTagFilterInfo.setText(String.format(Locale.getDefault(), "%d %s in filter", tagFilterCount, (tagFilterCount == 1 ? "tag" : "tags")));
        if (tagFilterCount == 0) {
            Drawable icon = ResourcesCompat.getDrawable(context.getResources(), R.drawable.rounded_add_24, context.getTheme());
            icon.setBounds(0, 0, Tools.dpToPx(context, 24), Tools.dpToPx(context, 24));
            binding.btnFilterEntryTags.setCompoundDrawablesWithIntrinsicBounds(null, null, icon, null);
            binding.txtFilterTagCount.setVisibility(View.GONE);
            binding.txtTagFilterInfo.setVisibility(View.GONE);
        }
        else {
            binding.btnFilterEntryTags.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
            binding.txtFilterTagCount.setVisibility(View.VISIBLE);
            binding.txtTagFilterInfo.setVisibility(View.VISIBLE);
        }
    }

    public AppliedFilter getFilters() {
        int moodIndex = getSelectedRadio(binding.rdgDm) - 1;
        int clarityIndex = getSelectedRadio(binding.rdgDc) - 1;
        int qualityIndex = getSelectedRadio(binding.rdgSq) - 1;
        DreamMoods mood = moodIndex >= 0 ? DreamMoods.values()[moodIndex] : DreamMoods.None;
        DreamClarity clarity = clarityIndex >= 0 ? DreamClarity.values()[clarityIndex] : DreamClarity.None;
        SleepQuality quality = qualityIndex >= 0 ? SleepQuality.values()[qualityIndex] : SleepQuality.None;
        return new AppliedFilter(filterTagsList, filterDreamTypes, mood, clarity, quality);
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

    private void updateStatesWhenLoaded() {
        ViewTreeObserver viewTreeObserver = binding.getRoot().getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    binding.getRoot().getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    setCurrentFilterCategoryStates();
                }
            });
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        dismiss();
    }
}
