package com.bitflaker.lucidsourcekit.main.dreamjournal;

import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.ViewCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.AudioLocation;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntry;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntryHasTag;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntryHasType;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntryTag;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.bitflaker.lucidsourcekit.general.database.values.DreamClarity;
import com.bitflaker.lucidsourcekit.general.database.values.DreamMoods;
import com.bitflaker.lucidsourcekit.general.database.values.SleepQuality;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.slider.Slider;

import java.util.HashMap;
import java.util.List;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class DreamJournalRatingEditor extends Fragment {
    OnBackButtonClicked mBackButtonListener;
    OnDoneButtonClicked mDoneButtonListener;
    OnDreamJournalEntrySaved mDreamJournalEntrySaved;
    ImageButton backToDreamEditor, closeEditor;
    MaterialButton doneRatingBtn;
    Slider sliderDreamMood, sliderSleepQuality, sliderDreamClarity;
    ImageView previewDreamMood, previewDreamClarity, previewSleepQuality;
    ImageView[] dreamMoods = new ImageView[5];
    ImageView[] sleepQualities = new ImageView[4];
    ImageView[] dreamClarities = new ImageView[4];
    ToggleButton toggleNightmare, toggleParalysis, toggleLucid, toggleRecurring, toggleFalseAwakening;
    LinearLayout specialDreamIconsContainer;
    private final String[] dreamMoodLabels = new String[] { "Terrible", "Poor", "Okay", "Great", "Outstanding"};
    private final String[] sleepQualityLabels = new String[] { "Terrible", "Poor", "Great", "Outstanding"};
    private final String[] dreamClarityLabels = new String[] { "Very Cloudy", "Cloudy", "Clear", "Crystal Clear"};
    private ColorStateList tertiaryColor;
    private ColorStateList colorOnPrimary;
    private OnCloseButtonClicked mCloseButtonClicked;
    private JournalInMemoryManager journalManger;
    private String journalEntryId;
    private MainDatabase db;
    private HashMap<ToggleButton, ImageView> toggleButtonIcons;
    private TextView specialDreamHeading, specialDreamDescription;
    private MaterialCardView cardRatingsPreview;
    private NestedScrollView bottomSheet;
    private ImageView dragHandle;
    private BottomSheetBehavior<NestedScrollView> bottomSheetBehavior;
    private CompositeDisposable compositeDisposable;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dream_journal_rating_editor_new, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ConstraintLayout ratingHeading = getView().findViewById(R.id.cl_rating_heading);
        setAdjustedStatusBarMargin(ratingHeading);

        compositeDisposable = new CompositeDisposable();
        toggleButtonIcons = new HashMap<>();
        tertiaryColor = Tools.getAttrColorStateList(R.attr.tertiaryTextColor, getContext().getTheme());
        colorOnPrimary = Tools.getAttrColorStateList(R.attr.colorOnPrimary, getContext().getTheme());

        backToDreamEditor = getView().findViewById(R.id.btn_dj_back_to_text);
        doneRatingBtn = getView().findViewById(R.id.btn_dj_done_rating);
        closeEditor = getView().findViewById(R.id.btn_dj_close_editor);

        cardRatingsPreview = getView().findViewById(R.id.mcv_dream_rating_preview);
        bottomSheet = getView().findViewById(R.id.nsv_dream_rating_bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        dragHandle = getView().findViewById(R.id.img_drag_handle);

        sliderDreamMood = getView().findViewById(R.id.sld_dj_dream_mood);
        sliderSleepQuality = getView().findViewById(R.id.sld_dj_sleep_quality);
        sliderDreamClarity = getView().findViewById(R.id.sld_dj_dream_clarity);

        toggleNightmare = getView().findViewById(R.id.tgl_dj_nightmare);
        toggleParalysis = getView().findViewById(R.id.tgl_dj_paralysis);
        toggleLucid = getView().findViewById(R.id.tgl_dj_lucid);
        toggleRecurring = getView().findViewById(R.id.tgl_dj_recurring);
        toggleFalseAwakening = getView().findViewById(R.id.tgl_dj_false_awakening);

        previewDreamMood = getView().findViewById(R.id.img_preview_dream_mood);
        previewDreamClarity = getView().findViewById(R.id.img_preview_dream_clarity);
        previewSleepQuality = getView().findViewById(R.id.img_preview_sleep_quality);
        specialDreamIconsContainer = getView().findViewById(R.id.ll_special_dream_icons);

        specialDreamHeading = getView().findViewById(R.id.txt_special_dream_heading);
        specialDreamDescription = getView().findViewById(R.id.txt_special_dream_description);

        dreamMoods = new ImageView[] {
                getView().findViewById(R.id.img_very_dissatisfied),
                getView().findViewById(R.id.img_dissatisfied),
                getView().findViewById(R.id.img_neutral_satisfied),
                getView().findViewById(R.id.img_satisfied),
                getView().findViewById(R.id.img_very_satisfied)
        };
        sleepQualities = new ImageView[] {
                getView().findViewById(R.id.img_very_bad_quality),
                getView().findViewById(R.id.img_bad_quality),
                getView().findViewById(R.id.img_good_quality),
                getView().findViewById(R.id.img_very_good_quality)
        };
        dreamClarities = new ImageView[] {
                getView().findViewById(R.id.img_very_unclear),
                getView().findViewById(R.id.img_unclear),
                getView().findViewById(R.id.img_clear),
                getView().findViewById(R.id.img_very_clear)
        };

        db = MainDatabase.getInstance(getContext());
        JournalInMemory jim = journalManger.getEntry(journalEntryId);
        restoreStoredEntry(jim);

        toggleNightmare.setOnClickListener(e -> toggleSpecialDreamType(jim, (ToggleButton) e, JournalInMemory.SpecialDreamType.NIGHTMARE));
        toggleParalysis.setOnClickListener(e -> toggleSpecialDreamType(jim, (ToggleButton) e, JournalInMemory.SpecialDreamType.PARALYSIS));
        toggleLucid.setOnClickListener(e -> toggleSpecialDreamType(jim, (ToggleButton) e, JournalInMemory.SpecialDreamType.LUCID));
        toggleRecurring.setOnClickListener(e -> toggleSpecialDreamType(jim, (ToggleButton) e, JournalInMemory.SpecialDreamType.RECURRING));
        toggleFalseAwakening.setOnClickListener(e -> toggleSpecialDreamType(jim, (ToggleButton) e, JournalInMemory.SpecialDreamType.FALSE_AWAKENING));

        sliderDreamMood.setLabelFormatter(value -> dreamMoodLabels[(int)value]);
        sliderSleepQuality.setLabelFormatter(value -> sleepQualityLabels[(int)value]);
        sliderDreamClarity.setLabelFormatter(value -> dreamClarityLabels[(int)value]);

        sliderDreamMood.addOnChangeListener((slider, value, fromUser) -> {
            handleIconSlider((int) value, dreamMoods, previewDreamMood);
            jim.setDreamMood(DreamMoods.values()[(int) value].getId());
        });
        sliderSleepQuality.addOnChangeListener((slider, value, fromUser) -> {
            handleIconSlider((int) value, sleepQualities, previewSleepQuality);
            jim.setSleepQuality(SleepQuality.values()[(int) value].getId());
        });
        sliderDreamClarity.addOnChangeListener((slider, value, fromUser) -> {
            handleIconSlider((int) value, dreamClarities, previewDreamClarity);
            jim.setDreamClarity(DreamClarity.values()[(int) value].getId());
        });

        closeEditor.setOnClickListener(e -> new AlertDialog.Builder(getContext(), Tools.getThemeDialog())
                .setTitle("Discard changes")
                .setMessage("Do you really want to discard all changes")
                .setPositiveButton(getResources().getString(R.string.yes), (dialog, which) -> {
                    journalManger.discardEntry(journalEntryId);
                    if(mCloseButtonClicked != null) {
                        mCloseButtonClicked.onEvent();
                    }
                })
                .setNegativeButton(getResources().getString(R.string.no), null)
                .show());

        backToDreamEditor.setOnClickListener(e -> {
            if(mBackButtonListener != null) {
                mBackButtonListener.onEvent();
            }
        });

        doneRatingBtn.setOnClickListener(e -> {
            if(jim.getTitle().equals("") || (jim.getDescription().equals("") && jim.getAudioRecordings().size() == 0)) {
                new AlertDialog.Builder(getContext(), Tools.getThemeDialog())
                        .setTitle("No content")
                        .setMessage("You have to provide a title and a description or audio recording for your dream journal entry!")
                        .setPositiveButton(getResources().getString(R.string.ok), null)
                        .show();
                return;
            }

            compositeDisposable.add(storeEntry().subscribeOn(Schedulers.io()).subscribe((currentEntryId) ->{
                if(mDreamJournalEntrySaved != null) {
                    mDreamJournalEntrySaved.onEvent(currentEntryId);
                }
            }));
        });

        cardRatingsPreview.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                cardRatingsPreview.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int screenTopToCardBottom = cardRatingsPreview.getBottom();
                int marginBelowCard = Tools.dpToPx(getContext(), 20);
                int height = getAppHeight();
                bottomSheetBehavior.setPeekHeight(height - (screenTopToCardBottom + marginBelowCard));
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    ViewCompat.setBackground(bottomSheet, ResourcesCompat.getDrawable(getResources(), R.drawable.bottomsheet_background_line_expanded, getContext().getTheme()));
                    dragHandle.setVisibility(View.INVISIBLE);
                }
                else {
                    ViewCompat.setBackground(bottomSheet, ResourcesCompat.getDrawable(getResources(), R.drawable.bottomsheet_background_line, getContext().getTheme()));
                    dragHandle.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) { }
        });
    }

    private void setAdjustedStatusBarMargin(ConstraintLayout heading) {
        // As the content editor's margin top is automatically adjusted because of the 'fitsSystemWindows' flag
        // we have to adjust the heading's margin top to be at the same height as the content editor's
        // heading height. Those values vary from device to device

        int insetTop = getActivity().getWindow().getDecorView().getRootWindowInsets().getStableInsetTop();
        int statusBarHeight = Tools.getStatusBarHeight(getContext());
        int marginTop = insetTop - statusBarHeight;

        LinearLayout.LayoutParams lParams = (LinearLayout.LayoutParams) heading.getLayoutParams();
        lParams.topMargin = marginTop;
        heading.setLayoutParams(Tools.addLinearLayoutParamsTopStatusbarSpacing(getContext(), lParams));
    }

    private void toggleSpecialDreamType(JournalInMemory jim, ToggleButton button, JournalInMemory.SpecialDreamType specialDreamType) {
        setToggledSpecialDreamType(button, getSpecialDreamTypeIcon(specialDreamType));
        if(jim != null) {
            jim.setSpecialDreamType(specialDreamType, button.isChecked());
        }
    }

    private static int getSpecialDreamTypeIcon(JournalInMemory.SpecialDreamType specialDreamType) {
        @DrawableRes int icon = -1;
        switch (specialDreamType) {
            case NIGHTMARE: icon = R.drawable.rounded_sentiment_stressed_24; break;
            case PARALYSIS: icon = R.drawable.ic_baseline_accessibility_new_24; break;
            case LUCID: icon = R.drawable.rounded_award_star_24; break;
            case RECURRING: icon = R.drawable.ic_round_loop_24; break;
            case FALSE_AWAKENING: icon = R.drawable.rounded_cinematic_blur_24; break;
        }
        return icon;
    }

    private void setToggledSpecialDreamType(ToggleButton button, @DrawableRes int iconId) {
        if(button.isChecked()) {
            setSpecialDreamNoticeIfFirst();
            ImageView icon = generateIcon(iconId);
            toggleButtonIcons.put(button, icon);
            specialDreamIconsContainer.addView(icon);
        }
        else {
            setRegularDreamNoticeIfLast(button);
            ImageView icon = toggleButtonIcons.get(button);
            toggleButtonIcons.remove(button);
            specialDreamIconsContainer.removeView(icon);
        }
    }

    private void setRegularDreamNoticeIfLast(ToggleButton button) {
        if (toggleButtonIcons.size() == 1 && toggleButtonIcons.containsKey(button)) {
            specialDreamIconsContainer.addView(generateIcon(R.drawable.rounded_eco_24));
            specialDreamHeading.setText("Regular dream");
            specialDreamDescription.setText("This is a normal dream without any special types");
        }
    }

    private void setSpecialDreamNoticeIfFirst() {
        if (toggleButtonIcons.size() == 0) {
            specialDreamIconsContainer.removeAllViews();
            specialDreamHeading.setText("Special dream");
            specialDreamDescription.setText("This is a special dream with at least one special type");
        }
    }

    private ImageView generateIcon(@DrawableRes int icon) {
        ImageView img = new ImageView(getContext());
        img.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        img.setImageResource(icon);
        img.setImageTintList(Tools.getAttrColorStateList(R.attr.secondaryTextColor, getContext().getTheme()));
        return img;
    }

    private void handleIconSlider(int value, ImageView[] icons, ImageView previewIcon) {
        if(value < icons.length && previewIcon != null) {
            for (int i = 0; i < icons.length; i++) {
                if(icons[i] != null) {
                    icons[i].setImageTintList(i == value ? colorOnPrimary : tertiaryColor);
                    ViewGroup.LayoutParams lParams = icons[i].getLayoutParams();
                    lParams.height = i == value ? Tools.dpToPx(getContext(), 24) : Tools.dpToPx(getContext(), 16);
                    icons[i].setLayoutParams(lParams);
                    icons[i].invalidate();
                }
            }
            previewIcon.setImageDrawable(icons[value].getDrawable());
        }
    }

    private int getAppHeight() {
        // TODO: check on phones without (navbar / gesture inset) if inset is 0, apparently might have higher value even without visible inset
        int insetBottom = getActivity().getWindow().getDecorView().getRootWindowInsets().getStableInsetBottom();
        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
        int deviceHeight = metrics.heightPixels;
        return deviceHeight - insetBottom;
    }

    private void restoreStoredEntry(JournalInMemory jim) {
        sliderDreamMood.setValue(DreamMoods.getEnum(jim.getDreamMood()).ordinal());
        handleIconSlider((int) sliderDreamMood.getValue(), dreamMoods, previewDreamMood);
        sliderDreamClarity.setValue(DreamClarity.getEnum(jim.getDreamClarity()).ordinal());
        handleIconSlider((int) sliderDreamClarity.getValue(), dreamClarities, previewDreamClarity);
        sliderSleepQuality.setValue(SleepQuality.getEnum(jim.getSleepQuality()).ordinal());
        handleIconSlider((int) sliderSleepQuality.getValue(), sleepQualities, previewSleepQuality);

        toggleNightmare.setChecked(jim.isNightmare());
        toggleSpecialDreamType(null, toggleNightmare, JournalInMemory.SpecialDreamType.NIGHTMARE);
        toggleParalysis.setChecked(jim.isParalysis());
        toggleSpecialDreamType(null, toggleParalysis, JournalInMemory.SpecialDreamType.PARALYSIS);
        toggleLucid.setChecked(jim.isLucid());
        toggleSpecialDreamType(null, toggleLucid, JournalInMemory.SpecialDreamType.LUCID);
        toggleRecurring.setChecked(jim.isRecurring());
        toggleSpecialDreamType(null, toggleRecurring, JournalInMemory.SpecialDreamType.RECURRING);
        toggleFalseAwakening.setChecked(jim.isFalseAwakening());
        toggleSpecialDreamType(null, toggleFalseAwakening, JournalInMemory.SpecialDreamType.FALSE_AWAKENING);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
    }

    @Override
    public void onDestroyView() {
        compositeDisposable.clear();
        super.onDestroyView();
    }

    private Single<Integer> storeEntry() {
        return Single.fromCallable(() -> {
            JournalInMemory jim = journalManger.getEntry(journalEntryId);
            JournalEntry entry = new JournalEntry(jim.getTime().getTimeInMillis(), jim.getTitle(), jim.getDescription(), jim.getSleepQuality(), jim.getDreamClarity(), jim.getDreamMood());

            if(jim.getEntryId() != -1) {
                entry.setEntryId(jim.getEntryId());
                db.getJournalEntryHasTagDao().deleteAll(jim.getEntryId()).blockingSubscribe();
                db.getJournalEntryIsTypeDao().deleteAll(jim.getEntryId()).blockingSubscribe();
            }

            int currentEntryId = db.getJournalEntryDao().insert(entry).blockingGet().intValue();
            List<JournalEntryHasType> journalEntryHasTypes = jim.getDreamTypes(currentEntryId);
            db.getJournalEntryIsTypeDao().insertAll(journalEntryHasTypes).blockingSubscribe();
            List<JournalEntryTag> journalEntryTagsToInsert = jim.getJournalEntryTag();
            db.getJournalEntryTagDao().insertAll(journalEntryTagsToInsert).blockingSubscribe();
            List<JournalEntryTag> journalEntryTags = db.getJournalEntryTagDao().getIdsByDescription(jim.getTags()).blockingGet();
            List<JournalEntryHasTag> journalEntryHasTags = jim.getJournalEntryHasTag(currentEntryId, journalEntryTags);
            db.getJournalEntryHasTagDao().insertAll(journalEntryHasTags).blockingSubscribe();
            List<AudioLocation> audioLocations = AudioLocation.parse(currentEntryId, jim.getAudioRecordings());
            db.getAudioLocationDao().insertAll(audioLocations).blockingSubscribe();
            jim.deleteMarkedAudioRecordings();
            return currentEntryId;
        });
    }

    public interface OnDreamJournalEntrySaved {
        void onEvent(int entryId);
    }

    public void setOnDreamJournalEntrySavedListener(OnDreamJournalEntrySaved eventListener) {
        mDreamJournalEntrySaved = eventListener;
    }

    public void setJournalEntryId(String id) {
        journalEntryId = id;
        journalManger = JournalInMemoryManager.getInstance();
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

    public interface OnCloseButtonClicked {
        void onEvent();
    }

    public void setOnCloseButtonClicked(OnCloseButtonClicked eventListener) {
        mCloseButtonClicked = eventListener;
    }

    private enum SelectedRating {
        DREAM_MOOD,
        SLEEP_QUALITY,
        DREAM_CLARITY,
        DREAM_CHARACTERISTICS
    }
}