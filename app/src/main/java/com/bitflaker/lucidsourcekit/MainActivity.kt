package com.bitflaker.lucidsourcekit

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bitflaker.lucidsourcekit.data.datastore.DataStoreKeys
import com.bitflaker.lucidsourcekit.data.datastore.DataStoreMigrator.migrateSetupFinishedToDataStore
import com.bitflaker.lucidsourcekit.data.datastore.DataStoreMigrator.migrateSharedPreferencesToDataStore
import com.bitflaker.lucidsourcekit.data.datastore.getSetting
import com.bitflaker.lucidsourcekit.data.datastore.updateSetting
import com.bitflaker.lucidsourcekit.database.MainDatabase
import com.bitflaker.lucidsourcekit.database.alarms.entities.AlarmToneTypes
import com.bitflaker.lucidsourcekit.database.alarms.updated.entities.ActiveAlarm
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.DreamClarity
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.DreamMood
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.DreamType
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.SleepQuality
import com.bitflaker.lucidsourcekit.database.goals.entities.Shuffle
import com.bitflaker.lucidsourcekit.database.goals.entities.ShuffleHasGoal
import com.bitflaker.lucidsourcekit.database.goals.entities.defaults.DefaultGoals
import com.bitflaker.lucidsourcekit.database.notifications.entities.NotificationCategory
import com.bitflaker.lucidsourcekit.database.questionnaire.entities.QuestionType.Companion.defaults
import com.bitflaker.lucidsourcekit.databinding.ActivityMainBinding
import com.bitflaker.lucidsourcekit.databinding.ActivityQuestionnaireBinding
import com.bitflaker.lucidsourcekit.main.MainViewer
import com.bitflaker.lucidsourcekit.main.alarms.AlarmHandler
import com.bitflaker.lucidsourcekit.main.notification.visual.KeypadAdapter
import com.bitflaker.lucidsourcekit.main.notification.visual.KeypadButtonModel
import com.bitflaker.lucidsourcekit.setup.SetupViewer
import com.bitflaker.lucidsourcekit.utils.Crypt
import com.bitflaker.lucidsourcekit.utils.Tools
import com.bitflaker.lucidsourcekit.utils.loadLanguage
import com.bitflaker.lucidsourcekit.utils.resolveDrawable
import com.bitflaker.lucidsourcekit.utils.showToastLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var storedHash: String
    private lateinit var storedSalt: ByteArray
    private val enteredPin = StringBuilder()
    private lateinit var db: MainDatabase
    companion object {
        var test: Int = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        test += 1
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        db = MainDatabase.getInstance(this)

        // Create internal data structure if it does not exist
        ensureValidDataStructure()

        // TODO: remove enforce dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        when (currentNightMode) {
            Configuration.UI_MODE_NIGHT_NO -> { }
            Configuration.UI_MODE_NIGHT_YES -> { }
        }
//        setTheme(Tools.getTheme());

        lifecycleScope.launch(Dispatchers.IO) {
            val context = this@MainActivity

            // Migrate shared preferences to the new DataStore
            migrateSharedPreferencesToDataStore(context)
            migrateSetupFinishedToDataStore(context)

            // Launch the setup viewer if the setup has not yet been completed
            if (!getSetting(DataStoreKeys.APP_SETUP_FINISHED)) {
                startActivity(Intent(this@MainActivity, SetupViewer::class.java).apply {
                    setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                })
                finish()
                return@launch
            }

            // Load the configured preferred language
            loadLanguage()

            // Update the app open streak and longest streak values
            updateAppOpenStreak()

            // Populate all static database values
            ensureStaticDatabaseValuesExist()

            // Ensure notification channels exist if supported
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannels()
            }

            // Re-enable all notifications and alarms which are enabled but are not running correctly
            AlarmHandler.reEnableAlarmsIfNotRunning(applicationContext, db.activeAlarmDao.getAllDetails())
            AlarmHandler.reEnableNotificationsIfNotRunning(applicationContext)

            // Shuffle goals for today if no shuffle currently exists
            shuffleGoalsForToday()

            // Create login form or start immediately in case no authentication is required
            startApplicationLogin()
        }

        binding.btnFingerprintUnlock.setOnClickListener { requestBiometricAuthentication() }
    }

    private fun ensureValidDataStructure() {
        val recordingsFolder = File(filesDir.absolutePath + "/Recordings")
        if (!recordingsFolder.exists()) {
            recordingsFolder.mkdir()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun createNotificationChannels() {
        val categories = db.notificationCategoryDao.getAll()
        for (category in categories) {
            val channel = NotificationChannel(category.id, category.description, NotificationManager.IMPORTANCE_DEFAULT).apply {
                enableLights(true)
                lightColor = Color.argb(255, 76, 59, 168)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private suspend fun ensureStaticDatabaseValuesExist() {
        db.dreamTypeDao.insertAll(DreamType.defaultData)
        db.activeAlarmDao.insert(ActiveAlarm.defaultData)
        db.dreamMoodDao.insertAll(DreamMood.defaultData)
        db.dreamClarityDao.insertAll(DreamClarity.defaultData)
        db.sleepQualityDao.insertAll(SleepQuality.defaultData)
        db.alarmToneTypesDao.insertAll(AlarmToneTypes.defaultData)
        db.notificationCategoryDao.insertAll(NotificationCategory.defaultData)
        db.questionTypeDao.insertAll(defaults)

        // Add default sample goals in case no goals are present in the database
        if (db.goalDao.getGoalCount() == 0) {
            db.goalDao.insertAll(DefaultGoals(this).goalsList)
        }
    }

    private suspend fun updateAppOpenStreak() {
        val todayMidnightMillis = Tools.getMidnightTime()
        val timeSinceFirstOpenLatestDay = todayMidnightMillis - getSetting(DataStoreKeys.FIRST_OPEN_LATEST_DAY)
        val dayMillis = TimeUnit.DAYS.toMillis(1)
        if (timeSinceFirstOpenLatestDay == dayMillis) {
            val streak = getSetting(DataStoreKeys.APP_OPEN_STREAK) + 1
            updateSetting(DataStoreKeys.FIRST_OPEN_LATEST_DAY, todayMidnightMillis)
            updateSetting(DataStoreKeys.APP_OPEN_STREAK, streak)
            if (streak > getSetting(DataStoreKeys.APP_OPEN_STREAK_LONGEST)) {
                updateSetting(DataStoreKeys.APP_OPEN_STREAK_LONGEST, streak)
            }
        } else if (timeSinceFirstOpenLatestDay > dayMillis || timeSinceFirstOpenLatestDay < 0) {
            updateSetting(DataStoreKeys.FIRST_OPEN_LATEST_DAY, todayMidnightMillis)
            updateSetting(DataStoreKeys.APP_OPEN_STREAK, 0L)
        }
    }

    private suspend fun shuffleGoalsForToday() {
        val dayTimeSpans = Tools.getTimeSpanFrom(0, true)
        val maybeShuffle = db.shuffleDao.getLastShuffleInDay(dayTimeSpans.first, dayTimeSpans.second)
        if (maybeShuffle == null) {
            val newShuffleId = db.shuffleDao.insert(Shuffle(dayTimeSpans.first, dayTimeSpans.second))
            db.shuffleHasGoalDao.insertAll(Tools.getNewShuffleGoals(this).map {
                ShuffleHasGoal(newShuffleId.toInt(), it.goalId)
            })
        }
    }

    private suspend fun startApplicationLogin() {
        val loginHash = getSetting(DataStoreKeys.AUTHENTICATION_HASH)
        val loginSalt = getSetting(DataStoreKeys.AUTHENTICATION_SALT)
        when (getSetting(DataStoreKeys.AUTHENTICATION_TYPE)) {
            "password" -> {
                runOnUiThread {
                    setupPasswordAuthentication()
                }
                setupBiometricAuthentication()
                storedHash = loginHash
                storedSalt = Base64.decode(loginSalt, Base64.DEFAULT)
            }
            "pin" -> {
                runOnUiThread {
                    setupPinAuthentication()
                }
                setupBiometricAuthentication()
                storedHash = loginHash
                storedSalt = Base64.decode(loginSalt, Base64.DEFAULT)
            }
            else -> runOnUiThread {
                startMainApp()
            }
        }
    }

    private fun setupPasswordAuthentication() {
        binding.llPwAuthContainer.visibility = View.VISIBLE
        binding.llPinAuthContainer.visibility = View.GONE
        binding.btnUnlock.setOnClickListener {
            try {
                val hash = Crypt.encryptString((binding.txtPassword).getText().toString(), storedSalt)
                runOnUiThread {
                    if (hash == storedHash) {
                        startMainApp()
                    } else {
                        Toast.makeText(this@MainActivity, "Invalid password", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun setupPinAuthentication() {
        binding.llPwAuthContainer.visibility = View.GONE
        binding.llPinAuthContainer.visibility = View.VISIBLE

        // Generate the keypad with the specified layout
        val buttons = arrayOf(
                '1', '2', '3',
                '4', '5', '6',
                '7', '8', '9',
                null, '0', '#'
        ).map {
            KeypadButtonModel(it, null)
        }

        // Set the icon for delete
        buttons[buttons.size - 1].buttonIcon = resolveDrawable(R.drawable.rounded_backspace_24)

        // Configure keypad adapter
        val keypadAdapter = KeypadAdapter(this, buttons)
        binding.rcvKeypad.setAdapter(keypadAdapter)
        binding.rcvKeypad.setLayoutManager(GridLayoutManager(this, 3))
        keypadAdapter.onButtonClick = ::onKeyboardButtonClickHandler
    }

    private fun onKeyboardButtonClickHandler(value: Char) {
        // Handle the case when the delete button was clicked
        if (value == '#') {
            if (enteredPin.isNotEmpty()) enteredPin.deleteCharAt(enteredPin.length - 1)
            val pinText = StringBuilder()
            repeat(enteredPin.length) {
                pinText.append("\u2022")
            }
            binding.txtEnteredPin.text = pinText.toString()
            return
        }

        // Add the entered character and validate the new value
        binding.txtEnteredPin.text = binding.txtEnteredPin.text.toString() + "•"
        enteredPin.append(value)
        lifecycleScope.launch(Dispatchers.IO) {
            val isValidPin = checkPin()
            runOnUiThread {
                if (isValidPin) {
                    startMainApp()
                }
                else if (enteredPin.length > 7) {
                    enteredPin.clear()
                    binding.txtEnteredPin.text = ""
                    Toast.makeText(this@MainActivity, "Invalid PIN", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun setupBiometricAuthentication() {
        if (getSetting(DataStoreKeys.AUTHENTICATION_USE_BIOMETRICS)) {
            runOnUiThread {
                val biometricManager = BiometricManager.from(this@MainActivity)
                when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
                    BiometricManager.BIOMETRIC_SUCCESS -> {
                        binding.btnFingerprintUnlock.setVisibility(View.VISIBLE)
                        requestBiometricAuthentication()
                    }
                    BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> showToastLong(this, "Biometric failed: No hardware available")
                    BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> showToastLong(this, "Biometric failed: Hardware unavailable")
                    BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> showToastLong(this, "Biometric failed: No biometric/device credentials enrolled")
                    BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> showToastLong(this, "Biometric failed: Security update required")
                    BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> showToastLong(this, "Biometric failed: Not supported")
                    BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> showToastLong(this, "Biometric failed: Biometric status unknown")
                }
            }
        }
    }

    private fun requestBiometricAuthentication() {
        val context = this@MainActivity
        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(context, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                startMainApp()
            }
        })
        prompt.authenticate(PromptInfo.Builder()
            .setTitle("Biometric Login")
            .setDescription("Log in using your biometric credentials")
            .setNegativeButtonText("Cancel")
            .build())
    }

    private fun checkPin(): Boolean {
        try {
            val hash = Crypt.encryptStringBlowfish(enteredPin.toString(), storedSalt)
            return hash != null && hash == storedHash
        } catch (_: Exception) {
            return false
        }
    }

    private fun startMainApp() {
        // Show loading indicator
        binding.llPwAuthContainer.visibility = View.GONE
        binding.llPinAuthContainer.visibility = View.GONE
        binding.btnFingerprintUnlock.visibility = View.GONE
        binding.clpbStartApp.visibility = View.VISIBLE

        // Create the main viewer intent and set optional extra actions
        val mainViewerIntent = Intent(this, MainViewer::class.java)
        val initialPage = intent.getStringExtra("INITIAL_PAGE")
        val journalType = intent.getIntExtra("DREAM_JOURNAL_TYPE", -1)
        if (initialPage != null) {
            mainViewerIntent.putExtra("INITIAL_PAGE", initialPage)
        }
        if (journalType != -1) {
            mainViewerIntent.putExtra("DREAM_JOURNAL_TYPE", journalType)
        }
        mainViewerIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(mainViewerIntent)
        finish()
    }
}