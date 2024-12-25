package nethical.digipaws.ui.dialogs

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import nethical.digipaws.R
import nethical.digipaws.databinding.DialogTweakBlockerWarningBinding
import nethical.digipaws.services.ViewBlockerService
import nethical.digipaws.ui.activity.MainActivity
import nethical.digipaws.utils.SavedPreferencesLoader

class TweakViewBlockerWarning(
    savedPreferencesLoader: SavedPreferencesLoader
) : BaseDialog(savedPreferencesLoader) {

    private lateinit var addReelData: SharedPreferences

    @SuppressLint("ApplySharedPref")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val tweakViewBlockerWarningBinding =
            DialogTweakBlockerWarningBinding.inflate(layoutInflater)

        // Configure NumberPicker
        tweakViewBlockerWarningBinding.selectMins.minValue = 1
        tweakViewBlockerWarningBinding.selectMins.maxValue = 240

        // Show additional checkbox options
        tweakViewBlockerWarningBinding.cbFirstReel.visibility = View.VISIBLE
        tweakViewBlockerWarningBinding.cbReelInbox.visibility = View.VISIBLE

        // Load saved preferences
        val previousData = savedPreferencesLoader!!.loadViewBlockerWarningInfo()
        tweakViewBlockerWarningBinding.selectMins.setValue(previousData.timeInterval / 60000)
        tweakViewBlockerWarningBinding.warningMsgEdit.setText(previousData.message)
        tweakViewBlockerWarningBinding.cbDynamicWarning.isChecked =
            previousData.isDynamicIntervalSettingAllowed
        tweakViewBlockerWarningBinding.cbProceedBtn.isChecked = previousData.isProceedDisabled

        // Load additional Reel data
        addReelData = requireContext().getSharedPreferences("config_reels", Context.MODE_PRIVATE)
        tweakViewBlockerWarningBinding.cbReelInbox.isChecked =
            addReelData.getBoolean("is_reel_inbox", false)
        tweakViewBlockerWarningBinding.cbFirstReel.isChecked =
            addReelData.getBoolean("is_reel_first", false)

        // Build and show the dialog
        return MaterialAlertDialogBuilder(requireContext())
            .setView(tweakViewBlockerWarningBinding.root)
            .setCancelable(false)
            .setPositiveButton(getString(R.string.save)) { dialog, _ ->
                val selectedMinInMs = tweakViewBlockerWarningBinding.selectMins.getValue() * 60000

                // Save data using SavedPreferencesLoader
                savedPreferencesLoader.saveViewBlockerWarningInfo(
                    MainActivity.WarningData(
                        tweakViewBlockerWarningBinding.warningMsgEdit.text.toString(),
                        selectedMinInMs,
                        tweakViewBlockerWarningBinding.cbDynamicWarning.isChecked,
                        tweakViewBlockerWarningBinding.cbProceedBtn.isChecked
                    )
                )

                // Save Reel data to SharedPreferences
                with(addReelData.edit()) {
                    putBoolean(
                        "is_reel_inbox",
                        tweakViewBlockerWarningBinding.cbReelInbox.isChecked
                    )
                    putBoolean(
                        "is_reel_first",
                        tweakViewBlockerWarningBinding.cbFirstReel.isChecked
                    )
                    commit() // Apply changes immediately
                }

                // Send broadcast to refresh ViewBlockerService
                sendRefreshRequest(ViewBlockerService.INTENT_ACTION_REFRESH_VIEW_BLOCKER)
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
    }

}
