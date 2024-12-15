package nethical.digipaws.ui.activity

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nethical.digipaws.Constants
import nethical.digipaws.R
import nethical.digipaws.databinding.ActivityMainBinding
import nethical.digipaws.databinding.DialogAddToCheatHoursBinding
import nethical.digipaws.databinding.DialogConfigTrackerBinding
import nethical.digipaws.databinding.DialogFocusModeBinding
import nethical.digipaws.databinding.DialogKeywordPackageBinding
import nethical.digipaws.databinding.DialogRemoveAntiUninstallBinding
import nethical.digipaws.databinding.DialogTweakBlockerWarningBinding
import nethical.digipaws.receivers.AdminReceiver
import nethical.digipaws.services.AppBlockerService
import nethical.digipaws.services.DigipawsMainService
import nethical.digipaws.services.KeywordBlockerService
import nethical.digipaws.services.UsageTrackingService
import nethical.digipaws.services.ViewBlockerService
import nethical.digipaws.utils.NotificationTimerManager
import nethical.digipaws.utils.SavedPreferencesLoader
import nl.joery.timerangepicker.TimeRangePicker
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var selectPinnedAppsLauncher: ActivityResultLauncher<Intent>

    private lateinit var selectBlockedAppsLauncher: ActivityResultLauncher<Intent>

    private lateinit var selectFocusModeUnblockedAppsLauncher: ActivityResultLauncher<Intent>

    private lateinit var selectBlockedKeywords: ActivityResultLauncher<Intent>

    private lateinit var addCheatHoursActivity: ActivityResultLauncher<Intent>

    private val savedPreferencesLoader = SavedPreferencesLoader(this)

    private var isDeviceAdminOn = false
    private var isAntiUninstallOn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setupActivityLaunchers()
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    private fun setupActivityLaunchers() {

        selectPinnedAppsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val selectedApps = result.data?.getStringArrayListExtra("SELECTED_APPS")
                selectedApps?.let {
                    savedPreferencesLoader.savePinned(it.toSet())
                }
            }
        }

        selectBlockedAppsLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val selectedApps = result.data?.getStringArrayListExtra("SELECTED_APPS")
                    selectedApps?.let {
                        savedPreferencesLoader.saveBlockedApps(it.toSet())
                        sendRefreshRequest(AppBlockerService.INTENT_ACTION_REFRESH_APP_BLOCKER)
                    }
                }
            }


        selectFocusModeUnblockedAppsLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val selectedApps = result.data?.getStringArrayListExtra("SELECTED_APPS")
                    selectedApps?.let {
                        savedPreferencesLoader.saveFocusModeUnblockedApps(selectedApps)
                    }
                }
            }

        selectBlockedKeywords =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val blockedKeywords = result.data?.getStringArrayListExtra("SELECTED_KEYWORDS")
                    blockedKeywords?.let {
                        savedPreferencesLoader.saveBlockedKeywords(it.toSet())
                        sendRefreshRequest(KeywordBlockerService.INTENT_ACTION_REFRESH_BLOCKED_KEYWORD_LIST)
                    }
                }
            }

        addCheatHoursActivity =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
                sendRefreshRequest(AppBlockerService.INTENT_ACTION_REFRESH_APP_BLOCKER)
            }
    }

    private fun setupClickListeners() {

        binding.selectPinnedApps.setOnClickListener {
            val intent = Intent(this, SelectAppsActivity::class.java)
            intent.putStringArrayListExtra(
                "PRE_SELECTED_APPS",
                ArrayList(savedPreferencesLoader.loadPinnedApps())
            )
            selectPinnedAppsLauncher.launch(intent)
        }

        binding.selectBlockedApps.setOnClickListener {
            val intent = Intent(this, SelectAppsActivity::class.java)
            intent.putStringArrayListExtra(
                "PRE_SELECTED_APPS",
                ArrayList(savedPreferencesLoader.loadBlockedApps())
            )
            selectBlockedAppsLauncher.launch(intent)
        }

        binding.selectBlockedKeywords.setOnClickListener {
            val intent = Intent(this, ManageKeywordsActivity::class.java)
            intent.putStringArrayListExtra(
                "PRE_SAVED_KEYWORDS",
                ArrayList(savedPreferencesLoader.loadBlockedKeywords())
            )
            selectBlockedKeywords.launch(intent)
        }

        binding.appBlockerSelectCheatHours.setOnClickListener {
            val intent = Intent(this, AddCheatHoursActivity::class.java)
            addCheatHoursActivity.launch(intent)
        }
        binding.btnConfigAppblockerWarning.setOnClickListener {
            makeTweakAppBlockerWarningIntervalDialog()
        }
        binding.btnConfigViewblockerWarning.setOnClickListener {
            makeTweakViewBlockerWarningIntervalDialog()
        }
        binding.btnConfigViewblockerCheatHours.setOnClickListener {
            makeViewBlockerCheatHoursDialog()
        }
        binding.btnConfigTracker.setOnClickListener{
            makeDialogConfigTracker()
        }
        binding.selectUsageStats.setOnClickListener {
            val intent = Intent(this, UsageMetricsActivity::class.java)
            startActivity(intent)
        }
        binding.startFocusMode.setOnClickListener {
            makeStartFocusModeDialog()
        }
        binding.btnUnlockAntiUninstall.setOnClickListener {
            makeRemoveAntiUninstallDialog()
        }
        binding.btnManagePreinstalledKeywords.setOnClickListener {
            manageKeywordPackDialog()
        }
        binding.selectFocusUnblockedApps.setOnClickListener {
            val intent = Intent(this, SelectAppsActivity::class.java)
            intent.putStringArrayListExtra(
                "PRE_SELECTED_APPS",
                ArrayList(savedPreferencesLoader.getFocusModeUnblockedApps())
            )
            selectFocusModeUnblockedAppsLauncher.launch(intent)
        }

        binding.antiUninstallCardChip.setOnClickListener {
            if (!isDeviceAdminOn) {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                val componentName = ComponentName(this, AdminReceiver::class.java)
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                intent.putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Enable admin to enable anti uninstall."
                )
                startActivity(intent)
            } else {
                if (binding.antiUninstallWarning.visibility == View.GONE) {
                    val intent = Intent(this, SetupAntiUninstallActivity::class.java)
                    startActivity(intent)
                } else {
                    openAccessibilitySettings(binding.root)
                }
            }
        }
    }

    private fun checkPermissions() {
        lifecycleScope.launch {
            val isAppBlockerOn =
                withContext(Dispatchers.IO) { isAccessibilityServiceEnabled(AppBlockerService::class.java) }
            val isViewBlockerOn =
                withContext(Dispatchers.IO) { isAccessibilityServiceEnabled(ViewBlockerService::class.java) }
            val isKeywordBlockerOn =
                withContext(Dispatchers.IO) { isAccessibilityServiceEnabled(KeywordBlockerService::class.java) }
            val isUsageTrackerOn =
                withContext(Dispatchers.IO) { isAccessibilityServiceEnabled(UsageTrackingService::class.java) }
            val isGeneralSettingsOn =
                withContext(Dispatchers.IO) { isAccessibilityServiceEnabled(DigipawsMainService::class.java) }

            val devicePolicyManager =
                getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val componentName = ComponentName(applicationContext, AdminReceiver::class.java)

            // Check if Device Admin is active
            isDeviceAdminOn = devicePolicyManager.isAdminActive(componentName)

            val antiUninstallInfo = getSharedPreferences("anti_uninstall", Context.MODE_PRIVATE)
            isAntiUninstallOn = antiUninstallInfo.getBoolean("is_anti_uninstall_on", false)

            withContext(Dispatchers.Main) {
                // App Blocker
                updateChip(isAppBlockerOn, binding.appBlockerStatusChip, binding.appBlockerWarning)
                binding.apply {
                    selectBlockedApps.isEnabled = isAppBlockerOn
                    btnConfigAppblockerWarning.isEnabled = isAppBlockerOn
                    appBlockerSelectCheatHours.isEnabled = isAppBlockerOn
                }

                // View Blocker
                updateChip(
                    isViewBlockerOn,
                    binding.viewBlockerStatusChip,
                    binding.viewBlockerWarning
                )
                binding.btnConfigViewblockerCheatHours.isEnabled = isViewBlockerOn
                binding.btnConfigViewblockerWarning.isEnabled = isViewBlockerOn

                // Keyword Blocker
                updateChip(
                    isKeywordBlockerOn,
                    binding.keywordBlockerStatusChip,
                    binding.keywordBlockerWarning
                )
                binding.selectBlockedKeywords.isEnabled = isKeywordBlockerOn
                binding.btnManagePreinstalledKeywords.isEnabled = isKeywordBlockerOn

                // Usage Tracker
                updateChip(
                    isUsageTrackerOn,
                    binding.usageTrackerStatusChip,
                    binding.usageTrackerWarning
                )
                binding.selectUsageStats.isEnabled = isUsageTrackerOn
                binding.btnConfigTracker.isEnabled = isUsageTrackerOn

                // General Settings
                updateChip(
                    isGeneralSettingsOn,
                    binding.focusModeStatusChip,
                    binding.focusModeWarning
                )
                binding.startFocusMode.isEnabled = isGeneralSettingsOn
                binding.selectFocusUnblockedApps.isEnabled = isGeneralSettingsOn

                // Anti-Uninstall settings
                binding.btnUnlockAntiUninstall.isEnabled = isAntiUninstallOn

                // Update Anti-Uninstall warning
                if (!isDeviceAdminOn) {
                    binding.antiUninstallWarning.text =
                        getString(R.string.please_enable_device_admin)
                } else if (!isGeneralSettingsOn) {
                    binding.antiUninstallWarning.text = getString(R.string.warning_general_settings)
                }

                // Handle anti-uninstall UI changes
                if (isDeviceAdminOn && isGeneralSettingsOn) {
                    updateChip(true, binding.antiUninstallCardChip, binding.antiUninstallWarning)
                    binding.antiUninstallCardChip.isEnabled = !isAntiUninstallOn
                    binding.antiUninstallCardChip.text =
                        if (isAntiUninstallOn) getString(R.string.setup_complete) else getString(R.string.enter_setup)
                }
            }
        }
    }


    private fun updateChip(isEnabled: Boolean,statusChip: Chip,warningText:TextView) {
        if (isEnabled) {
            statusChip.text = getString(R.string.enabled)
            statusChip.chipIcon = null
            warningText.visibility = View.GONE
        } else {
            statusChip.text = getString(R.string.disabled)
            statusChip.setChipIconResource(R.drawable.baseline_warning_24)
            warningText.visibility = View.VISIBLE
        }
    }
    private fun sendRefreshRequest(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }
    private fun isAccessibilityServiceEnabled(serviceClass: Class<out AccessibilityService>): Boolean {
        val serviceName = ComponentName(this, serviceClass).flattenToString()
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val isAccessibilityEnabled = Settings.Secure.getInt(
            contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            0
        )
        return isAccessibilityEnabled == 1 && enabledServices.contains(serviceName)
    }

    private fun makeTweakAppBlockerWarningIntervalDialog() {
        val tweakAppBlockerWarningBinding: DialogTweakBlockerWarningBinding =
            DialogTweakBlockerWarningBinding.inflate(layoutInflater)
        tweakAppBlockerWarningBinding.selectMins.minValue = 1
        tweakAppBlockerWarningBinding.selectMins.maxValue = 240

        val previousData = savedPreferencesLoader.loadAppBlockerWarningInfo()
        tweakAppBlockerWarningBinding.selectMins.setValue(previousData.timeInterval / 60000)
        tweakAppBlockerWarningBinding.warningMsgEdit.setText(previousData.message)
        MaterialAlertDialogBuilder(this)
            .setView(tweakAppBlockerWarningBinding.root)
            .setPositiveButton(getString(R.string.save)) { dialog, _ ->
                val selectedMinInMs = tweakAppBlockerWarningBinding.selectMins.getValue() * 60000
                savedPreferencesLoader.saveAppBlockerWarningInfo(
                    WarningData(
                        tweakAppBlockerWarningBinding.warningMsgEdit.text.toString(),
                        selectedMinInMs,
                        tweakAppBlockerWarningBinding.cbDynamicWarning.isChecked
                    )
                )
                sendRefreshRequest(AppBlockerService.INTENT_ACTION_REFRESH_APP_BLOCKER)
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    @SuppressLint("ApplySharedPref")
    private fun makeTweakViewBlockerWarningIntervalDialog() {
        val tweakViewBlockerWarningBinding: DialogTweakBlockerWarningBinding =
            DialogTweakBlockerWarningBinding.inflate(layoutInflater)
        tweakViewBlockerWarningBinding.selectMins.minValue = 1
        tweakViewBlockerWarningBinding.selectMins.maxValue = 240

        tweakViewBlockerWarningBinding.cbFirstReel.visibility = View.VISIBLE
        tweakViewBlockerWarningBinding.cbReelInbox.visibility = View.VISIBLE


        val previousData = savedPreferencesLoader.loadViewBlockerWarningInfo()
        tweakViewBlockerWarningBinding.selectMins.setValue(previousData.timeInterval / 60000)
        tweakViewBlockerWarningBinding.warningMsgEdit.setText(previousData.message)
        tweakViewBlockerWarningBinding.cbDynamicWarning.isChecked =
            previousData.isDynamicIntervalSettingAllowed

        val addReelData = getSharedPreferences("config_reels", Context.MODE_PRIVATE)
        tweakViewBlockerWarningBinding.cbReelInbox.isChecked =
            addReelData.getBoolean("is_reel_inbox", false)
        tweakViewBlockerWarningBinding.cbFirstReel.isChecked =
            addReelData.getBoolean("is_reel_first", false)

        MaterialAlertDialogBuilder(this)
            .setView(tweakViewBlockerWarningBinding.root)
            .setPositiveButton(getString(R.string.save)) { dialog, _ ->
                val selectedMinInMs = tweakViewBlockerWarningBinding.selectMins.getValue() * 60000
                savedPreferencesLoader.saveViewBlockerWarningInfo(
                    WarningData(
                        tweakViewBlockerWarningBinding.warningMsgEdit.text.toString(),
                        selectedMinInMs,
                        tweakViewBlockerWarningBinding.cbDynamicWarning.isChecked
                    )
                )
                val editor = addReelData.edit()
                editor.putBoolean(
                    "is_reel_inbox",
                    tweakViewBlockerWarningBinding.cbReelInbox.isChecked
                )
                editor.putBoolean(
                    "is_reel_first",
                    tweakViewBlockerWarningBinding.cbFirstReel.isChecked
                )

                editor.commit()
                sendRefreshRequest(ViewBlockerService.INTENT_ACTION_REFRESH_VIEW_BLOCKER)
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    fun openAccessibilitySettings(view: View) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Enable Accessibility")
            .setMessage("This app requires Accessibility permissions to function properly.")
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    @SuppressLint("ApplySharedPref")
    private fun makeDialogConfigTracker(){
        val dialogConfigurationTracker = DialogConfigTrackerBinding.inflate(layoutInflater)

        val sp = getSharedPreferences("config_tracker",Context.MODE_PRIVATE)

        dialogConfigurationTracker.cbReelCounter.isChecked = sp.getBoolean("is_reel_counter", true)
        dialogConfigurationTracker.cbTimeElapsed.isChecked = sp.getBoolean("is_time_elapsed", true)

        MaterialAlertDialogBuilder(this)
            .setView(dialogConfigurationTracker.root)
            .setPositiveButton(getString(R.string.save)) { dialog, _ ->

                val editor = sp.edit()
                editor.putBoolean(
                    "is_reel_counter",
                    dialogConfigurationTracker.cbReelCounter.isChecked
                )
                editor.putBoolean(
                    "is_time_elapsed",
                    dialogConfigurationTracker.cbTimeElapsed.isChecked
                )

                editor.commit()
                sendRefreshRequest(UsageTrackingService.INTENT_ACTION_REFRESH_USAGE_TRACKER)
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()


    }

    private fun makeViewBlockerCheatHoursDialog() {

        val dialogAddToCheatHoursBinding = DialogAddToCheatHoursBinding.inflate(layoutInflater)


        dialogAddToCheatHoursBinding.btnSelectUnblockedApps.visibility = View.GONE
        dialogAddToCheatHoursBinding.cheatHourTitle.visibility = View.GONE


        val viewBlockerCheatHours = getSharedPreferences("cheat_hours", Context.MODE_PRIVATE)
        val savedEndTimeInMinutes =
            viewBlockerCheatHours.getInt("view_blocker_start_time", -1)
        val savedStartTimeInMinutes = viewBlockerCheatHours.getInt("view_blocker_end_time", -1)
        val isProceedBtnDisabled =
            viewBlockerCheatHours.getBoolean("view_blocker_is_proceed_disabled", false)
        dialogAddToCheatHoursBinding.cbDisableProceed.isChecked = isProceedBtnDisabled

        var endTimeInMins: Int? = null
        var startTimeInMins: Int? = null

        if (savedStartTimeInMinutes != -1 || savedEndTimeInMinutes != -1) {
            dialogAddToCheatHoursBinding.picker.startTime =
                TimeRangePicker.Time(savedStartTimeInMinutes)
            dialogAddToCheatHoursBinding.picker.endTime =
                TimeRangePicker.Time(savedEndTimeInMinutes)
            startTimeInMins = dialogAddToCheatHoursBinding.picker.startTimeMinutes
            endTimeInMins = dialogAddToCheatHoursBinding.picker.endTimeMinutes
        } else {
            dialogAddToCheatHoursBinding.picker.startTimeMinutes = 0
            dialogAddToCheatHoursBinding.picker.endTimeMinutes = 0
            dialogAddToCheatHoursBinding.fromTime.text = getString(R.string.from)
            dialogAddToCheatHoursBinding.endTime.text = getString(R.string.end)
        }



        dialogAddToCheatHoursBinding.picker.setOnTimeChangeListener(object :
            TimeRangePicker.OnTimeChangeListener {
            override fun onStartTimeChange(startTime: TimeRangePicker.Time) {
                dialogAddToCheatHoursBinding.fromTime.text =
                    dialogAddToCheatHoursBinding.picker.startTime.toString()
                startTimeInMins = dialogAddToCheatHoursBinding.picker.startTimeMinutes
            }

            override fun onEndTimeChange(endTime: TimeRangePicker.Time) {
                dialogAddToCheatHoursBinding.endTime.text =
                    dialogAddToCheatHoursBinding.picker.endTime.toString()
                endTimeInMins = dialogAddToCheatHoursBinding.picker.endTimeMinutes
            }

            override fun onDurationChange(duration: TimeRangePicker.TimeDuration) {
            }
        })

        MaterialAlertDialogBuilder(this)
            .setView(dialogAddToCheatHoursBinding.root)
            .setPositiveButton(getString(R.string.save)) { dialog, _ ->
                if (startTimeInMins == null || endTimeInMins == null) {
                    Toast.makeText(
                        this,
                        getString(R.string.please_specify_time),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }
                savedPreferencesLoader.saveCheatHoursForViewBlocker(
                    savedStartTimeInMinutes,
                    savedEndTimeInMinutes,
                    dialogAddToCheatHoursBinding.cbDisableProceed.isChecked
                )
                sendRefreshRequest(ViewBlockerService.INTENT_ACTION_REFRESH_VIEW_BLOCKER)
                dialog.dismiss()

            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }


    private fun makeStartFocusModeDialog() {
        val dialogFocusModeBinding = DialogFocusModeBinding.inflate(layoutInflater)
        MaterialAlertDialogBuilder(this)
            .setView(dialogFocusModeBinding.root)
            .setPositiveButton(getString(R.string.start)) { _, _ ->
                val totalMillis = dialogFocusModeBinding.focusModeMinsPicker.getValue() * 60000
                savedPreferencesLoader.saveFocusModeData(
                    DigipawsMainService.FocusModeData(
                        true,
                        System.currentTimeMillis() + totalMillis
                    )
                )
                sendRefreshRequest(DigipawsMainService.INTENT_ACTION_REFRESH_FOCUS_MODE)
                val timer = NotificationTimerManager(this)
                // TODO: add notification permission check
                timer.startTimer(totalMillis.toLong())
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    @SuppressLint("ApplySharedPref")
    private fun manageKeywordPackDialog() {
        val dialogManageKeywordPacks = DialogKeywordPackageBinding.inflate(layoutInflater)

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.manage_keyword_blockers))
            .setView(dialogManageKeywordPacks.root)
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                val sp = getSharedPreferences("keyword_blocker_packs", Context.MODE_PRIVATE)
                sp.edit()
                    .putBoolean("adult_blocker", dialogManageKeywordPacks.cbAdultKeywords.isChecked)
                    .commit()
                sendRefreshRequest(KeywordBlockerService.INTENT_ACTION_REFRESH_BLOCKED_KEYWORD_LIST)
            }
            .show()
    }

    @SuppressLint("ApplySharedPref")
    private fun makeRemoveAntiUninstallDialog() {
        val antiUninstallInfo = getSharedPreferences("anti_uninstall", Context.MODE_PRIVATE)
        val mode = antiUninstallInfo.getInt("mode", -1)
        when (mode) {

            Constants.ANTI_UNINSTALL_TIMED_MODE -> {
                val dateString = antiUninstallInfo.getString("date", null)
                val parts: List<String> = dateString!!.split("/")
                val selectedDate = Calendar.getInstance()
                selectedDate.set(
                    Integer.parseInt(parts[2]),  // Year
                    Integer.parseInt(parts[0]) - 1,  // Month (0-based)
                    Integer.parseInt(parts[1])  // Day
                )


                val today = Calendar.getInstance()
                if (selectedDate.before(today)) {
                    Snackbar.make(
                        binding.root,
                        getString(R.string.anti_uninstall_removed),
                        Snackbar.LENGTH_SHORT
                    )
                        .show()
                    antiUninstallInfo.edit().putBoolean("is_anti_uninstall_on", false).commit()
                    sendRefreshRequest(DigipawsMainService.INTENT_ACTION_REFRESH_ANTI_UNINSTALL)

                } else {
                    val daysDiff =
                        (selectedDate.timeInMillis - today.timeInMillis) / (1000 * 60 * 60 * 24)

                    MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.failed))
                        .setMessage(getString(R.string.remaining_time_anti_uninstall, daysDiff))
                        .setPositiveButton("Ok", null)
                        .show()
                }

            }

            Constants.ANTI_UNINSTALL_PASSWORD_MODE -> {
                val dialogRemoveAntiUninstall =
                    DialogRemoveAntiUninstallBinding.inflate(layoutInflater)
                MaterialAlertDialogBuilder(this)
                    .setTitle(getString(R.string.remove_anti_uninstall))
                    .setView(dialogRemoveAntiUninstall.root)
                    .setPositiveButton(R.string.remove) { _, _ ->
                        if (antiUninstallInfo.getString(
                                "password",
                                "pass"
                            ) == dialogRemoveAntiUninstall.password.text.toString()
                        ) {
                            antiUninstallInfo.edit().putBoolean("is_anti_uninstall_on", false)
                                .commit()
                            sendRefreshRequest(DigipawsMainService.INTENT_ACTION_REFRESH_ANTI_UNINSTALL)

                            Snackbar.make(
                                binding.root,
                                "Anti Uninstall removed",
                                Snackbar.LENGTH_SHORT
                            )
                                .show()

                            checkPermissions()
                        } else {
                            Snackbar.make(
                                binding.root,
                                getString(R.string.incorrect_password_please_try_again) + antiUninstallInfo.getString(
                                    "password",
                                    "pass"
                                ),
                                Snackbar.LENGTH_SHORT
                            )
                                .setAction(getString(R.string.retry)) {
                                    makeRemoveAntiUninstallDialog()
                                }
                                .show()
                        }
                    }
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show()
            }
        }

    }

    data class WarningData(
        val message: String = "",
        val timeInterval: Int = 5000,
        val isDynamicIntervalSettingAllowed: Boolean = false
    )

}