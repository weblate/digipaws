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
        setupBlockers()
        val info = AccessibilityServiceInfo().apply {
            eventTypes =
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
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
                setupBlockers()
                Log.d("updated Packs", appBlocker.blockedAppsList.toString())
            }
        }
    }


    private fun handleAppBlockerResult(result: AppBlocker.AppBlockerResult?, packageName: String) {
        Log.d("result", result.toString())
        if (result == null || !result.isBlocked) return
        warningOverlayManager.showTextOverlay(
            "App Blocked",
            onClose = { pressHome() },
            onProceed = {
                lastEventActionTakenTimeStamp = SystemClock.uptimeMillis()
                appBlocker.putCooldownTo(packageName, SystemClock.uptimeMillis() + 60000)
            }, isProceedHidden = result.isProceedHidden
        )
    }
    private fun setupBlockers() {
        appBlocker.blockedAppsList = savedPreferencesLoader.loadBlockedApps().toHashSet()
        Log.d("cheatData", savedPreferencesLoader.loadCheatHoursList().toString())
        appBlocker.refreshCheatMinutesData(savedPreferencesLoader.loadCheatHoursList())
        Log.d("cheat", appBlocker.cheatMinutes.toString())

    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(refreshReceiver)
    }

}