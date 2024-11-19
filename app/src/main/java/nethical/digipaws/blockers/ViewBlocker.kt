package nethical.digipaws.blockers

import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import nethical.digipaws.utils.Tools
import java.util.Calendar

class ViewBlocker : BaseBlocker() {
    private val cooldownViewIdsList = mutableMapOf<String, Long>()
    private val blockedViewIdsList = mutableListOf(
        "com.instagram.android:id/root_clips_layout",
        "com.google.android.youtube:id/reel_recycler",
        "app.revanced.android.youtube:id/reel_recycler"
    )

    var isProceedBtnDisabled = false

    var isIGInboxReelAllowed = false
    var isFirstReelInFeedAllowed = false

    var typeEventScrollCounter: Int =
        0 //required for ignoring a few scroll events to allow viewing the first reel. resets to 0 after 3 scroll events

    var cheatMinuteStartTime: Int? = null
    var cheatMinutesEndTIme: Int? = null

    fun doesViewNeedToBeBlocked(node: AccessibilityNodeInfo): ViewBlockerResult? {
        if (isCheatHourActive()) {
            return null
        }

        if (isIGInboxReelAllowed && isViewOpened(
                node,
                "com.instagram.android:id/reply_bar_container"
            )
        ) {
            return null
        }



        blockedViewIdsList.forEach { viewId ->
            if(isViewOpened(node,viewId)){
                if (isCooldownActive(viewId)) {
                    return ViewBlockerResult(isReelFoundInCooldownState = true, viewId = viewId)
                }
                return ViewBlockerResult(isBlocked = true, viewId = viewId)
            }
        }
        return null
    }

    fun applyCooldown(viewId: String, endTime: Long) {
        cooldownViewIdsList[viewId] = endTime
    }

    private fun isCooldownActive(viewId: String): Boolean {
        val cooldownEnd = cooldownViewIdsList[viewId] ?: return false
        if (SystemClock.uptimeMillis() > cooldownEnd) {
            cooldownViewIdsList.remove(viewId)
            return false
        }
        return true
    }

    private fun isViewOpened(rootNode: AccessibilityNodeInfo, viewId: String): Boolean {
        val viewNode =
            findElementById(rootNode, viewId)
        // view found
        Log.d("viewfound", viewId)
        return viewNode != null
    }

    private fun isCheatHourActive(): Boolean {

        val currentTime = Calendar.getInstance()
        val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
        val currentMinute = currentTime.get(Calendar.MINUTE)

        val currentMinutes = Tools.convertToMinutesFromMidnight(currentHour, currentMinute)

        // If cheat hours are not set, treat as inactive
        if (cheatMinuteStartTime == null || cheatMinutesEndTIme == null || cheatMinuteStartTime == -1 || cheatMinutesEndTIme == -1) {
            return false
        }


        return currentMinutes in cheatMinuteStartTime!!..cheatMinutesEndTIme!!
    }
    data class ViewBlockerResult(
        val isBlocked: Boolean = false,
        val isReelFoundInCooldownState: Boolean = false,
        val viewId: String = ""
    )

    companion object {

        fun findElementById(node: AccessibilityNodeInfo?, id: String?): AccessibilityNodeInfo? {
            if (node == null) return null
            var targetNode: AccessibilityNodeInfo? = null
            try {
                targetNode = node.findAccessibilityNodeInfosByViewId(id!!)[0]
            } catch (e: Exception) {
                //	e.printStackTrace();
            }
            return targetNode
        }
    }

}
