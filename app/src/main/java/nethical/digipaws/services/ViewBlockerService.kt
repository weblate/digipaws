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

    private val userYSwipeSensitivity: Long = 2
    private var userYSwipeEventCounter: Long = 0


    private var cooldownInterval = 10 * 60000
    private var warningMessage = ""

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if(!isDelayOver()){
            return
        }
        val rootnode: AccessibilityNodeInfo? = rootInActiveWindow
        if(event?.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED){
            if (event.source?.className?.equals("androidx.viewpager.widget.ViewPager") == true) {
                userYSwipeEventCounter++
                if (userYSwipeEventCounter > userYSwipeSensitivity) {
                    Log.d("source", event.source?.className.toString())
                    userYSwipeEventCounter = 0
                    handleViewBlockerResult(rootnode?.let { viewBlocker.doesViewNeedToBeBlocked(it) })
                }
            }
        }
    }

    override fun onInterrupt() {
        TODO("Not yet implemented")
    }


    private fun handleViewBlockerResult(result: String?) {
        if (result != null) {
            warningOverlayManager.showTextOverlay(
                warningMessage,
                onClose = { pressBack() },
                onProceed = {
                lastEventActionTakenTimeStamp = SystemClock.uptimeMillis()
                    viewBlocker.applyCooldown(result, SystemClock.uptimeMillis() + cooldownInterval)
                },
                isProceedHidden = viewBlocker.isProceedBtnDisabled
            )
        }
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

        val viewBlockerCheatHours = getSharedPreferences("cheat_hours", Context.MODE_PRIVATE)
        viewBlocker.cheatMinuteStartTime =
            viewBlockerCheatHours.getInt("view_blocker_start_time", -1)
        viewBlocker.cheatMinutesEndTIme = viewBlockerCheatHours.getInt("view_blocker_end_time", -1)
        viewBlocker.isProceedBtnDisabled =
            viewBlockerCheatHours.getBoolean("view_blocker_is_proceed_disabled", false)
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

}