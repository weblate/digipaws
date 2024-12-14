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
import nethical.digipaws.Constants
import nethical.digipaws.blockers.ViewBlocker
import nethical.digipaws.ui.activity.WarningActivity
import nethical.digipaws.utils.WarningNotification

class ViewBlockerService : BaseBlockingService() {

    companion object {
        const val INTENT_ACTION_REFRESH_VIEW_BLOCKER = "nethical.digipaws.refresh.viewblocker"
        const val INTENT_ACTION_VIEW_BLOCKER_COOLDOWN =
            "nethical.digipaws.refresh.viewblocker.cooldown"
    }

    private val viewBlocker = ViewBlocker()

    private var cooldownIntervalInMillis = 10 * 60000
    private var warningMessage = ""
    private var isDynamicCooldownAllowed = false
    private var isProceedBtnDisabled = false

    private lateinit var warningNotification: WarningNotification

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if(!isDelayOver()){
            return
        }
        val rootNode: AccessibilityNodeInfo? = rootInActiveWindow

        handleViewBlockerResult(rootNode?.let { viewBlocker.doesViewNeedToBeBlocked(it) })
    }

    override fun onInterrupt() {
        TODO("Not yet implemented")
    }


    private fun handleViewBlockerResult(result: ViewBlocker.ViewBlockerResult?) {
        if (result == null || !result.isBlocked) return
        pressBack()

        val dialogIntent = Intent(this, WarningActivity::class.java)
        dialogIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        dialogIntent.putExtra("warning_message", warningMessage)
        dialogIntent.putExtra("mode", Constants.VIEW_BLOCKER_WARNING_MODE)
        dialogIntent.putExtra("is_dynamic_timing", isDynamicCooldownAllowed)
        dialogIntent.putExtra("result_id", result.viewId)
        dialogIntent.putExtra("default_cooldown", cooldownIntervalInMillis / 60000)
        dialogIntent.putExtra("is_proceed_disabled", isProceedBtnDisabled)
        startActivity(dialogIntent)

        warningNotification.sendNotification("Blocked", "Reel Tab was blocked")
    }

    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            when (intent.action) {
                INTENT_ACTION_REFRESH_VIEW_BLOCKER -> setupBlocker()

                INTENT_ACTION_VIEW_BLOCKER_COOLDOWN -> {
                    lastEventActionTakenTimeStamp = SystemClock.uptimeMillis()
                    val interval = if (warningOverlayManager.isDynamicCooldownALlowed) {
                        intent.getIntExtra("selected_time", cooldownIntervalInMillis)
                    } else {
                        cooldownIntervalInMillis
                    }
                    viewBlocker.applyCooldown(
                        intent.getStringExtra("result_id") ?: "xxxxxxxxxxxxxx",
                        SystemClock.uptimeMillis() + interval
                    )
                }
            }
        }
    }


    private fun setupBlocker() {
        val warningScreenConfig = savedPreferencesLoader.loadViewBlockerWarningInfo()
        cooldownIntervalInMillis = warningScreenConfig.timeInterval
        warningMessage = warningScreenConfig.message
        isDynamicCooldownAllowed =
            warningScreenConfig.isDynamicIntervalSettingAllowed


        val viewBlockerCheatHours = getSharedPreferences("cheat_hours", Context.MODE_PRIVATE)
        isProceedBtnDisabled =
            viewBlockerCheatHours.getBoolean("view_blocker_is_proceed_disabled", false)
        viewBlocker.cheatMinuteStartTime =
            viewBlockerCheatHours.getInt("view_blocker_start_time", -1)
        viewBlocker.cheatMinutesEndTIme = viewBlockerCheatHours.getInt("view_blocker_end_time", -1)

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
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.DEFAULT

        }
        serviceInfo = info
        warningNotification = WarningNotification(this)

        val filter = IntentFilter().apply {
            addAction(INTENT_ACTION_REFRESH_VIEW_BLOCKER)
            addAction(INTENT_ACTION_VIEW_BLOCKER_COOLDOWN)
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