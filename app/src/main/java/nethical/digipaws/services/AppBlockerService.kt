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
import nethical.digipaws.blockers.AppBlocker

class AppBlockerService : BaseBlockingService() {

    companion object {
        const val INTENT_ACTION_REFRESH_APP_BLOCKER = "nethical.digipaws.refresh.appblocker"
    }

    private val appBlocker = AppBlocker()

    private var cooldownInterval = 10 * 60000
    private var warningMessage = ""

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {

        val packageName = event?.packageName.toString()
        if (!isDelayOver()) {
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
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(refreshReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(refreshReceiver, filter)
        }
    }


    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("updated Packs", appBlocker.blockedAppsList.toString())

            if (intent != null && intent.action == INTENT_ACTION_REFRESH_APP_BLOCKER) {
                setupBlocker()
                Log.d("updated Packs", appBlocker.blockedAppsList.toString())
            }
        }
    }


    private fun handleAppBlockerResult(result: AppBlocker.AppBlockerResult?, packageName: String) {
        Log.d("result", result.toString())
        if (result == null || !result.isBlocked) return
        warningOverlayManager.showTextOverlay(
            warningMessage,
            onClose = { pressHome() },
            onProceed = {
                lastEventActionTakenTimeStamp = SystemClock.uptimeMillis()
                var interval = 0
                interval = if (warningOverlayManager.isDynamicCooldownALlowed) {
                    (warningOverlayManager.getSelectedCooldownMins()?.times(60000))
                        ?: cooldownInterval
                } else {
                    cooldownInterval
                }
                appBlocker.putCooldownTo(packageName, SystemClock.uptimeMillis() + interval)
            }, isProceedHidden = result.isProceedHidden
        )
    }

    private fun setupBlocker() {
        appBlocker.blockedAppsList = savedPreferencesLoader.loadBlockedApps().toHashSet()
        appBlocker.refreshCheatMinutesData(savedPreferencesLoader.loadCheatHoursList())

        val warningScreenConfig = savedPreferencesLoader.loadAppBlockerWarningInfo()
        cooldownInterval = warningScreenConfig.timeInterval
        warningMessage = warningScreenConfig.message
        warningOverlayManager.defaultCooldown = cooldownInterval / 60000
        warningOverlayManager.isDynamicCooldownALlowed =
            warningScreenConfig.isDynamicIntervalSettingAllowed
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(refreshReceiver)
    }

}