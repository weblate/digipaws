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
import android.view.accessibility.AccessibilityNodeInfo
import nethical.digipaws.blockers.ViewBlocker

class ViewBlockerService : BaseBlockingService() {

    companion object {
        const val INTENT_ACTION_REFRESH_VIEW_BLOCKER = "nethical.digipaws.refresh.viewblocker"
    }

    private val viewBlocker = ViewBlocker()

    private var cooldownInterval = 10 * 60000
    private var warningMessage = ""

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if(!isDelayOver()){
            return
        }
        val rootNode: AccessibilityNodeInfo? = rootInActiveWindow
        if (event?.packageName == "com.instagram.android" && event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            handleViewBlockerResult(rootNode?.let { viewBlocker.doesViewNeedToBeBlocked(it) })
        } else if (event?.packageName == "com.google.android.youtube" && event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            handleViewBlockerResult(rootNode?.let { viewBlocker.doesViewNeedToBeBlocked(it) })
        }
    }

    override fun onInterrupt() {
        TODO("Not yet implemented")
    }


    private fun handleViewBlockerResult(result: ViewBlocker.ViewBlockerResult?) {
        if (result == null || !result.isBlocked) return
        warningOverlayManager.showTextOverlay(
            warningMessage,
            onClose = { pressBack() },
            onProceed = {
                lastEventActionTakenTimeStamp = SystemClock.uptimeMillis()
                var interval = 0
                interval = if (warningOverlayManager.isDynamicCooldownALlowed) {
                    (warningOverlayManager.getSelectedCooldownMins()?.times(60000))
                        ?: cooldownInterval
                } else {
                    cooldownInterval
                }
                viewBlocker.applyCooldown(
                    result.viewId,
                    SystemClock.uptimeMillis() + interval
                )
            },
            isProceedHidden = viewBlocker.isProceedBtnDisabled
        )
    }

    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null && intent.action == INTENT_ACTION_REFRESH_VIEW_BLOCKER) {
                setupBlocker()
            }
        }
    }


    private fun setupBlocker() {
        val warningScreenConfig = savedPreferencesLoader.loadViewBlockerWarningInfo()
        cooldownInterval = warningScreenConfig.timeInterval
        warningMessage = warningScreenConfig.message
        warningOverlayManager.isDynamicCooldownALlowed =
            warningScreenConfig.isDynamicIntervalSettingAllowed
        warningOverlayManager.defaultCooldown = cooldownInterval / 60000


        val viewBlockerCheatHours = getSharedPreferences("cheat_hours", Context.MODE_PRIVATE)
        viewBlocker.cheatMinuteStartTime =
            viewBlockerCheatHours.getInt("view_blocker_start_time", -1)
        viewBlocker.cheatMinutesEndTIme = viewBlockerCheatHours.getInt("view_blocker_end_time", -1)
        viewBlocker.isProceedBtnDisabled =
            viewBlockerCheatHours.getBoolean("view_blocker_is_proceed_disabled", false)

        val addReelData = getSharedPreferences("config_reels", Context.MODE_PRIVATE)
        viewBlocker.isIGInboxReelAllowed = addReelData.getBoolean("is_reel_inbox", false)
        viewBlocker.isFirstReelInFeedAllowed = addReelData.getBoolean("is_reel_first", false)
        Log.d("data", viewBlocker.isFirstReelInFeedAllowed.toString())
    }


    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onServiceConnected() {
        super.onServiceConnected()
        setupBlocker()
        val info = AccessibilityServiceInfo().apply {
            eventTypes =
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or AccessibilityEvent.TYPE_VIEW_SCROLLED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.DEFAULT

        }
        serviceInfo = info


        val filter = IntentFilter().apply {
            addAction(INTENT_ACTION_REFRESH_VIEW_BLOCKER)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(refreshReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(refreshReceiver, filter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(refreshReceiver)
    }


}