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
import android.widget.Toast
import androidx.annotation.RequiresApi
import nethical.digipaws.blockers.KeywordBlocker
import nethical.digipaws.data.blockers.KeywordPacks

class KeywordBlockerService : BaseBlockingService() {

    var refreshCooldown = 1000
    companion object {
        const val INTENT_ACTION_REFRESH_BLOCKED_KEYWORD_LIST =
            "nethical.digipaws.refresh.keywordblocker.blockedwords"

        const val INTENT_ACTION_REFRESH_CONFIG =
            "nethical.digipaws.refresh.keywordblocker.config"
    }

    private val keywordBlocker = KeywordBlocker()


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {

        if (!isDelayOver(refreshCooldown) || event == null || event.packageName == "nethical.digipaws") {
            return
        }
        val rootnode: AccessibilityNodeInfo? = rootInActiveWindow
        Log.d("KeywordBlocker", "Searching Keywords")
        handleKeywordBlockerResult(keywordBlocker.checkIfUserGettingFreaky(rootnode, event))

        lastEventActionTakenTimeStamp = SystemClock.uptimeMillis()
    }

    override fun onInterrupt() {
        TODO("Not yet implemented")
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onServiceConnected() {
        super.onServiceConnected()
        setupBlockedWords()
        setupConfig()
        val info = AccessibilityServiceInfo().apply {
            eventTypes =
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or AccessibilityEvent.TYPE_VIEW_SCROLLED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.DEFAULT
        }
        serviceInfo = info


        val filter = IntentFilter().apply {
            addAction(INTENT_ACTION_REFRESH_BLOCKED_KEYWORD_LIST)
            addAction(INTENT_ACTION_REFRESH_CONFIG)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(refreshReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(refreshReceiver, filter)
        }
    }


    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                INTENT_ACTION_REFRESH_BLOCKED_KEYWORD_LIST -> setupBlockedWords()
                INTENT_ACTION_REFRESH_CONFIG -> setupConfig()
            }
        }
    }

    private fun handleKeywordBlockerResult(result: KeywordBlocker.KeywordBlockerResult) {
        if (result.resultDetectWord == null) return
        Toast.makeText(
            this,
            "Blocked keyword ${result.resultDetectWord} was found.",
            Toast.LENGTH_LONG
        ).show()
        if (result.isHomePressRequested) {
            pressHome()
        }
    }


    private fun setupBlockedWords() {
        val keywords = savedPreferencesLoader.loadBlockedKeywords().toMutableSet()
        val sp = getSharedPreferences("keyword_blocker_packs", Context.MODE_PRIVATE)
        val isAdultBlockerOn = sp.getBoolean("adult_blocker", false)
        if (isAdultBlockerOn) {
            keywords.addAll(KeywordPacks.adultKeywords)
        }
        keywordBlocker.blockedKeyword = keywords.toHashSet()
    }

    private fun setupConfig() {
        val sp = getSharedPreferences("keyword_blocker_configs", Context.MODE_PRIVATE)

        keywordBlocker.isSearchAllTextFields = sp.getBoolean("search_all_text_fields", false)
        keywordBlocker.redirectUrl =
            sp.getString("redirect_url", "https://www.youtube.com/watch?v=x31tDT-4fQw&t=1s")
                .toString()

        if (keywordBlocker.isSearchAllTextFields) {
            refreshCooldown = 5000
        }

    }



    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(refreshReceiver)
    }
}