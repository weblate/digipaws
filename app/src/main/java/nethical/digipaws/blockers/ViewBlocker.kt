package nethical.digipaws.blockers

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

class ViewBlocker : BaseBlocker() {
    private val cooldownViewIdsList = mutableMapOf<String, Long>()
    private val blockedViewIdsList = mutableListOf("com.instagram.android:id/root_clips_layout")

    fun doesViewNeedToBeBlocked(node: AccessibilityNodeInfo): String? {
        blockedViewIdsList.forEach { viewId ->
            if (isCooldownActive(viewId)) return@forEach
            if(isViewOpened(node,viewId)){
                Log.d("ViewBlocker", "Blocking view ID: $viewId")
                return viewId
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
        return viewNode != null
    }

    private fun findElementById(node: AccessibilityNodeInfo?, id: String?): AccessibilityNodeInfo? {
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
