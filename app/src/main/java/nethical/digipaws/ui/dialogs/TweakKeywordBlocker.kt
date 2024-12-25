package nethical.digipaws.ui.dialogs

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import nethical.digipaws.R
import nethical.digipaws.databinding.DialogKeywordBlockerConfigBinding
import nethical.digipaws.services.KeywordBlockerService

class TweakKeywordBlocker : BaseDialog() {

    private lateinit var sharedPreferences: SharedPreferences

    @SuppressLint("ApplySharedPref")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogManageKeywordBlocker = DialogKeywordBlockerConfigBinding.inflate(layoutInflater)

        // Initialize SharedPreferences
        sharedPreferences =
            requireContext().getSharedPreferences("keyword_blocker_configs", Context.MODE_PRIVATE)

        // Load current preferences into dialog
        dialogManageKeywordBlocker.cbSearchTextField.isChecked =
            sharedPreferences.getBoolean("search_all_text_fields", false)
        dialogManageKeywordBlocker.redirectUrl.setText(
            sharedPreferences.getString(
                "redirect_url",
                "https://www.youtube.com/watch?v=x31tDT-4fQw&t=1s"
            )
        )

        // Build and show the dialog
        return MaterialAlertDialogBuilder(requireContext())
            .setView(dialogManageKeywordBlocker.root)
            .setCancelable(false)
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                // Save changes to SharedPreferences
                with(sharedPreferences.edit()) {
                    putBoolean(
                        "search_all_text_fields",
                        dialogManageKeywordBlocker.cbSearchTextField.isChecked
                    )
                    putString(
                        "redirect_url",
                        dialogManageKeywordBlocker.redirectUrl.text.toString()
                    )
                    commit() // Save changes immediately
                }

                // Send broadcast to refresh the KeywordBlockerService
                sendRefreshRequest(KeywordBlockerService.INTENT_ACTION_REFRESH_CONFIG)
            }
            .setNegativeButton(getString(R.string.close)) { _, _ ->
                // Do nothing on cancel
            }
            .create()
    }

}
