package com.bitflaker.lucidsourcekit.main.binauralbeats

import android.icu.util.Calendar
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.data.Brainwaves
import com.bitflaker.lucidsourcekit.data.records.BinauralBeat
import com.bitflaker.lucidsourcekit.databinding.FragmentMainBinauralBeatsBinding
import com.bitflaker.lucidsourcekit.databinding.SheetBinauralAutoStopBinding
import com.bitflaker.lucidsourcekit.databinding.SheetBinauralBeatsBinding
import com.bitflaker.lucidsourcekit.utils.Tools
import com.bitflaker.lucidsourcekit.utils.attrColor
import com.bitflaker.lucidsourcekit.utils.dpToPx
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale

class BinauralBeatsView : Fragment() {
    private lateinit var binding: FragmentMainBinauralBeatsBinding
    private val autoStopHandler = Handler(Looper.getMainLooper())
    private var binauralBeatsPlayer: BinauralBeatsPlayer? = null
    private var repeatBeat = false
    private var playingFinished = false
    private var isAutoStopTimerRunning = false
    private var autoStopInterval = -1

//    private val noises = arrayListOf(
//        BackgroundNoise("Flowing water", R.drawable.ic_baseline_water_24, 25f),
//        BackgroundNoise("Explosions", R.drawable.ic_baseline_brightness_7_24, 0f),
//        BackgroundNoise("Wind", R.drawable.ic_baseline_air_24, 75f),
//        BackgroundNoise("Alarm", R.drawable.ic_baseline_access_alarm_24, 10f),
//        BackgroundNoise("White noise", R.drawable.ic_baseline_waves_24, 100f)
//    )

    private val stopCurrentlyPlayingTrack = Runnable {
        val context = requireContext()
        binding.btnPlayTrack.setIcon(ResourcesCompat.getDrawable(context.resources, R.drawable.ic_baseline_play_arrow_24, context.theme))
        binding.btnAutoStop.setImageDrawable(ResourcesCompat.getDrawable(context.resources, R.drawable.ic_outline_timer_24, context.theme))
        binauralBeatsPlayer?.pause()
        autoStopInterval = -1
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentMainBinauralBeatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()

        // Set handler to show binaural beats selector
        binding.btnDisplayAllBeats.setOnClickListener {
            showTrackSelector()
        }

        // Add handler to show background noise configurator
        binding.btnAddBackgroundNoise.setOnClickListener {
            Tools.showPlaceholderDialog(context)
        }

        // Set handler to toggle track looping
        binding.btnLoopTrack.setOnClickListener {
            repeatBeat = !repeatBeat
            if (repeatBeat) {
                binding.btnLoopTrack.setImageDrawable(ResourcesCompat.getDrawable(context.resources, R.drawable.ic_baseline_repeat_on_24, context.theme))
                Toast.makeText(context, "Repeat is on", Toast.LENGTH_SHORT).show()
            } else {
                binding.btnLoopTrack.setImageDrawable(ResourcesCompat.getDrawable(context.resources, R.drawable.ic_baseline_repeat_24, context.theme))
                Toast.makeText(context, "Repeat is off", Toast.LENGTH_SHORT).show()
            }
        }

        // Set auto stop handler
        binding.btnAutoStop.setOnClickListener {
            if (autoStopInterval != -1) {
                requestDisableAutoStopConfirmation()
            }
            else {
                showAutoStopConfigurator()
            }
        }

        // Set handler for play / pause
        binding.btnPlayTrack.setOnClickListener {
            handlePlayButtonClick()
        }

        // Configure line chart progress settings
        binding.lgBinauralTimeProgress.setBottomLineSpacing(10f)
        binding.lgBinauralTimeProgress.setDrawGradient(true)
        binding.lgBinauralTimeProgress.setGradientOpacity(0.5f)
        binding.lgBinauralTimeProgress.setDrawProgressIndicator(false)

        // Configure frequency legend text and color
        val labels = arrayOf("β", "α", "θ", "δ")
        binding.tlBinauralLegend.setData(
            labels,
            Brainwaves.stageColors,
            context.attrColor(R.attr.secondaryTextColor),
            context.attrColor(R.attr.tertiaryTextColor),
            18
        )
    }

    private fun showTrackSelector() {
        val context = requireContext()
        val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogStyle)
        val beatsBinding = SheetBinauralBeatsBinding.inflate(getLayoutInflater())
        bottomSheetDialog.setContentView(beatsBinding.root)

        val adapter = RecyclerViewAdapterBinauralBeatsSelector(context, BinauralBeatsCollection.binauralBeats)
        adapter.onEntryClickedListener = { binauralBeat, _ ->
            bottomSheetDialog.dismiss()
            selectBinauralBeat(binauralBeat)
        }
        beatsBinding.rcvListBinauralBeats.setAdapter(adapter)
        beatsBinding.rcvListBinauralBeats.setLayoutManager(LinearLayoutManager(context))
        bottomSheetDialog.show()
    }

    private fun handlePlayButtonClick() {
        val context = requireContext()
        if (binauralBeatsPlayer == null) {
            Toast.makeText(context, "No binaural beat selected", Toast.LENGTH_SHORT).show()
            return
        }

        // Restart track in case the track finished
        if (playingFinished) {
            binding.txtBinauralBeatsTimeline.text = getTimeStringFromSeconds(0)
            binding.lgBinauralTimeProgress.resetProgress()
            playingFinished = false
        }

        // Start playing selected binaural beat and autostop timer (if configured while not playing beat)
        if (binauralBeatsPlayer?.isPlaying == false) {
            binding.btnPlayTrack.setIcon(ResourcesCompat.getDrawable(context.resources, R.drawable.ic_baseline_pause_24, context.theme))
            binauralBeatsPlayer?.play()
            if (autoStopInterval != -1 && !isAutoStopTimerRunning) {
                startAutoStopTimeNow()
            }
        } else {
            binding.btnPlayTrack.setIcon(ResourcesCompat.getDrawable(context.resources, R.drawable.ic_baseline_play_arrow_24, context.theme))
            binauralBeatsPlayer?.pause()
        }
    }

    private fun showAutoStopConfigurator() {
        val context = requireContext()
        val bottomSheetDialog = BottomSheetDialog(context, R.style.BottomSheetDialogStyle)
        val autoStopBinding = SheetBinauralAutoStopBinding.inflate(layoutInflater)
        bottomSheetDialog.setContentView(autoStopBinding.root)

        // Set hour field constraints
        autoStopBinding.npHoursAutoStop.setMinValue(0)
        autoStopBinding.npHoursAutoStop.setMaxValue(23)
        autoStopBinding.npHoursAutoStop.value = 0

        // Set minute field constraints
        autoStopBinding.npMinutesAutoStop.setMinValue(0)
        autoStopBinding.npMinutesAutoStop.setMaxValue(59)
        autoStopBinding.npMinutesAutoStop.value = 0

        // Set second field constraints
        autoStopBinding.npSecondsAutoStop.setMinValue(0)
        autoStopBinding.npSecondsAutoStop.setMaxValue(59)
        autoStopBinding.npSecondsAutoStop.value = 0

        // Set handler to set auto stop timer (and start if currently playing track)
        autoStopBinding.btnApplyAutoStop.setOnClickListener {
            isAutoStopTimerRunning = false
            autoStopInterval = (autoStopBinding.npHoursAutoStop.value * 3600 + autoStopBinding.npMinutesAutoStop.value * 60 + autoStopBinding.npSecondsAutoStop.value) * 1000
            if (binauralBeatsPlayer?.isPlaying == true) {
                startAutoStopTimeNow()
            }
            binding.btnAutoStop.setImageDrawable(ResourcesCompat.getDrawable(context.resources, R.drawable.ic_outline_timer_off_24, context.theme))
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    private fun requestDisableAutoStopConfirmation() {
        val context = requireContext()
        MaterialAlertDialogBuilder(context, R.style.Theme_LucidSourceKit_ThemedDialog)
            .setTitle("Disable Auto-Stop")
            .setMessage("Do you really want to disable Auto-Stop?")
            .setPositiveButton(resources.getString(R.string.yes)) { _, _ ->
                autoStopHandler.removeCallbacks(stopCurrentlyPlayingTrack)
                binding.btnAutoStop.setImageDrawable(ResourcesCompat.getDrawable(context.resources, R.drawable.ic_outline_timer_24, context.theme))
                autoStopInterval = -1
            }
            .setNegativeButton(resources.getString(R.string.no), null)
            .show()
    }

    private fun selectBinauralBeat(binauralBeat: BinauralBeat) {
        playingFinished = false
        val context = requireContext()

        // Set values for selected track and update from `no selection` view
        val lParams = (binding.llBbpCarrierFreqHeading.layoutParams as LinearLayout.LayoutParams).apply {
            topMargin = 24.dpToPx
        }
        binding.llBbpCarrierFreqHeading.setLayoutParams(lParams)
        binding.llBbpTimeContainer.visibility = View.VISIBLE
        binding.lgBinauralTimeProgress.visibility = View.VISIBLE
        binding.txtBbpCarrierFreqHeading.visibility = View.VISIBLE
        binding.txtBinauralBeatsTotalTime.text = String.format(" / %s", getTimeStringFromSeconds(binauralBeat.frequencyList.duration.toInt()))
        binding.txtBinauralBeatsTimeline.text = getTimeStringFromSeconds(0)
        binding.txtCurrentBinauralFrequency.text = "0.00"
        binding.txtCarrierFrequency.text = String.format(Locale.ENGLISH, "%.0f Hz", binauralBeat.baseFrequency)
        binding.txtCarrierFrequency.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        binding.txtCarrierFrequency.setTextColor(context.attrColor(R.attr.primaryTextColor))
        binding.txtCarrierFrequency.setCompoundDrawables(null, null, null, null)
        binding.txtBbpCarrierFreqHeading.setTextColor(context.attrColor(R.attr.secondaryTextColor))
        binding.btnPlayTrack.setIcon(ResourcesCompat.getDrawable(context.resources, R.drawable.ic_baseline_play_arrow_24, context.theme))

//        binding.txtCurrTrackName.setVisibility(View.VISIBLE);
//        binding.txtCurrTrackName.setText(binauralBeat.title());
//        binding.txtCurrTrackDescription.setText(binauralBeat.description());

        // Stop data from previous track and reset to defaults
        setDataForProgress(binauralBeat, 0)
        binauralBeatsPlayer?.stop()
        binding.lgBinauralTimeProgress.resetProgress()

        // Set the data for the current track
        binding.lgBinauralTimeProgress.setData(
            binauralBeat.frequencyList,
            32f,
            4f,
            0f,
            false,
            Brainwaves.stageColors,
            Brainwaves.stageFrequencyCenters
        )

        // Create new player
        val player = BinauralBeatsPlayer(binauralBeat)

        // Set handler for progress to update progress and data (e.g. frequency, ...)
        player.onProgressListener = { currentBinauralBeat, progress ->
            setDataForProgress(currentBinauralBeat, progress)
            activity?.runOnUiThread {
                binding.lgBinauralTimeProgress.updateProgress(progress.toDouble())
            } ?: player.stop()
        }

        // Set handler for a finished track (stop / repeat track)
        player.onFinishedListener = { currentBinauralBeat ->
            playingFinished = true
            setEndValues(currentBinauralBeat)
            if (repeatBeat) {
                playingFinished = false
                binding.lgBinauralTimeProgress.resetProgress()
                player.play()
            } else {
                binding.btnPlayTrack.setIcon(ResourcesCompat.getDrawable(context.resources, R.drawable.ic_baseline_play_arrow_24, context.theme))
            }
        }
        binauralBeatsPlayer = player
    }

    private fun startAutoStopTimeNow() {
        val calendar = Calendar.getInstance()
        autoStopHandler.postAtTime(stopCurrentlyPlayingTrack, calendar.timeInMillis + autoStopInterval)
        autoStopHandler.postDelayed(stopCurrentlyPlayingTrack, autoStopInterval.toLong())
        isAutoStopTimerRunning = true
    }

    private fun setEndValues(currentBinauralBeat: BinauralBeat) {
        activity?.runOnUiThread {
            val finishProgress = currentBinauralBeat.frequencyList.duration.toInt()
            binding.txtBinauralBeatsTimeline.text = getTimeStringFromSeconds(finishProgress)
            binding.txtCurrentBinauralFrequency.text = String.format(Locale.ENGLISH, "%.2f", currentBinauralBeat.frequencyList.getFrequencyAtDuration(finishProgress.toDouble()))
            // TODO: the progress updating is a bit a workaround and should be better
            binding.lgBinauralTimeProgress.updateProgress(binding.lgBinauralTimeProgress.durationProgress.toDouble())
        }
    }

    private fun setDataForProgress(binauralBeat: BinauralBeat, progress: Int) {
        val currFreq = binauralBeat.frequencyList.getFrequencyAtDuration(progress.toDouble())
        val stageIndex = Brainwaves.getStageIndex(currFreq)
        val greekLetter = Brainwaves.stageFrequencyGreekLetters[stageIndex]
        val greekLetterName = Brainwaves.stageFrequencyGreekLetterNames[stageIndex]
        activity?.runOnUiThread {
            binding.txtCurrentFrequencyGreekLetter.text = greekLetter
            binding.txtCurrentFrequencyName.text = greekLetterName
            binding.tlBinauralLegend.setCurrentSelectedIndex(stageIndex)
            binding.txtCurrentBinauralFrequency.text = String.format(Locale.ENGLISH, "%.2f", currFreq)
            binding.txtBinauralBeatsTimeline.text = getTimeStringFromSeconds(progress)
        } ?: binauralBeatsPlayer!!.stop()
    }

    private fun getTimeStringFromSeconds(totalSeconds: Int): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        val textPrefix = if (hours > 0) String.format(Locale.getDefault(), "%02d:", hours) else ""
        return textPrefix + String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}