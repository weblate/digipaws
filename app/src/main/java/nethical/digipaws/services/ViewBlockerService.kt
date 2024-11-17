package nethical.digipaws.services

import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import nethical.digipaws.blockers.ViewBlocker

class ViewBlockerService : BaseBlockingService() {

    private val viewBlocker = ViewBlocker()

    private val userYSwipeSensitivity: Long = 2
    private var userYSwipeEventCounter: Long = 0

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
                "View Blocked",
                onClose = { pressBack() },
                onProceed = {
                lastEventActionTakenTimeStamp = SystemClock.uptimeMillis()
                    viewBlocker.applyCooldown(result, SystemClock.uptimeMillis() + 60000)
            })
        }
    }


    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onServiceConnected() {
        super.onServiceConnected()

        val info = AccessibilityServiceInfo().apply {
            eventTypes =
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or AccessibilityEvent.TYPE_VIEW_SCROLLED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.DEFAULT

        }
        serviceInfo = info


    }
}