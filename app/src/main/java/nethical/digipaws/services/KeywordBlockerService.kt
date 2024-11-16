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
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import nethical.digipaws.blockers.KeywordBlocker
import nethical.digipaws.utils.SavedPreferencesLoader

class KeywordBlockerService : AccessibilityService() {

    companion object {
        val INTENT_ACTION_REFRESH_BLOCKED_KEYWORD_LIST = "nethical.digipaws.refresh.keywordblocker"
    }

    val savedPreferencesLoader = SavedPreferencesLoader(this)


    private val keywordBlocker = KeywordBlocker()

    private var lastEventActionTakenTimeStamp: Long =
        SystemClock.uptimeMillis() // prevents repetitive global actions


    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isDelayOver()) {
            return
        }

        val rootnode: AccessibilityNodeInfo? = rootInActiveWindow
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            handleKeywordBlockerResult(keywordBlocker.checkIfUserGettingFreaky(rootnode))
        }
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
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.DEFAULT
        }
        serviceInfo = info


        val filter = IntentFilter().apply {
            addAction(INTENT_ACTION_REFRESH_BLOCKED_KEYWORD_LIST)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(refreshReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(refreshReceiver, filter)
        }
    }


    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null && intent.action == INTENT_ACTION_REFRESH_BLOCKED_KEYWORD_LIST) {
                setupBlockers()
            }
        }
    }

    private fun handleKeywordBlockerResult(detectedWord: String?) {
        if (detectedWord == null) return
        Toast.makeText(this, "Blocked keyword $detectedWord was found.", Toast.LENGTH_LONG).show()
        pressHome()
    }


    private fun setupBlockers() {
        keywordBlocker.blockedKeyword = savedPreferencesLoader.loadBlockedKeywords().toHashSet()
    }

    private fun pressHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
        lastEventActionTakenTimeStamp = SystemClock.uptimeMillis()
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