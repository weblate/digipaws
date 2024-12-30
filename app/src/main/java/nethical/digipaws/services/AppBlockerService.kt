package nethical.digipaws.services

import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import nethical.digipaws.Constants
import nethical.digipaws.blockers.AppBlocker
import nethical.digipaws.ui.activity.WarningActivity

class AppBlockerService : BaseBlockingService() {

    companion object {
        const val INTENT_ACTION_REFRESH_APP_BLOCKER = "nethical.digipaws.refresh.appblocker"
        const val INTENT_ACTION_REFRESH_APP_BLOCKER_COOLDOWN =
            "nethical.digipaws.refresh.appblocker.cooldown"
    }

    private val appBlocker = AppBlocker()

    private var cooldownIntervalInMillis = 10 * 60000
    private var warningMessage = ""
    private var isDynamicCooldownALlowed = false
    private var isProceedDisabled = false
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {

        val packageName = event?.packageName.toString()
        if (!isDelayOver(4000)) {
            return
        }
        handleAppBlockerResult(appBlocker.doesAppNeedToBeBlocked(packageName), packageName)
    }

    override fun onInterrupt() {
        TODO("Not yet implemented")
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onServiceConnected() {
        super.onServiceConnected()
        setupBlocker()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.DEFAULT
        }
        serviceInfo = info


        val filter = IntentFilter().apply {
            addAction(INTENT_ACTION_REFRESH_APP_BLOCKER)
            addAction(INTENT_ACTION_REFRESH_APP_BLOCKER_COOLDOWN)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(refreshReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(refreshReceiver, filter)
        }
    }


    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            when (intent.action) {
                INTENT_ACTION_REFRESH_APP_BLOCKER -> setupBlocker()
                INTENT_ACTION_REFRESH_APP_BLOCKER_COOLDOWN -> {
                    val interval = intent.getIntExtra("selected_time", cooldownIntervalInMillis)
                    appBlocker.putCooldownTo(
                        intent.getStringExtra("result_id") ?: "xxxxxxxxxxxxxx",
                        SystemClock.uptimeMillis() + interval
                    )
                }
            }

        }
    }


    private fun handleAppBlockerResult(result: AppBlocker.AppBlockerResult?, packageName: String) {
        Log.d("result", result.toString())
        if (result == null || !result.isBlocked) return

        val dialogIntent = Intent(this, WarningActivity::class.java)
        dialogIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        dialogIntent.putExtra("warning_message", warningMessage)
        dialogIntent.putExtra("mode", Constants.WARNING_SCREEN_MODE_APP_BLOCKER)
        dialogIntent.putExtra("is_dynamic_timing", isDynamicCooldownALlowed)
        dialogIntent.putExtra("result_id", packageName)
        dialogIntent.putExtra("default_cooldown", cooldownIntervalInMillis / 60000)
        dialogIntent.putExtra("is_proceed_disabled", isProceedDisabled)
        startActivity(dialogIntent)

        lastEventActionTakenTimeStamp = SystemClock.uptimeMillis()
    }

    private fun setupBlocker() {
        appBlocker.blockedAppsList = savedPreferencesLoader.loadBlockedApps().toHashSet()
        appBlocker.refreshCheatMinutesData(savedPreferencesLoader.loadAppBlockerCheatHoursList())

        val warningScreenConfig = savedPreferencesLoader.loadAppBlockerWarningInfo()
        cooldownIntervalInMillis = warningScreenConfig.timeInterval
        warningMessage = warningScreenConfig.message
        isDynamicCooldownALlowed =
            warningScreenConfig.isDynamicIntervalSettingAllowed
        isProceedDisabled = warningScreenConfig.isProceedDisabled
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(refreshReceiver)
    }

}