package nethical.digipaws.services

import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import nethical.digipaws.blockers.KeywordBlocker
import nethical.digipaws.data.blockers.KeywordPacks

class KeywordBlockerService : BaseBlockingService() {

    companion object {
        const val INTENT_ACTION_REFRESH_BLOCKED_KEYWORD_LIST =
            "nethical.digipaws.refresh.keywordblocker"
    }

    private val keywordBlocker = KeywordBlocker()


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
        val keywords = savedPreferencesLoader.loadBlockedKeywords().toMutableSet()
        val sp = getSharedPreferences("keyword_blocker_packs", Context.MODE_PRIVATE)
        val isAdultBlockerOn = sp.getBoolean("adult_blocker", false)
        if (isAdultBlockerOn) {
            keywords.addAll(KeywordPacks.adultKeywords)
        }
        keywordBlocker.blockedKeyword = keywords.toHashSet()
    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(refreshReceiver)
    }
}