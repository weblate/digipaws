
package nethical.digipaws

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import nethical.digipaws.databinding.ActivityMainBinding
import nethical.digipaws.databinding.DialogAddToCheatHoursBinding
import nethical.digipaws.databinding.DialogTweakBlockerWarningBinding
import nethical.digipaws.services.AppBlockerService
import nethical.digipaws.services.KeywordBlockerService
import nethical.digipaws.services.ViewBlockerService
import nethical.digipaws.utils.SavedPreferencesLoader
import nethical.digipaws.utils.Tools
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var selectPinnedAppsLauncher: ActivityResultLauncher<Intent>

    private lateinit var selectBlockedAppsLauncher: ActivityResultLauncher<Intent>

    private lateinit var selectBlockedKeywords: ActivityResultLauncher<Intent>

    private lateinit var addCheatHoursActivity: ActivityResultLauncher<Intent>

    val savedPreferencesLoader = SavedPreferencesLoader(this)

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

        binding.selectCheatHours.setOnClickListener {
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
    }

    private fun sendRefreshRequest(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
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
                        false
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

        val convertedStartTime = Tools.convertMinutesTo24Hour(startTimeInMins)
        val convertedEndTIme = Tools.convertMinutesTo24Hour(endTimeInMins)

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
                        Tools.convertToMinutesFromMidnight(selectedHour, selectedMinute)

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
                        Tools.convertToMinutesFromMidnight(selectedHour, selectedMinute)
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


    data class WarningData(
        val message: String = "",
        val timeInterval: Int = 5000,
        val isDynamicIntervalSettingAllowed: Boolean = false
    )

}