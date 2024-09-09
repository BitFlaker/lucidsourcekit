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
import android.widget.ToggleButton;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.data.enums.journalratings.DreamClarity;
import com.bitflaker.lucidsourcekit.data.enums.journalratings.DreamMoods;
import com.bitflaker.lucidsourcekit.data.enums.journalratings.DreamTypes;
import com.bitflaker.lucidsourcekit.data.enums.journalratings.SleepQuality;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.resulttables.DreamJournalEntry;
import com.bitflaker.lucidsourcekit.databinding.FragmentJournalEditorRatingBinding;
import com.bitflaker.lucidsourcekit.databinding.SheetJournalRatingPersistentBinding;
import com.bitflaker.lucidsourcekit.utils.Tools;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.HashMap;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class DreamJournalEditorRatingView extends Fragment {
    private final String[] dreamMoodLabels = new String[] { "Terrible", "Poor", "Okay", "Great", "Outstanding"};
    private final String[] sleepQualityLabels = new String[] { "Terrible", "Poor", "Great", "Outstanding"};
    private final String[] dreamClarityLabels = new String[] { "Very Cloudy", "Cloudy", "Clear", "Crystal Clear"};
    private ImageView[] dreamMoods = new ImageView[5];
    private ImageView[] sleepQualities = new ImageView[4];
    private ImageView[] dreamClarities = new ImageView[4];
    private HashMap<ToggleButton, ImageView> toggleButtonIcons;
    private ColorStateList unselectedIconColor;
    private ColorStateList selectedIconColor;
    private BottomSheetBehavior<NestedScrollView> bottomSheetBehavior;
    private CompositeDisposable compositeDisposable;
    private DreamJournalEntry entry;
    private OnCloseButtonClicked mCloseButtonClicked;
    private OnBackButtonClicked mBackButtonListener;
    private OnDoneButtonClicked mDoneButtonListener;
    private FragmentJournalEditorRatingBinding binding;
    private SheetJournalRatingPersistentBinding sheetBinding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentJournalEditorRatingBinding.inflate(inflater, container, false);
        sheetBinding = binding.sheetRating;
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setAdjustedStatusBarMargin(binding.clRatingHeading);

        compositeDisposable = new CompositeDisposable();
        toggleButtonIcons = new HashMap<>();
        unselectedIconColor = Tools.getAttrColorStateList(R.attr.colorSurfaceContainerHigh, getContext().getTheme());
        selectedIconColor = Tools.getAttrColorStateList(R.attr.colorPrimary, getContext().getTheme());

        bottomSheetBehavior = BottomSheetBehavior.from(sheetBinding.nsvDreamRatingBottomSheet);

        dreamMoods = new ImageView[] {
                sheetBinding.imgVeryDissatisfied,
                sheetBinding.imgDissatisfied,
                sheetBinding.imgNeutralSatisfied,
                sheetBinding.imgSatisfied,
                sheetBinding.imgVerySatisfied
        };
        sleepQualities = new ImageView[] {
                sheetBinding.imgVeryBadQuality,
                sheetBinding.imgBadQuality,
                sheetBinding.imgGoodQuality,
                sheetBinding.imgVeryGoodQuality
        };
        dreamClarities = new ImageView[] {
                sheetBinding.imgVeryUnclear,
                sheetBinding.imgUnclear,
                sheetBinding.imgClear,
                sheetBinding.imgVeryClear
        };
        restoreStoredEntry();

        sheetBinding.tglDjNightmare.setOnClickListener(e -> toggleSpecialDreamType((ToggleButton) e, DreamTypes.Nightmare.getId()));
        sheetBinding.tglDjParalysis.setOnClickListener(e -> toggleSpecialDreamType((ToggleButton) e, DreamTypes.SleepParalysis.getId()));
        sheetBinding.tglDjLucid.setOnClickListener(e -> toggleSpecialDreamType((ToggleButton) e, DreamTypes.Lucid.getId()));
        sheetBinding.tglDjRecurring.setOnClickListener(e -> toggleSpecialDreamType((ToggleButton) e, DreamTypes.Recurring.getId()));
        sheetBinding.tglDjFalseAwakening.setOnClickListener(e -> toggleSpecialDreamType((ToggleButton) e, DreamTypes.FalseAwakening.getId()));

        sheetBinding.sldDjDreamMood.setLabelFormatter(value -> dreamMoodLabels[(int)value]);
        sheetBinding.sldDjSleepQuality.setLabelFormatter(value -> sleepQualityLabels[(int)value]);
        sheetBinding.sldDjDreamClarity.setLabelFormatter(value -> dreamClarityLabels[(int)value]);

        sheetBinding.sldDjDreamMood.addOnChangeListener((slider, value, fromUser) -> {
            handleIconSlider((int) value, dreamMoods, binding.imgPreviewDreamMood);
            entry.journalEntry.moodId = DreamMoods.values()[(int) value].getId();
        });
        sheetBinding.sldDjSleepQuality.addOnChangeListener((slider, value, fromUser) -> {
            handleIconSlider((int) value, sleepQualities, binding.imgPreviewSleepQuality);
            entry.journalEntry.qualityId = SleepQuality.values()[(int) value].getId();
        });
        sheetBinding.sldDjDreamClarity.addOnChangeListener((slider, value, fromUser) -> {
            handleIconSlider((int) value, dreamClarities, binding.imgPreviewDreamClarity);
            entry.journalEntry.clarityId = DreamClarity.values()[(int) value].getId();
        });

        binding.btnDjCloseEditor.setOnClickListener(e -> new MaterialAlertDialogBuilder(getContext(), R.style.Theme_LucidSourceKit_ThemedDialog)
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

        binding.btnDjBackToText.setOnClickListener(e -> {
            if(mBackButtonListener != null) {
                mBackButtonListener.onEvent();
            }
        });

        binding.btnDjDoneRating.setOnClickListener(e -> {
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

        binding.mcvDreamRatingPreview.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                binding.mcvDreamRatingPreview.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int screenTopToCardBottom = binding.mcvDreamRatingPreview.getBottom();
                int marginBelowCard = Tools.dpToPx(getContext(), 20);
                int height = getAppHeight();
                bottomSheetBehavior.setPeekHeight(height - (screenTopToCardBottom + marginBelowCard));
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                sheetBinding.imgDragHandle.setVisibility(newState == BottomSheetBehavior.STATE_EXPANDED ? View.INVISIBLE : View.VISIBLE);
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
            binding.llSpecialDreamIcons.addView(icon);
            entry.addDreamType(dreamType);
        }
        else {
            setRegularDreamNoticeIfLast(button);
            ImageView icon = toggleButtonIcons.get(button);
            toggleButtonIcons.remove(button);
            binding.llSpecialDreamIcons.removeView(icon);
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
            binding.llSpecialDreamIcons.addView(generateIcon(R.drawable.rounded_eco_24));
            binding.txtSpecialDreamHeading.setText("Regular dream");
            binding.txtSpecialDreamDescription.setText("This is a normal dream without any special types");
        }
    }

    private void setSpecialDreamNoticeIfFirst() {
        if (toggleButtonIcons.isEmpty()) {
            binding.llSpecialDreamIcons.removeAllViews();
            binding.txtSpecialDreamHeading.setText("Special dream");
            binding.txtSpecialDreamDescription.setText("This is a special dream with at least one special type");
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
        sheetBinding.sldDjDreamMood.setValue(DreamMoods.getEnum(entry.journalEntry.moodId).ordinal());
        handleIconSlider((int) sheetBinding.sldDjDreamMood.getValue(), dreamMoods, binding.imgPreviewDreamMood);
        sheetBinding.sldDjDreamClarity.setValue(DreamClarity.getEnum(entry.journalEntry.clarityId).ordinal());
        handleIconSlider((int) sheetBinding.sldDjDreamClarity.getValue(), dreamClarities, binding.imgPreviewDreamClarity);
        sheetBinding.sldDjSleepQuality.setValue(SleepQuality.getEnum(entry.journalEntry.qualityId).ordinal());
        handleIconSlider((int) sheetBinding.sldDjSleepQuality.getValue(), sleepQualities, binding.imgPreviewSleepQuality);

        sheetBinding.tglDjNightmare.setChecked(entry.hasSpecialType(DreamTypes.Nightmare.getId()));
        toggleSpecialDreamType(sheetBinding.tglDjNightmare, DreamTypes.Nightmare.getId());
        sheetBinding.tglDjParalysis.setChecked(entry.hasSpecialType(DreamTypes.SleepParalysis.getId()));
        toggleSpecialDreamType(sheetBinding.tglDjParalysis, DreamTypes.SleepParalysis.getId());
        sheetBinding.tglDjLucid.setChecked(entry.hasSpecialType(DreamTypes.Lucid.getId()));
        toggleSpecialDreamType(sheetBinding.tglDjLucid, DreamTypes.Lucid.getId());
        sheetBinding.tglDjRecurring.setChecked(entry.hasSpecialType(DreamTypes.Recurring.getId()));
        toggleSpecialDreamType(sheetBinding.tglDjRecurring, DreamTypes.Recurring.getId());
        sheetBinding.tglDjFalseAwakening.setChecked(entry.hasSpecialType(DreamTypes.FalseAwakening.getId()));
        toggleSpecialDreamType(sheetBinding.tglDjFalseAwakening, DreamTypes.FalseAwakening.getId());
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