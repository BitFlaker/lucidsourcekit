package com.bitflaker.lucidsourcekit.main.dreamjournal;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.slider.Slider;

public class DreamJournalRatingEditor extends Fragment {
    OnBackButtonClicked mBackButtonListener;
    OnDoneButtonClicked mDoneButtonListener;
    ImageButton backToDreamEditor;
    MaterialButton doneRatingBtn;
    MaterialCardView cardDreamMood, cardSleepQuality, cardDreamClarity, cardDreamCharacteristics;
    SelectedRating selectedRating = SelectedRating.DREAM_MOOD;
    ImageView dotSleepQuality, dotDreamClarity, dotDreamCharacteristics;
    View lineBottomDreamMood, lineTopSleepQuality, lineBottomSleepQuality, lineTopDreamClarity, lineBottomDreamClarity, lineTopDreamCharacteristics;
    Slider sliderDreamMood, sliderSleepQuality, sliderDreamClarity;
    LinearLayout indicatorsDreamMood, indicatorsSleepQuality, indicatorsDreamClarity, containerDreamCharacteristics, dreamCharacteristicsIcons;
    TextView textDreamMood, textSleepQuality, textDreamClarity;
    ImageView previewSelectionDreamMood, previewSelectionDreamClarity, previewSelectionSleepQuality;
    boolean qualityPressed = false, clarityPressed = false;
    ImageView[] dreamMoods = new ImageView[5];
    String[] dreamMoodLabels = new String[] { "Terrible", "Poor", "Okay", "Great", "Outstanding"};
    ImageView[] sleepQualities = new ImageView[4];
    String[] sleepQualityLabels = new String[] { "Terrible", "Poor", "Great", "Outstanding"};
    ImageView[] dreamClarities = new ImageView[4];
    String[] dreamClarityLabels = new String[] { "Very Cloudy", "Cloudy", "Clear", "Crystal Clear"};
    ToggleButton toggleNightmare, toggleParalysis, toggleLucid, toggleRecurring, toggleFalseAwakening;
    ImageView characterNightmare, characterParalysis, characterLucid, characterRecurring, characterFalseAwakening;
    private JournalInMemoryManager journalManger;
    private String journalEntryId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_dream_journal_rating_editor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        backToDreamEditor = getView().findViewById(R.id.btn_dj_back_to_text);
        doneRatingBtn = getView().findViewById(R.id.btn_dj_done_rating);
        cardDreamMood = getView().findViewById(R.id.mcv_dj_dream_mood);
        cardSleepQuality = getView().findViewById(R.id.mcv_dj_sleep_quality);
        cardDreamClarity = getView().findViewById(R.id.mcv_dj_dream_clarity);
        cardDreamCharacteristics = getView().findViewById(R.id.mcv_dj_dream_characteristics);
        dotSleepQuality = getView().findViewById(R.id.vw_dot_dj_sleep_quality);
        dotDreamClarity = getView().findViewById(R.id.vw_dot_dj_dream_clarity);
        dotDreamCharacteristics = getView().findViewById(R.id.vw_dot_dj_dream_characteristics);
        lineBottomDreamMood = getView().findViewById(R.id.vw_line_dj_bottom_dream_mood);
        lineTopSleepQuality = getView().findViewById(R.id.vw_line_dj_top_sleep_quality);
        lineBottomSleepQuality = getView().findViewById(R.id.vw_line_dj_bottom_sleep_quality);
        lineTopDreamClarity = getView().findViewById(R.id.vw_line_dj_top_dream_clarity);
        lineBottomDreamClarity = getView().findViewById(R.id.vw_line_dj_bottom_dream_clarity);
        lineTopDreamCharacteristics = getView().findViewById(R.id.vw_line_dj_top_dream_characteristics);
        sliderDreamMood = getView().findViewById(R.id.sld_dj_dream_mood);
        sliderSleepQuality = getView().findViewById(R.id.sld_dj_sleep_quality);
        sliderDreamClarity = getView().findViewById(R.id.sld_dj_dream_clarity);
        indicatorsDreamMood = getView().findViewById(R.id.ll_dj_dream_mood);
        indicatorsSleepQuality = getView().findViewById(R.id.ll_dj_sleep_quality);
        indicatorsDreamClarity = getView().findViewById(R.id.ll_dj_dream_clarity);
        containerDreamCharacteristics = getView().findViewById(R.id.ll_dj_dream_characteristics);
        textSleepQuality = getView().findViewById(R.id.txt_dj_sleep_quality_selection_text);
        textDreamMood = getView().findViewById(R.id.txt_dj_dream_mood_selection_text);
        textDreamClarity = getView().findViewById(R.id.txt_dj_dream_clarity_selection_text);
        previewSelectionSleepQuality = getView().findViewById(R.id.img_dj_selection_sleep_quality);
        previewSelectionDreamClarity = getView().findViewById(R.id.img_dj_selection_dream_clarity);
        previewSelectionDreamMood = getView().findViewById(R.id.img_dj_selection_dream_mood);
        dreamCharacteristicsIcons = getView().findViewById(R.id.ll_dj_dream_characteristics_icons);
        toggleNightmare = getView().findViewById(R.id.tgl_dj_nightmare);
        toggleParalysis = getView().findViewById(R.id.tgl_dj_paralysis);
        toggleLucid = getView().findViewById(R.id.tgl_dj_lucid);
        toggleRecurring = getView().findViewById(R.id.tgl_dj_recurring);
        toggleFalseAwakening = getView().findViewById(R.id.tgl_dj_false_awakening);
        characterNightmare = getView().findViewById(R.id.img_dj_character_nightmare);
        characterParalysis = getView().findViewById(R.id.img_dj_character_paralysis);
        characterLucid = getView().findViewById(R.id.img_dj_character_lucid);
        characterRecurring = getView().findViewById(R.id.img_dj_character_recurring);
        characterFalseAwakening = getView().findViewById(R.id.img_dj_character_false_awakening);

        dreamMoods[0] = getView().findViewById(R.id.img_very_dissatisfied);
        dreamMoods[1] = getView().findViewById(R.id.img_dissatisfied);
        dreamMoods[2] = getView().findViewById(R.id.img_neutral_satisfied);
        dreamMoods[3] = getView().findViewById(R.id.img_satisfied);
        dreamMoods[4] = getView().findViewById(R.id.img_very_satisfied);

        sleepQualities[0] = getView().findViewById(R.id.img_very_bad_quality);
        sleepQualities[1] = getView().findViewById(R.id.img_bad_quality);
        sleepQualities[2] = getView().findViewById(R.id.img_good_quality);
        sleepQualities[3] = getView().findViewById(R.id.img_very_good_quality);

        dreamClarities[0] = getView().findViewById(R.id.img_very_unclear);
        dreamClarities[1] = getView().findViewById(R.id.img_unclear);
        dreamClarities[2] = getView().findViewById(R.id.img_clear);
        dreamClarities[3] = getView().findViewById(R.id.img_very_clear);

        toggleNightmare.setOnClickListener(e -> characterNightmare.setVisibility(toggleNightmare.isChecked() ? View.VISIBLE : View.GONE));
        toggleParalysis.setOnClickListener(e -> characterParalysis.setVisibility(toggleParalysis.isChecked() ? View.VISIBLE : View.GONE));
        toggleLucid.setOnClickListener(e -> characterLucid.setVisibility(toggleLucid.isChecked() ? View.VISIBLE : View.GONE));
        toggleRecurring.setOnClickListener(e -> characterRecurring.setVisibility(toggleRecurring.isChecked() ? View.VISIBLE : View.GONE));
        toggleFalseAwakening.setOnClickListener(e -> characterFalseAwakening.setVisibility(toggleFalseAwakening.isChecked() ? View.VISIBLE : View.GONE));

        sliderDreamMood.addOnChangeListener((slider, value, fromUser) -> {
            deselectAllDreamMoodIcons();
            textDreamMood.setText(dreamMoodLabels[(int)value]);
            previewSelectionDreamMood.setImageDrawable(dreamMoods[(int)value].getDrawable());
            dreamMoods[(int)value].setImageTintList(Tools.getAttrColorStateList(R.attr.primaryTextColor, getContext().getTheme()));
            dreamMoods[(int)value].invalidate();
        });
        sliderDreamMood.setOnTouchListener((View.OnTouchListener) (view1, motionEvent) -> {
            int action = motionEvent.getAction();
            if (action==MotionEvent.ACTION_UP) { focusCardSleepQuality(); }
            return false;
        });
        sliderSleepQuality.addOnChangeListener((slider, value, fromUser) -> {
            deselectAllSleepQualityIcons();
            textSleepQuality.setText(sleepQualityLabels[(int)value]);
            previewSelectionSleepQuality.setImageDrawable(sleepQualities[(int)value].getDrawable());
            sleepQualities[(int)value].setImageTintList(Tools.getAttrColorStateList(R.attr.primaryTextColor, getContext().getTheme()));
            sleepQualities[(int)value].invalidate();
        });
        sliderSleepQuality.setOnTouchListener((View.OnTouchListener) (view1, motionEvent) -> {
            int action = motionEvent.getAction();
            if (action==MotionEvent.ACTION_UP) { focusCardDreamClarity(); }
            return false;
        });
        sliderDreamClarity.addOnChangeListener((slider, value, fromUser) -> {
            deselectAllDreamClarityIcons();
            textDreamClarity.setText(dreamClarityLabels[(int)value]);
            previewSelectionDreamClarity.setImageDrawable(dreamClarities[(int)value].getDrawable());
            dreamClarities[(int)value].setImageTintList(Tools.getAttrColorStateList(R.attr.primaryTextColor, getContext().getTheme()));
            dreamClarities[(int)value].invalidate();
        });
        sliderDreamClarity.setOnTouchListener((View.OnTouchListener) (view1, motionEvent) -> {
            int action = motionEvent.getAction();
            if (action==MotionEvent.ACTION_UP) { focusCardDreamCharacteristics(); }
            return false;
        });

        backToDreamEditor.setOnClickListener(e -> {
            if(mBackButtonListener != null) {
                mBackButtonListener.onEvent();
            }
        });
        doneRatingBtn.setOnClickListener(e -> {
            if(mDoneButtonListener != null) {
                mDoneButtonListener.onEvent();
            }
        });

        cardDreamMood.setOnClickListener(e -> {
            if(selectedRating != SelectedRating.DREAM_MOOD){
                deselectLast();
                selectedRating = SelectedRating.DREAM_MOOD;
                cardDreamMood.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.colorPrimary, getContext().getTheme()));
                sliderDreamMood.setVisibility(View.VISIBLE);
                indicatorsDreamMood.setVisibility(View.VISIBLE);
                previewSelectionDreamMood.setVisibility(View.GONE);
            }
        });
        cardSleepQuality.setOnClickListener(e -> focusCardSleepQuality());
    }

    private void focusCardSleepQuality() {
        if(selectedRating != SelectedRating.SLEEP_QUALITY){
            deselectLast();
            selectedRating = SelectedRating.SLEEP_QUALITY;
            cardSleepQuality.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.colorPrimary, getContext().getTheme()));
            cardDreamClarity.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.slightElevated, getContext().getTheme()));
            dotSleepQuality.setImageTintList(Tools.getAttrColorStateList(R.attr.colorPrimary, getContext().getTheme()));
            lineBottomDreamMood.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.colorPrimary, getContext().getTheme()));
            lineTopSleepQuality.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.colorPrimary, getContext().getTheme()));
            if (!qualityPressed) {
                qualityPressed = true;
                lineBottomSleepQuality.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.tertiaryTextColor, getContext().getTheme()));
                lineTopDreamClarity.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.tertiaryTextColor, getContext().getTheme()));
                dotDreamClarity.setImageTintList(Tools.getAttrColorStateList(R.attr.tertiaryTextColor, getContext().getTheme()));
            }
            sliderSleepQuality.setVisibility(View.VISIBLE);
            indicatorsSleepQuality.setVisibility(View.VISIBLE);
            previewSelectionSleepQuality.setVisibility(View.GONE);
            textSleepQuality.setVisibility(View.VISIBLE);

            if(!cardDreamClarity.isClickable()) {
                cardDreamClarity.setClickable(true);
                cardDreamClarity.setOnClickListener(e1 -> focusCardDreamClarity());
            }
        }
    }

    private void focusCardDreamClarity() {
        if(selectedRating != SelectedRating.DREAM_CLARITY){
            deselectLast();
            selectedRating = SelectedRating.DREAM_CLARITY;
            cardDreamClarity.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.colorPrimary, getContext().getTheme()));
            cardDreamCharacteristics.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.slightElevated, getContext().getTheme()));
            dotDreamClarity.setImageTintList(Tools.getAttrColorStateList(R.attr.colorPrimary, getContext().getTheme()));
            lineBottomSleepQuality.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.colorPrimary, getContext().getTheme()));
            lineTopDreamClarity.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.colorPrimary, getContext().getTheme()));
            if(!clarityPressed) {
                clarityPressed = true;
                lineTopDreamCharacteristics.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.tertiaryTextColor, getContext().getTheme()));
                lineBottomDreamClarity.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.tertiaryTextColor, getContext().getTheme()));
                dotDreamCharacteristics.setImageTintList(Tools.getAttrColorStateList(R.attr.tertiaryTextColor, getContext().getTheme()));
            }
            sliderDreamClarity.setVisibility(View.VISIBLE);
            indicatorsDreamClarity.setVisibility(View.VISIBLE);
            previewSelectionDreamClarity.setVisibility(View.GONE);
            textDreamClarity.setVisibility(View.VISIBLE);

            if(!cardDreamCharacteristics.isClickable()) {
                cardDreamCharacteristics.setClickable(true);
                cardDreamCharacteristics.setOnClickListener(e2 -> focusCardDreamCharacteristics());
            }
        }
    }

    private void focusCardDreamCharacteristics() {
        if (selectedRating != SelectedRating.DREAM_CHARACTERISTICS) {
            deselectLast();
            selectedRating = SelectedRating.DREAM_CHARACTERISTICS;
            cardDreamCharacteristics.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.colorPrimary, getContext().getTheme()));
            dotDreamCharacteristics.setImageTintList(Tools.getAttrColorStateList(R.attr.colorPrimary, getContext().getTheme()));
            lineBottomDreamClarity.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.colorPrimary, getContext().getTheme()));
            lineTopDreamCharacteristics.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.colorPrimary, getContext().getTheme()));
            containerDreamCharacteristics.setVisibility(View.VISIBLE);
            dreamCharacteristicsIcons.setVisibility(View.GONE);
        }
    }

    private void deselectAllDreamMoodIcons() {
        for (ImageView img : dreamMoods) {
            img.setImageTintList(Tools.getAttrColorStateList(R.attr.tertiaryTextColor, getContext().getTheme()));
            img.invalidate();
        }
    }

    private void deselectAllSleepQualityIcons() {
        for (ImageView img : sleepQualities) {
            img.setImageTintList(Tools.getAttrColorStateList(R.attr.tertiaryTextColor, getContext().getTheme()));
            img.invalidate();
        }
    }

    private void deselectAllDreamClarityIcons() {
        for (ImageView img : dreamClarities) {
            img.setImageTintList(Tools.getAttrColorStateList(R.attr.tertiaryTextColor, getContext().getTheme()));
            img.invalidate();
        }
    }

    private void deselectLast() {
        switch (selectedRating){
            case DREAM_MOOD:
                cardDreamMood.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.slightElevated, getContext().getTheme()));
                sliderDreamMood.setVisibility(View.GONE);
                indicatorsDreamMood.setVisibility(View.GONE);
                previewSelectionDreamMood.setVisibility(View.VISIBLE);
                break;
            case SLEEP_QUALITY:
                cardSleepQuality.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.slightElevated, getContext().getTheme()));
                sliderSleepQuality.setVisibility(View.GONE);
                indicatorsSleepQuality.setVisibility(View.GONE);
                previewSelectionSleepQuality.setVisibility(View.VISIBLE);
                break;
            case DREAM_CLARITY:
                cardDreamClarity.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.slightElevated, getContext().getTheme()));
                sliderDreamClarity.setVisibility(View.GONE);
                indicatorsDreamClarity.setVisibility(View.GONE);
                previewSelectionDreamClarity.setVisibility(View.VISIBLE);
                break;
            case DREAM_CHARACTERISTICS:
                cardDreamCharacteristics.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.slightElevated, getContext().getTheme()));
                containerDreamCharacteristics.setVisibility(View.GONE);
                dreamCharacteristicsIcons.setVisibility(View.VISIBLE);
                break;
        }
    }

    public void setJournalEntryId(String id) {
        journalEntryId = id;
    }

    public interface OnBackButtonClicked {
        void onEvent();
    }

    public void setOnBackButtonClicked(OnBackButtonClicked eventListener) {
        mBackButtonListener = eventListener;
    }

    public interface OnDoneButtonClicked {
        void onEvent();
    }

    public void setOnDoneButtonClicked(OnDoneButtonClicked eventListener) {
        mDoneButtonListener = eventListener;
    }

    private enum SelectedRating {
        DREAM_MOOD,
        SLEEP_QUALITY,
        DREAM_CLARITY,
        DREAM_CHARACTERISTICS
    }
}