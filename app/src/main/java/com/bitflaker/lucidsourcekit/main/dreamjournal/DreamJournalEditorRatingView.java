package com.bitflaker.lucidsourcekit.main.dreamjournal;

import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.data.enums.journalratings.DreamClarity;
import com.bitflaker.lucidsourcekit.data.enums.journalratings.DreamMoods;
import com.bitflaker.lucidsourcekit.data.enums.journalratings.DreamTypes;
import com.bitflaker.lucidsourcekit.data.enums.journalratings.SleepQuality;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.resulttables.DreamJournalEntry;
import com.bitflaker.lucidsourcekit.databinding.FragmentJournalEditorRatingBinding;
import com.bitflaker.lucidsourcekit.utils.Tools;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class DreamJournalEditorRatingView extends Fragment {
    private final String[] dreamMoodLabels = new String[] { "Terrible", "Poor", "Okay", "Great", "Outstanding"};
    private final String[] sleepQualityLabels = new String[] { "Terrible", "Poor", "Great", "Outstanding"};
    private final String[] dreamClarityLabels = new String[] { "Very Cloudy", "Cloudy", "Clear", "Crystal Clear"};
    private ImageView[] dreamMoods = new ImageView[5];
    private ImageView[] sleepQualities = new ImageView[4];
    private ImageView[] dreamClarities = new ImageView[4];
    private ColorStateList unselectedIconColor;
    private ColorStateList selectedIconColor;
    private CompositeDisposable compositeDisposable;
    private DreamJournalEntry entry;
    private OnBackButtonClicked mBackButtonListener;
    private OnDoneButtonClicked mDoneButtonListener;
    private FragmentJournalEditorRatingBinding binding;
    private OnCloseButtonClicked mCloseButtonClicked;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentJournalEditorRatingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        compositeDisposable = new CompositeDisposable();
        unselectedIconColor = Tools.getAttrColorStateList(R.attr.colorSurfaceContainerHigh, getContext().getTheme());
        selectedIconColor = Tools.getAttrColorStateList(R.attr.colorOnSurface, getContext().getTheme());

        dreamMoods = new ImageView[] {
                binding.imgVeryDissatisfied,
                binding.imgDissatisfied,
                binding.imgNeutralSatisfied,
                binding.imgSatisfied,
                binding.imgVerySatisfied
        };
        sleepQualities = new ImageView[] {
                binding.imgVeryBadQuality,
                binding.imgBadQuality,
                binding.imgGoodQuality,
                binding.imgVeryGoodQuality
        };
        dreamClarities = new ImageView[] {
                binding.imgVeryUnclear,
                binding.imgUnclear,
                binding.imgClear,
                binding.imgVeryClear
        };
        restoreStoredEntry();

        binding.tglDjNightmare.setOnClickListener(e -> toggleSpecialDreamType((ToggleButton) e, DreamTypes.Nightmare.getId()));
        binding.tglDjParalysis.setOnClickListener(e -> toggleSpecialDreamType((ToggleButton) e, DreamTypes.SleepParalysis.getId()));
        binding.tglDjLucid.setOnClickListener(e -> toggleSpecialDreamType((ToggleButton) e, DreamTypes.Lucid.getId()));
        binding.tglDjRecurring.setOnClickListener(e -> toggleSpecialDreamType((ToggleButton) e, DreamTypes.Recurring.getId()));
        binding.tglDjFalseAwakening.setOnClickListener(e -> toggleSpecialDreamType((ToggleButton) e, DreamTypes.FalseAwakening.getId()));

        binding.sldDjDreamMood.setLabelFormatter(value -> dreamMoodLabels[(int) value]);
        binding.sldDjSleepQuality.setLabelFormatter(value -> sleepQualityLabels[(int) value]);
        binding.sldDjDreamClarity.setLabelFormatter(value -> dreamClarityLabels[(int) value]);

        binding.sldDjDreamMood.addOnChangeListener((slider, value, fromUser) -> {
            handleIconSlider((int) value, dreamMoods);
            entry.journalEntry.moodId = DreamMoods.values()[(int) value].getId();
        });
        binding.sldDjSleepQuality.addOnChangeListener((slider, value, fromUser) -> {
            handleIconSlider((int) value, sleepQualities);
            entry.journalEntry.qualityId = SleepQuality.values()[(int) value].getId();
        });
        binding.sldDjDreamClarity.addOnChangeListener((slider, value, fromUser) -> {
            handleIconSlider((int) value, dreamClarities);
            entry.journalEntry.clarityId = DreamClarity.values()[(int) value].getId();
        });

        binding.btnDjCloseEditor.setOnClickListener(e -> {
            if (mBackButtonListener != null) {
                mBackButtonListener.onEvent();
            }
        });

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
    }

    private void toggleSpecialDreamType(ToggleButton button, String dreamType) {
        if (button.isChecked()) {
            entry.addDreamType(dreamType);
        }
        else {
            entry.removeDreamType(dreamType);
        }
    }

    private void handleIconSlider(int value, ImageView[] icons) {
        if(value < icons.length) {
            for (int i = 0; i < icons.length; i++) {
                if(icons[i] != null) {
                    icons[i].setImageTintList(i == value ? selectedIconColor : unselectedIconColor);
                    LinearLayout.LayoutParams lParams = (LinearLayout.LayoutParams) icons[i].getLayoutParams();
                    lParams.topMargin = i == value ? Tools.dpToPx(getContext(), 4) : 0;
                    lParams.height = i == value ? Tools.dpToPx(getContext(), 20) : Tools.dpToPx(getContext(), 16);
                    icons[i].setLayoutParams(lParams);
                    icons[i].invalidate();
                }
            }
        }
    }

    private void restoreStoredEntry() {
        binding.sldDjDreamMood.setValue(DreamMoods.getEnum(entry.journalEntry.moodId).ordinal());
        handleIconSlider((int) binding.sldDjDreamMood.getValue(), dreamMoods);
        binding.sldDjDreamClarity.setValue(DreamClarity.getEnum(entry.journalEntry.clarityId).ordinal());
        handleIconSlider((int) binding.sldDjDreamClarity.getValue(), dreamClarities);
        binding.sldDjSleepQuality.setValue(SleepQuality.getEnum(entry.journalEntry.qualityId).ordinal());
        handleIconSlider((int) binding.sldDjSleepQuality.getValue(), sleepQualities);

        binding.tglDjNightmare.setChecked(entry.hasSpecialType(DreamTypes.Nightmare.getId()));
        toggleSpecialDreamType(binding.tglDjNightmare, DreamTypes.Nightmare.getId());
        binding.tglDjParalysis.setChecked(entry.hasSpecialType(DreamTypes.SleepParalysis.getId()));
        toggleSpecialDreamType(binding.tglDjParalysis, DreamTypes.SleepParalysis.getId());
        binding.tglDjLucid.setChecked(entry.hasSpecialType(DreamTypes.Lucid.getId()));
        toggleSpecialDreamType(binding.tglDjLucid, DreamTypes.Lucid.getId());
        binding.tglDjRecurring.setChecked(entry.hasSpecialType(DreamTypes.Recurring.getId()));
        toggleSpecialDreamType(binding.tglDjRecurring, DreamTypes.Recurring.getId());
        binding.tglDjFalseAwakening.setChecked(entry.hasSpecialType(DreamTypes.FalseAwakening.getId()));
        toggleSpecialDreamType(binding.tglDjFalseAwakening, DreamTypes.FalseAwakening.getId());
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

    public void updatePreview() {
        String title = entry.journalEntry.title;
        String description = entry.journalEntry.description;
        boolean emptyTitle = title == null || title.isEmpty();
        boolean emptyDescription = description == null || description.isEmpty();

        binding.txtJournalTitle.setTextColor(Tools.getAttrColor(emptyTitle ? R.attr.tertiaryTextColor : R.attr.primaryTextColor, getContext().getTheme()));
        binding.txtJournalDescription.setTextColor(Tools.getAttrColor(emptyDescription ? R.attr.tertiaryTextColor : R.attr.secondaryTextColor, getContext().getTheme()));

        binding.txtJournalTitle.setText(emptyTitle ? "No title provided" : title);
        binding.txtJournalDescription.setText(emptyDescription ? "No description provided. Try to write the dream down to better memorize them." : description);

        binding.llTagsHolder.removeAllViews();
        RecyclerViewAdapterDreamJournal.MainViewHolder.setTagList(binding.llTagsHolder, 0, 0, entry.getStringTags(), getActivity());
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