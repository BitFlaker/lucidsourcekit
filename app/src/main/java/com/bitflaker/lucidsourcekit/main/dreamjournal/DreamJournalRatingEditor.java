package com.bitflaker.lucidsourcekit.main.dreamjournal;

import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.resulttables.DreamJournalEntry;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.bitflaker.lucidsourcekit.general.database.values.DreamClarity;
import com.bitflaker.lucidsourcekit.general.database.values.DreamMoods;
import com.bitflaker.lucidsourcekit.general.database.values.DreamTypes;
import com.bitflaker.lucidsourcekit.general.database.values.SleepQuality;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;

import java.util.HashMap;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class DreamJournalRatingEditor extends Fragment {
    private final String[] dreamMoodLabels = new String[] { "Terrible", "Poor", "Okay", "Great", "Outstanding"};
    private final String[] sleepQualityLabels = new String[] { "Terrible", "Poor", "Great", "Outstanding"};
    private final String[] dreamClarityLabels = new String[] { "Very Cloudy", "Cloudy", "Clear", "Crystal Clear"};
    private ImageView[] dreamMoods = new ImageView[5];
    private ImageView[] sleepQualities = new ImageView[4];
    private ImageView[] dreamClarities = new ImageView[4];
    private ToggleButton toggleNightmare, toggleParalysis, toggleLucid, toggleRecurring, toggleFalseAwakening;
    private Slider sliderDreamMood, sliderSleepQuality, sliderDreamClarity;
    private ImageView previewDreamMood, previewDreamClarity, previewSleepQuality;
    private LinearLayout specialDreamIconsContainer;
    private ColorStateList unselectedIconColor;
    private ColorStateList selectedIconColor;
    private ImageView dragHandle;
    private TextView specialDreamHeading, specialDreamDescription;
    private HashMap<ToggleButton, ImageView> toggleButtonIcons;
    private MaterialCardView cardRatingsPreview;
    private BottomSheetBehavior<NestedScrollView> bottomSheetBehavior;
    private CompositeDisposable compositeDisposable;
    private DreamJournalEntry entry;
    private OnCloseButtonClicked mCloseButtonClicked;
    private OnBackButtonClicked mBackButtonListener;
    private OnDoneButtonClicked mDoneButtonListener;

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
        unselectedIconColor = Tools.getAttrColorStateList(R.attr.colorSurfaceContainerHigh, getContext().getTheme());
        selectedIconColor = Tools.getAttrColorStateList(R.attr.colorPrimary, getContext().getTheme());

        MaterialButton backToDreamEditor = getView().findViewById(R.id.btn_dj_back_to_text);
        MaterialButton doneRatingBtn = getView().findViewById(R.id.btn_dj_done_rating);
        MaterialButton closeEditor = getView().findViewById(R.id.btn_dj_close_editor);

        cardRatingsPreview = getView().findViewById(R.id.mcv_dream_rating_preview);
        bottomSheetBehavior = BottomSheetBehavior.from(getView().findViewById(R.id.nsv_dream_rating_bottom_sheet));
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

        restoreStoredEntry();

        toggleNightmare.setOnClickListener(e -> toggleSpecialDreamType((ToggleButton) e, DreamTypes.Nightmare.getId()));
        toggleParalysis.setOnClickListener(e -> toggleSpecialDreamType((ToggleButton) e, DreamTypes.SleepParalysis.getId()));
        toggleLucid.setOnClickListener(e -> toggleSpecialDreamType((ToggleButton) e, DreamTypes.Lucid.getId()));
        toggleRecurring.setOnClickListener(e -> toggleSpecialDreamType((ToggleButton) e, DreamTypes.Recurring.getId()));
        toggleFalseAwakening.setOnClickListener(e -> toggleSpecialDreamType((ToggleButton) e, DreamTypes.FalseAwakening.getId()));

        sliderDreamMood.setLabelFormatter(value -> dreamMoodLabels[(int)value]);
        sliderSleepQuality.setLabelFormatter(value -> sleepQualityLabels[(int)value]);
        sliderDreamClarity.setLabelFormatter(value -> dreamClarityLabels[(int)value]);

        sliderDreamMood.addOnChangeListener((slider, value, fromUser) -> {
            handleIconSlider((int) value, dreamMoods, previewDreamMood);
            entry.journalEntry.moodId = DreamMoods.values()[(int) value].getId();
        });
        sliderSleepQuality.addOnChangeListener((slider, value, fromUser) -> {
            handleIconSlider((int) value, sleepQualities, previewSleepQuality);
            entry.journalEntry.qualityId = SleepQuality.values()[(int) value].getId();
        });
        sliderDreamClarity.addOnChangeListener((slider, value, fromUser) -> {
            handleIconSlider((int) value, dreamClarities, previewDreamClarity);
            entry.journalEntry.clarityId = DreamClarity.values()[(int) value].getId();
        });

        closeEditor.setOnClickListener(e -> new MaterialAlertDialogBuilder(getContext(), R.style.Theme_LucidSourceKit_ThemedDialog)
                .setTitle("Discard changes")
                .setMessage("Do you really want to discard all changes")
                .setPositiveButton(getResources().getString(R.string.yes), (dialog, which) -> {
                    entry.removeAllAddedRecordings();
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
            if(TextUtils.isEmpty(entry.journalEntry.title) || (TextUtils.isEmpty(entry.journalEntry.description) && entry.audioLocations.isEmpty())) {
                new MaterialAlertDialogBuilder(getContext(), R.style.Theme_LucidSourceKit_ThemedDialog)
                        .setTitle("No content")
                        .setMessage("You have to provide a title and a description or audio recording for your dream journal entry!")
                        .setPositiveButton(getResources().getString(R.string.ok), null)
                        .show();
                return;
            }

            if (mDoneButtonListener != null) {
                mDoneButtonListener.onEvent();
            }
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
                dragHandle.setVisibility(newState == BottomSheetBehavior.STATE_EXPANDED ? View.INVISIBLE : View.VISIBLE);
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

    private void toggleSpecialDreamType(ToggleButton button, String dreamType) {
        @DrawableRes int iconId = getSpecialDreamTypeIcon(dreamType);
        if(button.isChecked()) {
            setSpecialDreamNoticeIfFirst();
            ImageView icon = generateIcon(iconId);
            toggleButtonIcons.put(button, icon);
            specialDreamIconsContainer.addView(icon);
            entry.addDreamType(dreamType);
        }
        else {
            setRegularDreamNoticeIfLast(button);
            ImageView icon = toggleButtonIcons.get(button);
            toggleButtonIcons.remove(button);
            specialDreamIconsContainer.removeView(icon);
            entry.removeDreamType(dreamType);
        }
    }

    private static int getSpecialDreamTypeIcon(String dreamType) {
        @DrawableRes int icon = switch (DreamTypes.getEnum(dreamType)) {
            case Nightmare -> R.drawable.rounded_sentiment_stressed_24;
            case SleepParalysis -> R.drawable.ic_baseline_accessibility_new_24;
            case Lucid -> R.drawable.rounded_award_star_24;
            case Recurring -> R.drawable.ic_round_loop_24;
            case FalseAwakening -> R.drawable.rounded_cinematic_blur_24;
            default -> -1;
        };
        return icon;
    }

    private void setRegularDreamNoticeIfLast(ToggleButton button) {
        if (toggleButtonIcons.size() == 1 && toggleButtonIcons.containsKey(button)) {
            specialDreamIconsContainer.addView(generateIcon(R.drawable.rounded_eco_24));
            specialDreamHeading.setText("Regular dream");
            specialDreamDescription.setText("This is a normal dream without any special types");
        }
    }

    private void setSpecialDreamNoticeIfFirst() {
        if (toggleButtonIcons.isEmpty()) {
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
                    icons[i].setImageTintList(i == value ? selectedIconColor : unselectedIconColor);
                    LinearLayout.LayoutParams lParams = (LinearLayout.LayoutParams) icons[i].getLayoutParams();
                    lParams.topMargin = i == value ? Tools.dpToPx(getContext(), 6) : 0;
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

    private void restoreStoredEntry() {
        sliderDreamMood.setValue(DreamMoods.getEnum(entry.journalEntry.moodId).ordinal());
        handleIconSlider((int) sliderDreamMood.getValue(), dreamMoods, previewDreamMood);
        sliderDreamClarity.setValue(DreamClarity.getEnum(entry.journalEntry.clarityId).ordinal());
        handleIconSlider((int) sliderDreamClarity.getValue(), dreamClarities, previewDreamClarity);
        sliderSleepQuality.setValue(SleepQuality.getEnum(entry.journalEntry.qualityId).ordinal());
        handleIconSlider((int) sliderSleepQuality.getValue(), sleepQualities, previewSleepQuality);

        toggleNightmare.setChecked(entry.hasSpecialType(DreamTypes.Nightmare.getId()));
        toggleSpecialDreamType(toggleNightmare, DreamTypes.Nightmare.getId());
        toggleParalysis.setChecked(entry.hasSpecialType(DreamTypes.SleepParalysis.getId()));
        toggleSpecialDreamType(toggleParalysis, DreamTypes.SleepParalysis.getId());
        toggleLucid.setChecked(entry.hasSpecialType(DreamTypes.Lucid.getId()));
        toggleSpecialDreamType(toggleLucid, DreamTypes.Lucid.getId());
        toggleRecurring.setChecked(entry.hasSpecialType(DreamTypes.Recurring.getId()));
        toggleSpecialDreamType(toggleRecurring, DreamTypes.Recurring.getId());
        toggleFalseAwakening.setChecked(entry.hasSpecialType(DreamTypes.FalseAwakening.getId()));
        toggleSpecialDreamType(toggleFalseAwakening, DreamTypes.FalseAwakening.getId());
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

    public interface OnDreamJournalEntrySaved {
        void onEvent(int entryId);
    }

    public void setJournalEntryId(DreamJournalEntry entry) {
        this.entry = entry;
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
}