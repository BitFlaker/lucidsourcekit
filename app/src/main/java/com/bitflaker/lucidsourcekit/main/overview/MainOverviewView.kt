package com.bitflaker.lucidsourcekit.main.overview

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bitflaker.lucidsourcekit.database.MainDatabase
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.resulttables.DreamJournalEntry
import com.bitflaker.lucidsourcekit.databinding.FragmentMainOverviewBinding
import com.bitflaker.lucidsourcekit.main.alarms.AlarmEditorView
import com.bitflaker.lucidsourcekit.main.alarms.AlarmManagerView
import com.bitflaker.lucidsourcekit.main.alarms.RecyclerViewAdapterAlarms
import com.bitflaker.lucidsourcekit.main.dreamjournal.RecyclerViewAdapterDreamJournal
import com.bitflaker.lucidsourcekit.main.notification.NotificationManagerView
import com.bitflaker.lucidsourcekit.main.questionnaire.QuestionnaireOverviewActivity
import com.bitflaker.lucidsourcekit.utils.Tools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainOverviewView : Fragment() {
    var rememberJournalEntryClickedListener: ((DreamJournalEntry) -> Unit)? = null
    private lateinit var binding: FragmentMainOverviewBinding
    private lateinit var db: MainDatabase
    private lateinit var adapterAlarms: RecyclerViewAdapterAlarms
    private val alarmInteractionLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val createdNew = result.data?.getBooleanExtra("CREATED_NEW_ALARM", false)
            val alarmId = result.data?.getLongExtra("ALARM_ID", -1)
            if (createdNew != null && alarmId != null && !createdNew && alarmId != -1L) {
                lifecycleScope.launch(Dispatchers.IO) {
                    adapterAlarms.reloadModifiedAlarmWithId(alarmId)
                }
            }
        }
    }
    private val alarmManagerLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        lifecycleScope.launch(Dispatchers.IO) {
            val storedAlarms = db.storedAlarmDao.getAllActive()
            requireActivity().runOnUiThread {
                adapterAlarms.setData(storedAlarms)
                setNoActiveAlarmsMessageVisible(storedAlarms.isEmpty())
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentMainOverviewBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = MainDatabase.getInstance(context)
        adapterAlarms = RecyclerViewAdapterAlarms(this, requireActivity(), arrayListOf())
        binding.rcvRememberDream.visibility = View.GONE
        binding.crdNoRememberEntry.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            // Load active alarms
            val storedAlarms = db.storedAlarmDao.getAllActive()
            requireActivity().runOnUiThread {
                adapterAlarms.setData(storedAlarms)
                setNoActiveAlarmsMessageVisible(storedAlarms.isEmpty())
            }

            // Load remember entry
            val entryId = db.journalEntryDao.getRandomEntryId() ?: return@launch
            val entry = db.journalEntryDao.getEntryDataById(entryId.toInt())
            requireActivity().runOnUiThread {
                val adapter = RecyclerViewAdapterDreamJournal(
                    this@MainOverviewView,
                    requireActivity(),
                    null,
                    listOf(entry)
                )
                adapter.isCompactMode = true
                binding.rcvRememberDream.visibility = View.VISIBLE
                binding.rcvRememberDream.adapter = adapter
                binding.rcvRememberDream.setLayoutManager(LinearLayoutManager(context))
                binding.crdNoRememberEntry.visibility = View.GONE
                adapter.onEntryClickListener = rememberJournalEntryClickedListener
            }
        }

        // Setup active alarm list viewer
        adapterAlarms.setSelectionModeEnabled(false)
        adapterAlarms.controlsVisible = false
        adapterAlarms.onEntryClickedListener = { storedAlarm ->
            val editor = Intent(context, AlarmEditorView::class.java)
            editor.putExtra("ALARM_ID", storedAlarm.alarmId)
            alarmInteractionLauncher.launch(editor)
        }
        binding.rcvActiveAlarms.setAdapter(adapterAlarms)
        binding.rcvActiveAlarms.setLayoutManager(LinearLayoutManager(context))
        binding.rcvActiveAlarms.setItemAnimator(null)
        binding.btnManageAlarms.setOnClickListener { alarmManagerLauncher.launch(Intent(context, AlarmManagerView::class.java)) }

        // Questionnaire quick access
        binding.btnQaQuestionnaire.setOnClickListener { startActivity(Intent(context, QuestionnaireOverviewActivity::class.java)) }
        binding.llQaQuestionnaire.setOnClickListener { binding.btnQaQuestionnaire.performClick() }

        // Notification quick access
        binding.btnQaNotifications.setOnClickListener { startActivity(Intent(context, NotificationManagerView::class.java)) }
        binding.llQaNotifications.setOnClickListener { binding.btnQaNotifications.performClick() }

        // More button quick access placeholder
        binding.btnQaMore.setOnClickListener { Tools.showPlaceholderDialog(requireContext()) }
        binding.llQaMore.setOnClickListener { binding.btnQaMore.performClick() }

        // Lockscreen button quick access placeholder
        binding.btnQaLockscreen.setOnClickListener { Tools.showPlaceholderDialog(requireContext()) }
        binding.llQaLockscreen.setOnClickListener { binding.btnQaLockscreen.performClick() }
    }

    private fun setNoActiveAlarmsMessageVisible(visible: Boolean) {
        binding.txtNoAlarmsSet.visibility = if (visible) View.VISIBLE else View.GONE
        binding.rcvActiveAlarms.visibility = if (visible) View.GONE else View.VISIBLE
    }
}