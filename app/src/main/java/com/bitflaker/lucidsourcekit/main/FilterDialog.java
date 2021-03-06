package com.bitflaker.lucidsourcekit.main;

import static android.view.inputmethod.EditorInfo.IME_ACTION_DONE;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.widget.TextViewCompat;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.general.JournalTypes;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.bitflaker.lucidsourcekit.general.database.values.DreamClarity;
import com.bitflaker.lucidsourcekit.general.database.values.DreamMoods;
import com.bitflaker.lucidsourcekit.general.database.values.SleepQuality;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FilterDialog extends Dialog implements android.view.View.OnClickListener {
    private MaterialButton ok, cancel, filterDreamMoodCat, filterDreamClarityCat, filterSleepQualityCat, filterJournalTypeCat, filterDreamTypeCat, filterEntryTagsCat;
    private AutoCompleteTextView filterTags;
    private final String[] tags;
    private View.OnClickListener okClickListener;
    private List<String> filterTagsList;
    private boolean[] filterDreamTypes;
    private AppliedFilter currentFilter;
    private RadioGroup filterDreamMoodRadioGroup, filterDreamClarityRadioGroup, filterSleepQualityRadioGroup, filterJournalTypeRadioGroup;
    private LinearLayout filterDreamType, filterEntryTag;

    public FilterDialog(Activity activity, String[] tags, AppliedFilter currentFilter) {
        super(activity, Tools.getThemeDialog());
        this.tags = tags;
        this.currentFilter = currentFilter;
        filterDreamTypes = currentFilter.getFilterDreamTypes();
        filterTagsList = new ArrayList<>();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_filter);

        setData();
        setupSimpleCategoryListeners();
        setupTagsCategoryListener();
        for (String filterTag : currentFilter.getFilterTagsList()) { addTagFilterEntry(filterTag); }
        setupStateWatchListeners();
        setCurrentFilterCategoryStates();

        ok.setOnClickListener(okClickListener);
        cancel.setOnClickListener(this);
    }

    private void setCurrentFilterCategoryStates() {
        checkboxSelectionWatcher(filterDreamTypeCat, filterDreamType);
        checkboxSelectionWatcher(filterEntryTagsCat, filterEntryTag);
        radioSelectionWatcher(filterJournalTypeCat, filterJournalTypeRadioGroup);
        radioSelectionWatcher(filterDreamMoodCat, filterDreamMoodRadioGroup);
        radioSelectionWatcher(filterDreamClarityCat, filterDreamClarityRadioGroup);
        radioSelectionWatcher(filterSleepQualityCat, filterSleepQualityRadioGroup);
    }

    private void setupStateWatchListeners() {
        for (int i = 0; i < filterJournalTypeRadioGroup.getChildCount(); i++){
            ((RadioButton) filterJournalTypeRadioGroup.getChildAt(i)).setOnCheckedChangeListener((compoundButton, b) -> radioSelectionWatcher(filterJournalTypeCat, filterJournalTypeRadioGroup));
        }
        for (int i = 0; i < filterDreamMoodRadioGroup.getChildCount(); i++){
            ((RadioButton) filterDreamMoodRadioGroup.getChildAt(i)).setOnCheckedChangeListener((compoundButton, b) -> radioSelectionWatcher(filterDreamMoodCat, filterDreamMoodRadioGroup));
        }
        for (int i = 0; i < filterDreamClarityRadioGroup.getChildCount(); i++){
            ((RadioButton) filterDreamClarityRadioGroup.getChildAt(i)).setOnCheckedChangeListener((compoundButton, b) -> radioSelectionWatcher(filterDreamClarityCat, filterDreamClarityRadioGroup));
        }
        for (int i = 0; i < filterSleepQualityRadioGroup.getChildCount(); i++){
            ((RadioButton) filterSleepQualityRadioGroup.getChildAt(i)).setOnCheckedChangeListener((compoundButton, b) -> radioSelectionWatcher(filterSleepQualityCat, filterSleepQualityRadioGroup));
        }
    }

    private void setupTagsCategoryListener() {
        filterTags.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, tags));
        filterTags.setOnEditorActionListener((textView, i, keyEvent) -> {
            String enteredTag = filterTags.getText().toString();
            if(i == IME_ACTION_DONE && Arrays.asList(tags).contains(enteredTag) && !filterTagsList.contains(enteredTag)){
                addTagFilterEntry(enteredTag);
            }
            return true;
        });
        filterTags.setOnItemClickListener((adapterView, view, i, l) -> {
            String enteredTag = filterTags.getText().toString();
            if(Arrays.asList(tags).contains(enteredTag) && !filterTagsList.contains(enteredTag)){
                addTagFilterEntry(enteredTag);
            }
        });
    }

    private void setupSimpleCategoryListeners() {
        filterDreamMoodCat.setOnClickListener(e -> openCloseExpander(filterDreamMoodRadioGroup));
        ((RadioButton) filterDreamMoodRadioGroup.getChildAt((currentFilter.getDreamMood().ordinal() + 1) % DreamMoods.values().length)).setChecked(true);
        filterDreamClarityCat.setOnClickListener(e -> openCloseExpander(filterDreamClarityRadioGroup));
        ((RadioButton) filterDreamClarityRadioGroup.getChildAt((currentFilter.getDreamClarity().ordinal() + 1) % DreamClarity.values().length)).setChecked(true);
        filterSleepQualityCat.setOnClickListener(e -> openCloseExpander(filterSleepQualityRadioGroup));
        ((RadioButton) filterSleepQualityRadioGroup.getChildAt((currentFilter.getSleepQuality().ordinal() + 1) % SleepQuality.values().length)).setChecked(true);
        filterJournalTypeCat.setOnClickListener(e -> openCloseExpander(filterJournalTypeRadioGroup));
        ((RadioButton) filterJournalTypeRadioGroup.getChildAt((currentFilter.getJournalType().ordinal() + 1) % JournalTypes.values().length)).setChecked(true);
        filterDreamTypeCat.setOnClickListener(e -> openCloseExpander(filterDreamType));
        setCheckedStateInList(filterDreamType, currentFilter.getFilterDreamTypes());
        filterEntryTagsCat.setOnClickListener(e -> openCloseExpander(filterEntryTag));
    }

    private void setData() {
        ok = findViewById(R.id.btn_filter_ok);
        cancel = findViewById(R.id.btn_filter_cancel);
        filterTags = findViewById(R.id.actv_filter_tags);
        filterDreamMoodCat = findViewById(R.id.btn_filter_dream_mood);
        filterDreamMoodRadioGroup = findViewById(R.id.rdg_dm);
        filterDreamClarityCat = findViewById(R.id.btn_filter_dream_clarity);
        filterDreamClarityRadioGroup = findViewById(R.id.rdg_dc);
        filterSleepQualityCat = findViewById(R.id.btn_filter_sleep_quality);
        filterSleepQualityRadioGroup = findViewById(R.id.rdg_sq);
        filterJournalTypeCat = findViewById(R.id.btn_filter_journal_type);
        filterJournalTypeRadioGroup = findViewById(R.id.rdg_jt);
        filterDreamTypeCat = findViewById(R.id.btn_filter_dream_type);
        filterDreamType = findViewById(R.id.ll_dt);
        filterEntryTag = findViewById(R.id.ll_et);
        filterEntryTagsCat = findViewById(R.id.btn_filter_entry_tags);
    }

    private void radioSelectionWatcher(MaterialButton categoryButton, RadioGroup container) {
        if(((RadioButton) container.getChildAt(0)).isChecked()){
            TextViewCompat.setCompoundDrawableTintList(categoryButton, Tools.getAttrColorStateList(R.attr.slightElevated, getContext().getTheme()));
        }
        else {
            TextViewCompat.setCompoundDrawableTintList(categoryButton, Tools.getAttrColorStateList(R.attr.colorPrimary, getContext().getTheme()));
        }
    }

    private void checkboxSelectionWatcher(MaterialButton categoryButton, LinearLayout container) {
        boolean anythingChecked = false;
        for (int i = 0; i < container.getChildCount(); i++) {
            View childView = container.getChildAt(i);
            if(childView instanceof CheckBox && ((CheckBox) childView).isChecked()){
                anythingChecked = true;
                break;
            }
        }
        if(anythingChecked){
            TextViewCompat.setCompoundDrawableTintList(categoryButton, Tools.getAttrColorStateList(R.attr.colorPrimary, getContext().getTheme()));
        }
        else{
            TextViewCompat.setCompoundDrawableTintList(categoryButton, Tools.getAttrColorStateList(R.attr.slightElevated, getContext().getTheme()));
        }
    }

    private void setCheckedStateInList(LinearLayout container, boolean[] values) {
        for (int i = 0; i < container.getChildCount(); i++){
            CheckBox chk = (CheckBox) container.getChildAt(i);
            if(values[i]) { chk.setChecked(true); }
            chk.setOnCheckedChangeListener((compoundButton, b) -> {
                filterDreamTypes[getIdInContainer(container, chk)] = b;
                checkboxSelectionWatcher(filterDreamTypeCat, filterDreamType);
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

    private void openCloseExpander(View filterSleepQualityRadioGroup) {
        if (filterSleepQualityRadioGroup.getVisibility() == View.GONE) {
            filterSleepQualityRadioGroup.setVisibility(View.VISIBLE);
        } else {
            filterSleepQualityRadioGroup.setVisibility(View.GONE);
        }
    }

    private void addTagFilterEntry(String enteredTag) {
        CheckBox tag = generateCheckbox(enteredTag, R.drawable.ic_baseline_label_24);
        tag.setChecked(true);
        tag.setOnCheckedChangeListener((compoundButton, b) -> {
            if(b) { filterTagsList.add(enteredTag); }
            else { filterTagsList.remove(enteredTag); }
            checkboxSelectionWatcher(filterEntryTagsCat, filterEntryTag);
        });
        filterEntryTag.addView(tag);
        filterTagsList.add(enteredTag);
        filterTags.setText("");
        checkboxSelectionWatcher(filterEntryTagsCat, filterEntryTag);
    }

    private CheckBox generateCheckbox(String text, int icon) {
        CheckBox chk = new CheckBox(getContext());
        chk.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        chk.setCompoundDrawablesWithIntrinsicBounds(null, null, AppCompatResources.getDrawable(getContext(), icon), null);
        chk.setText(text);
        return chk;
    }

    public AppliedFilter getFilters() {
        int typeIndex = getSelectedRadio(filterJournalTypeRadioGroup)-1;
        int moodIndex = getSelectedRadio(filterDreamMoodRadioGroup)-1;
        int clarityIndex = getSelectedRadio(filterDreamClarityRadioGroup)-1;
        int qualityIndex = getSelectedRadio(filterSleepQualityRadioGroup)-1;
        JournalTypes type = typeIndex >= 0 ? JournalTypes.values()[typeIndex] : JournalTypes.None;
        DreamMoods mood = moodIndex >= 0 ? DreamMoods.values()[moodIndex] : DreamMoods.None;
        DreamClarity clarity = clarityIndex >= 0 ? DreamClarity.values()[clarityIndex] : DreamClarity.None;
        SleepQuality quality = qualityIndex >= 0 ? SleepQuality.values()[qualityIndex] : SleepQuality.None;
        return new AppliedFilter(filterTagsList, filterDreamTypes, type, mood, clarity, quality);
    }

    private int getSelectedRadio(RadioGroup radioGroup) {
        for (int i = 0; i < radioGroup.getChildCount(); i++) {
            if(((RadioButton) radioGroup.getChildAt(i)).isChecked()){
                return i;
            }
        }
        return -1;
    }

    public void setOnClickPositiveButton(View.OnClickListener listener) {
        okClickListener = listener;
    }

    @Override
    public void onClick(View view) {
        dismiss();
    }
}
