package nethical.digipaws.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import nethical.digipaws.blockers.KeywordBlocker
import nethical.digipaws.utils.SavedPreferencesLoader

class KeywordBlockerService : AccessibilityService() {

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
    }


    private fun handleKeywordBlockerResult(detectedWord: String?) {
        if (detectedWord == null) return
        Toast.makeText(this, "Blocked keyword $detectedWord was found.", Toast.LENGTH_LONG).show()
        pressHome()
    }


    private fun setupBlockers() {
        val savedPreferencesLoader = SavedPreferencesLoader(this)
        keywordBlocker.adultKeyword = savedPreferencesLoader.loadBlockedKeywords().toHashSet()
    }

    private fun pressHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
        lastEventActionTakenTimeStamp = SystemClock.uptimeMillis()
    }

    fun isDelayOver(): Boolean {
        return isDelayOver(2000)
    }

    fun isDelayOver(delay: Int): Boolean {
        val currentTime = SystemClock.uptimeMillis().toFloat()
        return currentTime - lastEventActionTakenTimeStamp > delay
    }

}