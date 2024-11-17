package nethical.digipaws.services

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import nethical.digipaws.utils.SavedPreferencesLoader
import nethical.digipaws.utils.WarningOverlayManager

open class BaseBlockingService : AccessibilityService() {
    val savedPreferencesLoader: SavedPreferencesLoader by lazy {
        SavedPreferencesLoader(this)
    }

    val warningOverlayManager: WarningOverlayManager by lazy {
        WarningOverlayManager(this)
    }

    var lastEventActionTakenTimeStamp: Long =
        SystemClock.uptimeMillis() // prevents repetitive global actions

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    }

    override fun onInterrupt() {
    }

    fun isDelayOver(): Boolean {
        return isDelayOver(500)
    }

    fun isDelayOver(delay: Int): Boolean {
        val currentTime = SystemClock.uptimeMillis().toFloat()
        return currentTime - lastEventActionTakenTimeStamp > delay
    }

    fun pressHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
        lastEventActionTakenTimeStamp = SystemClock.uptimeMillis()
    }

    fun pressBack() {
        performGlobalAction(GLOBAL_ACTION_BACK)
        lastEventActionTakenTimeStamp = SystemClock.uptimeMillis()
    }
}