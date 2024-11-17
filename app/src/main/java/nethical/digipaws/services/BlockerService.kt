package nethical.digipaws.services

import android.accessibilityservice.AccessibilityService
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
import nethical.digipaws.blockers.AppBlocker
import nethical.digipaws.blockers.ViewBlocker
import nethical.digipaws.utils.SavedPreferencesLoader
import nethical.digipaws.utils.UsageStatOverlayManager
import nethical.digipaws.utils.WarningOverlayManager

class BlockerService : AccessibilityService() {

    companion object {
        val INTENT_ACTION_REFRESH_APP_BLOCKER = "nethical.digipaws.refresh.appblocker"
    }

    private val savedPreferencesLoader = SavedPreferencesLoader(this)
    private val appBlocker = AppBlocker()
    private val viewBlocker = ViewBlocker()

    private var lastEventActionTakenTimeStamp: Long =
        SystemClock.uptimeMillis() // prevents repetitive global actions

    private val userYSwipeSensitivity: Long = 2
    private var userYSwipeEventCounter: Long = 0


    private val usageStatOverlayManager = UsageStatOverlayManager(this)
    private val warningOverlayManager = WarningOverlayManager(this)

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName.toString()

        if(!isDelayOver()){
            return
        }
        val rootnode: AccessibilityNodeInfo? = rootInActiveWindow
        if(event?.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED){
            handleAppBlockerResult(appBlocker.doesAppNeedToBeBlocked(packageName),packageName)
        }
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

    private fun handleAppBlockerResult(isActionRequired: Boolean,packageName: String){
        if(isActionRequired){
            warningOverlayManager.showTextOverlay(
                "App Blocked",
                onClose = { pressHome() },
                onProceed = {
                lastEventActionTakenTimeStamp = SystemClock.uptimeMillis()
                appBlocker.putCooldownTo(packageName,  SystemClock.uptimeMillis() + 60000)
            })
        }
    }

    private fun handleViewBlockerResult(result: String?) {
        if (result == ViewBlocker.RETURN_RESULT_REEL_TAB_IN_COOLDOWN) {
            usageStatOverlayManager.incrementReelScrollCount()
            return
        }
        if (result != null) {
            usageStatOverlayManager.incrementReelScrollCount()
            warningOverlayManager.showTextOverlay(
                "View Blocked",
                onClose = { pressBack() },
                onProceed = {
                lastEventActionTakenTimeStamp = SystemClock.uptimeMillis()
                    viewBlocker.applyCooldown(result, SystemClock.uptimeMillis() + 60000)
            })
        }
    }



    private fun pressHome(){
        performGlobalAction(GLOBAL_ACTION_HOME)
        lastEventActionTakenTimeStamp = SystemClock.uptimeMillis()
    }

    private fun pressBack() {
        performGlobalAction(GLOBAL_ACTION_BACK)
        lastEventActionTakenTimeStamp = SystemClock.uptimeMillis()
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onServiceConnected() {
        super.onServiceConnected()
        setupBlockers()

        usageStatOverlayManager.startDisplaying()
        val info = AccessibilityServiceInfo().apply {
            eventTypes =
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or AccessibilityEvent.TYPE_VIEW_SCROLLED
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



    private fun setupBlockers() {
        appBlocker.blockedAppsList = savedPreferencesLoader.loadBlockedApps().toHashSet()
        appBlocker.refreshCheatMinutesData(savedPreferencesLoader.loadCheatHoursList())

    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(refreshReceiver)
    }

    fun isDelayOver(): Boolean {
        return isDelayOver(2000)
    }

    fun isDelayOver(delay: Int): Boolean {
        val currentTime = SystemClock.uptimeMillis().toFloat()
        return currentTime - lastEventActionTakenTimeStamp > delay
    }

}