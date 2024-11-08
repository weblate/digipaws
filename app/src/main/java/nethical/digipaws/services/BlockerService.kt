package nethical.digipaws.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import nethical.digipaws.blockers.AppBlocker
import nethical.digipaws.blockers.ViewBlocker
import nethical.digipaws.utils.OverlayManager

class BlockerService : AccessibilityService() {
    private val appBlocker = AppBlocker()
    private val viewBlocker = ViewBlocker()

    private var lastEventActionTakenTimeStamp: Long = SystemClock.uptimeMillis()
    private val overlayManager = OverlayManager(this)

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName.toString()

        if(!isDelayOver()){
            return
        }
        if(event?.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED){
            handleAppBlockerResult(appBlocker.doesAppNeedToBeBlocked(packageName),packageName)
            handleViewBlockerResult(viewBlocker.doesViewNeedToBeBlocked(rootInActiveWindow))
        }
        if(event?.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED){
            handleViewBlockerResult(viewBlocker.doesViewNeedToBeBlocked(rootInActiveWindow))
        }

    }

    override fun onInterrupt() {
        TODO("Not yet implemented")
    }

    private fun handleAppBlockerResult(isActionRequired: Boolean,packageName: String){
        if(isActionRequired){
            overlayManager.showOverlay("App Blocked", onClose = {pressHome()}, onProceed = {
                lastEventActionTakenTimeStamp = SystemClock.uptimeMillis()
                appBlocker.putCooldownTo(packageName,  SystemClock.uptimeMillis() + 60000)
            })
        }
    }

    private fun handleViewBlockerResult(viewId: String?){
        if(viewId != null){
            overlayManager.showOverlay("View Blocked", onClose = {pressback()}, onProceed = {
                lastEventActionTakenTimeStamp = SystemClock.uptimeMillis()
                viewBlocker.applyCooldown(viewId,  SystemClock.uptimeMillis() + 60000)
            })
        }
    }


    private fun pressHome(){
        performGlobalAction(GLOBAL_ACTION_HOME)
        lastEventActionTakenTimeStamp = SystemClock.uptimeMillis()
    }

    private fun pressback(){
        performGlobalAction(GLOBAL_ACTION_BACK)
        lastEventActionTakenTimeStamp = SystemClock.uptimeMillis()
    }
    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.DEFAULT
        }
        serviceInfo = info
    }


    fun isDelayOver(): Boolean {
        return isDelayOver(500)
    }

    fun isDelayOver( delay: Int): Boolean {
        val currentTime = SystemClock.uptimeMillis().toFloat()
        return currentTime - lastEventActionTakenTimeStamp > delay
    }
}