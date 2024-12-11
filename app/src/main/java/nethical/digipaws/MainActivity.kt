
package nethical.digipaws

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.TimePickerDialog
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
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import nethical.digipaws.databinding.ActivityMainBinding
import nethical.digipaws.databinding.DialogAddToCheatHoursBinding
import nethical.digipaws.databinding.DialogConfigTrackerBinding
import nethical.digipaws.databinding.DialogFocusModeBinding
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
import nethical.digipaws.utils.TimeTools
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var selectPinnedAppsLauncher: ActivityResultLauncher<Intent>

    private lateinit var selectBlockedAppsLauncher: ActivityResultLauncher<Intent>

    private lateinit var selectFocusModeUnblockedAppsLauncher: ActivityResultLauncher<Intent>

    private lateinit var selectBlockedKeywords: ActivityResultLauncher<Intent>

    private lateinit var addCheatHoursActivity: ActivityResultLauncher<Intent>

    val savedPreferencesLoader = SavedPreferencesLoader(this)

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

        checkAccessibilityPermissions()

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
                    openAccessibilitySettings(binding.btnUnlockAntiUninstall)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkAccessibilityPermissions()
    }

    private fun checkAccessibilityPermissions(){
        val isAppBlockerOn = isAccessibilityServiceEnabled(AppBlockerService::class.java)
        updateChip(isAppBlockerOn,binding.appBlockerStatusChip,binding.appBlockerWarning)
        binding.selectBlockedApps.isEnabled = isAppBlockerOn
        binding.btnConfigAppblockerWarning.isEnabled = isAppBlockerOn
        binding.appBlockerSelectCheatHours.isEnabled = isAppBlockerOn


        val isViewBlockerOn = isAccessibilityServiceEnabled(ViewBlockerService::class.java)
        updateChip(isViewBlockerOn,binding.viewBlockerStatusChip,binding.viewBlockerWarning)
        binding.btnConfigViewblockerCheatHours.isEnabled = isViewBlockerOn
        binding.btnConfigViewblockerWarning.isEnabled = isViewBlockerOn

        val isKeywordBlockerOn = isAccessibilityServiceEnabled(KeywordBlockerService::class.java)
        updateChip(isKeywordBlockerOn,binding.keywordBlockerStatusChip,binding.keywordBlockerWarning)
        binding.selectBlockedKeywords.isEnabled = isKeywordBlockerOn

        val isUsageTrackerOn = isAccessibilityServiceEnabled(UsageTrackingService::class.java)
        updateChip(isUsageTrackerOn,binding.usageTrackerStatusChip,binding.usageTrackerWarning)
        binding.selectUsageStats.isEnabled = isUsageTrackerOn
        binding.btnConfigTracker.isEnabled = isUsageTrackerOn


        val isGeneralSettingsOn = isAccessibilityServiceEnabled(DigipawsMainService::class.java)
        updateChip(isGeneralSettingsOn, binding.focusModeStatusChip, binding.focusModeWarning)
        binding.startFocusMode.isEnabled = isGeneralSettingsOn
        binding.selectFocusUnblockedApps.isEnabled = isGeneralSettingsOn


        val devicePolicyManager =
            getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val componentName = ComponentName(this, AdminReceiver::class.java)
        // Check if Device Admin is active
        isDeviceAdminOn = devicePolicyManager.isAdminActive(componentName)


        val antiUninstallInfo = getSharedPreferences("anti_uninstall", Context.MODE_PRIVATE)
        isAntiUninstallOn =
            antiUninstallInfo.getBoolean("is_anti_uninstall_on", false)
        binding.btnUnlockAntiUninstall.isEnabled = isAntiUninstallOn

        if (!isDeviceAdminOn) {
            binding.antiUninstallWarning.text = "Please enable device admin"
        } else {
            if (!isGeneralSettingsOn) {
                binding.antiUninstallWarning.text =
                    "Please enable General features accessibility service"
            }
        }
        if (isAntiUninstallOn) {
        }
        if (isDeviceAdminOn && isGeneralSettingsOn) {
            updateChip(true, binding.antiUninstallCardChip, binding.antiUninstallWarning)

            if (isAntiUninstallOn) {
                binding.antiUninstallCardChip.isEnabled = false
            } else {
                binding.antiUninstallCardChip.text = "Enter Setup"
                binding.antiUninstallCardChip.isEnabled = true
            }
        }


    }

    private fun updateChip(isEnabled: Boolean,statusChip: Chip,warningText:TextView) {
        if (isEnabled) {
            statusChip.text = "Enabled"
            statusChip.chipIcon = null
            warningText.visibility = View.GONE
        } else {
            statusChip.text = "Disabled"
            statusChip.setChipIconResource(R.drawable.baseline_warning_24)
            warningText.visibility = View.VISIBLE
        }
    }
    private fun sendRefreshRequest(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }
    fun isAccessibilityServiceEnabled(serviceClass: Class<out AccessibilityService>): Boolean {
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

        val prevdata = savedPreferencesLoader.loadAppBlockerWarningInfo()
        tweakAppBlockerWarningBinding.selectMins.value = prevdata.timeInterval / 60000
        tweakAppBlockerWarningBinding.warningMsgEdit.setText(prevdata.message)
        MaterialAlertDialogBuilder(this)
            .setTitle("Configure Warning Screen")
            .setView(tweakAppBlockerWarningBinding.root)
            .setPositiveButton("Save") { dialog, _ ->
                val selectedMinInMs = tweakAppBlockerWarningBinding.selectMins.value * 60000
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
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    @SuppressLint("ApplySharedPref")
    private fun makeTweakViewBlockerWarningIntervalDialog() {
        val tweakViewlockerWarningBinding: DialogTweakBlockerWarningBinding =
            DialogTweakBlockerWarningBinding.inflate(layoutInflater)
        tweakViewlockerWarningBinding.selectMins.minValue = 1
        tweakViewlockerWarningBinding.selectMins.maxValue = 240

        tweakViewlockerWarningBinding.cbFirstReel.visibility = View.VISIBLE
        tweakViewlockerWarningBinding.cbReelInbox.visibility = View.VISIBLE


        val prevdata = savedPreferencesLoader.loadViewBlockerWarningInfo()
        tweakViewlockerWarningBinding.selectMins.value = prevdata.timeInterval / 60000
        tweakViewlockerWarningBinding.warningMsgEdit.setText(prevdata.message)
        tweakViewlockerWarningBinding.cbDynamicWarning.isChecked =
            prevdata.isDynamicIntervalSettingAllowed

        val addReelData = getSharedPreferences("config_reels", Context.MODE_PRIVATE)
        tweakViewlockerWarningBinding.cbReelInbox.isChecked =
            addReelData.getBoolean("is_reel_inbox", false)
        tweakViewlockerWarningBinding.cbFirstReel.isChecked =
            addReelData.getBoolean("is_reel_first", false)

        MaterialAlertDialogBuilder(this)
            .setTitle("Configure Warning Screen")
            .setView(tweakViewlockerWarningBinding.root)
            .setPositiveButton("Save") { dialog, _ ->
                val selectedMinInMs = tweakViewlockerWarningBinding.selectMins.value * 60000
                savedPreferencesLoader.saveViewBlockerWarningInfo(
                    WarningData(
                        tweakViewlockerWarningBinding.warningMsgEdit.text.toString(),
                        selectedMinInMs,
                        tweakViewlockerWarningBinding.cbDynamicWarning.isChecked
                    )
                )
                val editor = addReelData.edit()
                editor.putBoolean(
                    "is_reel_inbox",
                    tweakViewlockerWarningBinding.cbReelInbox.isChecked
                )
                editor.putBoolean(
                    "is_reel_first",
                    tweakViewlockerWarningBinding.cbFirstReel.isChecked
                )

                editor.commit()
                sendRefreshRequest(ViewBlockerService.INTENT_ACTION_REFRESH_VIEW_BLOCKER)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    fun openAccessibilitySettings(view: View){
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

    private fun makeDialogConfigTracker(){
        val dialogconfigTracker = DialogConfigTrackerBinding.inflate(layoutInflater)

        val sp = getSharedPreferences("config_tracker",Context.MODE_PRIVATE)

        dialogconfigTracker.cbReelCounter.isChecked = sp.getBoolean("is_reel_counter",true)
        dialogconfigTracker.cbTimeElapsed.isChecked = sp.getBoolean("is_time_elapsed",true)

        MaterialAlertDialogBuilder(this)
            .setTitle("Configure Warning Screen")
            .setView(dialogconfigTracker.root)
            .setPositiveButton("Save") { dialog, _ ->

                val editor = sp.edit()
                editor.putBoolean(
                    "is_reel_counter",
                    dialogconfigTracker.cbReelCounter.isChecked
                )
                editor.putBoolean(
                    "is_time_elapsed",
                    dialogconfigTracker.cbTimeElapsed.isChecked
                )

                editor.commit()
                sendRefreshRequest(UsageTrackingService.INTENT_ACTION_REFRESH_USAGE_TRACKER)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()


    }

    private fun makeViewBlockerCheatHoursDialog() {

        val dialogAddToCheatHoursBinding = DialogAddToCheatHoursBinding.inflate(layoutInflater)


        dialogAddToCheatHoursBinding.btnSelectUnblockedApps.visibility = View.GONE
        dialogAddToCheatHoursBinding.cheatHourTitle.visibility = View.GONE


        val viewBlockerCheatHours = getSharedPreferences("cheat_hours", Context.MODE_PRIVATE)
        var endTimeInMins =
            viewBlockerCheatHours.getInt("view_blocker_start_time", -1)
        var startTimeInMins = viewBlockerCheatHours.getInt("view_blocker_end_time", -1)
        val isProceedBtnDisabled =
            viewBlockerCheatHours.getBoolean("view_blocker_is_proceed_disabled", false)

        val convertedStartTime = TimeTools.convertMinutesTo24Hour(startTimeInMins)
        val convertedEndTIme = TimeTools.convertMinutesTo24Hour(endTimeInMins)

        dialogAddToCheatHoursBinding.btnSelectEndTime.text =
            "Start Time: ${convertedStartTime.first}:${convertedStartTime.second}"

        dialogAddToCheatHoursBinding.btnSelectStartTime.text =
            "End Time: ${convertedEndTIme.first}:${convertedEndTIme.second}"


        dialogAddToCheatHoursBinding.cbDisableProceed.isChecked = isProceedBtnDisabled


        dialogAddToCheatHoursBinding.btnSelectEndTime.setOnClickListener {
            if (startTimeInMins == null) {
                Toast.makeText(
                    this,
                    "Please Specify Start Time first",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val timePickerDialog = TimePickerDialog(
                this,
                { _, selectedHour, selectedMinute ->
                    val selectedEndTime =
                        TimeTools.convertToMinutesFromMidnight(selectedHour, selectedMinute)

                    // Ensure end time is after start time
                    if (startTimeInMins != null && selectedEndTime <= startTimeInMins!!) {
                        Toast.makeText(
                            this,
                            "End time must be after start time!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        endTimeInMins = selectedEndTime
                        dialogAddToCheatHoursBinding.btnSelectEndTime.text =
                            "End Time: " + String.format("%02d:%02d", selectedHour, selectedMinute)
                    }
                },
                hour,
                minute,
                false
            )
            timePickerDialog.show()
        }
        dialogAddToCheatHoursBinding.btnSelectStartTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val timePickerDialog = TimePickerDialog(
                this,
                { _, selectedHour, selectedMinute ->
                    startTimeInMins =
                        TimeTools.convertToMinutesFromMidnight(selectedHour, selectedMinute)
                    dialogAddToCheatHoursBinding.btnSelectStartTime.text =
                        "Start Time: " + String.format("%02d:%02d", selectedHour, selectedMinute)
                },
                hour,
                minute,
                false // Use 24-hour format
            )
            timePickerDialog.show()
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Specify Cheat Hours")
            .setView(dialogAddToCheatHoursBinding.root)
            .setPositiveButton("Save") { dialog, _ ->
                if (startTimeInMins == null) {
                    Toast.makeText(this, "Please Select a start time", Toast.LENGTH_SHORT).show()
                } else if (endTimeInMins == null) {
                    Toast.makeText(this, "Please Select an end time", Toast.LENGTH_SHORT).show()
                } else {
                    savedPreferencesLoader.saveCheatHoursForViewBlocker(
                        startTimeInMins!!,
                        endTimeInMins!!,
                        dialogAddToCheatHoursBinding.cbDisableProceed.isChecked
                    )
                    sendRefreshRequest(ViewBlockerService.INTENT_ACTION_REFRESH_VIEW_BLOCKER)
                    dialog.dismiss()
                }

            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }


    private fun makeStartFocusModeDialog() {
        val dialogFocusModeBinding = DialogFocusModeBinding.inflate(layoutInflater)
        dialogFocusModeBinding.focusModeMinsPicker.minValue = 1
        dialogFocusModeBinding.focusModeMinsPicker.maxValue = 10000
        MaterialAlertDialogBuilder(this)
            .setTitle("Start Focus Mode")
            .setView(dialogFocusModeBinding.root)
            .setPositiveButton("Start") { _, _ ->
                val totalMillis = dialogFocusModeBinding.focusModeMinsPicker.value * 60000
                savedPreferencesLoader.saveFocusModeData(
                    DigipawsMainService.FocusModeData(
                        true,
                        System.currentTimeMillis() + totalMillis
                    )
                )
                sendRefreshRequest(DigipawsMainService.INTENT_ACTION_REFRESH_FOCUS_MODE)
                val timer = NotificationTimerManager(this)
                // TODO: add permission check
                timer.startTimer(totalMillis.toLong())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    @SuppressLint("ApplySharedPref")
    private fun makeRemoveAntiUninstallDialog() {
        val dialogRemoveAntiUninstall = DialogRemoveAntiUninstallBinding.inflate(layoutInflater)
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
                );


                val today = Calendar.getInstance()
                if (selectedDate.before(today)) {
                    Snackbar.make(
                        binding.root,
                        "Anti Uninstall removed",
                        Snackbar.LENGTH_SHORT
                    )
                        .show()
                    antiUninstallInfo.edit().putBoolean("is_anti_uninstall_on", false).commit()
                    sendRefreshRequest(DigipawsMainService.INTENT_ACTION_REFRESH_ANTI_UNINSTALL)

                } else {
                    val daysDiff =
                        (selectedDate.timeInMillis - today.timeInMillis) / (1000 * 60 * 60 * 24)

                    MaterialAlertDialogBuilder(this)
                        .setTitle("Failed")
                        .setMessage("You still have $daysDiff days to go before unlocking anti-uninstall")
                        .setPositiveButton("Ok", null)
                        .show()
                }

            }

            Constants.ANTI_UNINSTALL_PASSWORD_MODE -> {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Remove Anti-Uninstall")
                    .setView(dialogRemoveAntiUninstall.root)
                    .setPositiveButton("Remove") { _, _ ->
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

                            checkAccessibilityPermissions()
                        } else {
                            Snackbar.make(
                                binding.root,
                                "Incorrect password. Please try again. " + antiUninstallInfo.getString(
                                    "password",
                                    "pass"
                                ),
                                Snackbar.LENGTH_SHORT
                            )
                                .setAction("Retry") {
                                    makeRemoveAntiUninstallDialog()
                                }
                                .show()
                        }
                    }
                    .setNegativeButton("Cancel", null)
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