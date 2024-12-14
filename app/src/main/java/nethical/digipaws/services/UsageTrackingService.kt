package nethical.digipaws.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.view.accessibility.AccessibilityEvent
import nethical.digipaws.blockers.ViewBlocker
import nethical.digipaws.ui.overlay.UsageStatOverlayManager
import nethical.digipaws.utils.SavedPreferencesLoader
import nethical.digipaws.utils.TimeTools
import java.util.concurrent.TimeUnit

class UsageTrackingService : AccessibilityService() {

    private var screenOnTime: Long = 0
    private var accumulatedTime: Long = 0
    private var isScreenOn = false
    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null

    private val usageStatOverlayManager by lazy { UsageStatOverlayManager(this) }
    private var userYSwipeEventCounter: Long = 0

    private var attentionSpanDataList = mutableMapOf<String, MutableList<AttentionSpanVideoItem>>()
    private var lastVideoViewFoundTime: Long? = null
    private val savedPreferencesLoader = SavedPreferencesLoader(this)
    private var reelCountData = mutableMapOf<String, Int>()

    private var isReelCountToBeDisplayed = true
    private var isTimeElapsedCounterOn = true

    companion object {

        const val INTENT_ACTION_REFRESH_USAGE_TRACKER = "nethical.digipaws.refresh.usage_tracker"
        private const val UPDATE_INTERVAL = 1000L // 1 second
        private const val TAG = "ScreenTimeTracking"
        private const val USER_Y_SWIPE_THRESHOLD: Long = 2
        const val VIDEO_TYPE_REEL = 1
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (!isTimeElapsedCounterOn) return
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> handleScreenOn()
                Intent.ACTION_SCREEN_OFF -> handleScreenOff()
            }
        }
    }

    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                INTENT_ACTION_REFRESH_USAGE_TRACKER -> setupTracker()
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onServiceConnected() {
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_SCROLLED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.DEFAULT
        }

        registerReceiver(screenReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        })


        val filter = IntentFilter().apply {
            addAction(INTENT_ACTION_REFRESH_USAGE_TRACKER)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(refreshReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(refreshReceiver, filter)
        }

        setupTracker()

        attentionSpanDataList = savedPreferencesLoader.loadUsageHoursAttentionSpanData()
        reelCountData = savedPreferencesLoader.getReelsScrolled()
        usageStatOverlayManager.startDisplaying()
    }

    private fun setupTracker(){
        val sp = getSharedPreferences("config_tracker",Context.MODE_PRIVATE)

        isReelCountToBeDisplayed = sp.getBoolean("is_reel_counter",true)
        isTimeElapsedCounterOn = sp.getBoolean("is_time_elapsed",true)

        if (!isTimeElapsedCounterOn) {
            usageStatOverlayManager.binding?.timeElapsedTxt?.visibility = View.GONE
            handleScreenOff()
        }else{
            usageStatOverlayManager.binding?.timeElapsedTxt?.visibility = View.VISIBLE

            // Initialize if the screen is already on
            if ((getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive) {
                handleScreenOn()
            }
        }

    }

    private fun handleScreenOn() {
        isScreenOn = true
        screenOnTime = System.currentTimeMillis()
        startTimeTracking()
        Log.d(TAG, "Screen ON - Continuing from: ${formatElapsedTime(accumulatedTime)}")
    }

    private fun handleScreenOff() {
        isScreenOn = false
        updateAccumulatedTime()
        stopTimeTracking()
        Log.d(TAG, "Screen OFF - Current accumulated: ${formatElapsedTime(accumulatedTime)}")
    }

    private fun startTimeTracking() {
        stopTimeTracking() // Ensure only one tracker runs
        updateRunnable = object : Runnable {
            override fun run() {
                if (isScreenOn) {
                    val currentTime = System.currentTimeMillis()
                    val totalTime = accumulatedTime + (currentTime - screenOnTime)
                    usageStatOverlayManager.binding?.timeElapsedTxt?.text =
                        formatElapsedTime(totalTime)
                    handler.postDelayed(this, UPDATE_INTERVAL)
                }
            }
        }
        handler.post(updateRunnable!!)
    }

    private fun stopTimeTracking() {
        updateRunnable?.let { handler.removeCallbacks(it) }
        updateRunnable = null
    }

    private fun updateAccumulatedTime() {
        if (isScreenOn) {
            accumulatedTime += System.currentTimeMillis() - screenOnTime
            screenOnTime = System.currentTimeMillis()
        }
    }

    private fun formatElapsedTime(milliseconds: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            when {
                event.source?.className == "androidx.viewpager.widget.ViewPager" -> takeReelAction()
                event.packageName == "com.google.android.youtube" &&
                        event.source?.className == "android.support.v7.widget.RecyclerView" -> {
                    val reelView = ViewBlocker.findElementById(
                        rootInActiveWindow,
                        "com.google.android.youtube:id/reel_recycler"
                    )
                    if (reelView != null) takeReelAction() else resetReelTracking()
                }

                else -> resetReelTracking()
            }
        }
    }

    private fun resetReelTracking() {
        usageStatOverlayManager.binding?.reelCounter?.visibility = View.GONE
        lastVideoViewFoundTime = null
    }

    private fun trackAttentionSpan(type: Int = VIDEO_TYPE_REEL) {
        lastVideoViewFoundTime?.let {
            val elapsedTime = (SystemClock.uptimeMillis() - it) / 1000f

            val currentDate = TimeTools.getCurrentDate()
            if (attentionSpanDataList[currentDate] == null) {
                attentionSpanDataList[currentDate] = mutableListOf()
            }

            attentionSpanDataList[currentDate]?.add(
                AttentionSpanVideoItem(
                    elapsedTime,
                    TimeTools.getCurrentTime(),
                    type
                )
            )
            savedPreferencesLoader.saveReelsScrolled(reelCountData)
            savedPreferencesLoader.saveUsageHoursAttentionSpanData(attentionSpanDataList)

        }
        lastVideoViewFoundTime = SystemClock.uptimeMillis()
    }

    private fun takeReelAction() {
        if (++userYSwipeEventCounter > USER_Y_SWIPE_THRESHOLD) {
            userYSwipeEventCounter = 0
            val date = TimeTools.getCurrentDate()
            val newCount = (reelCountData[date] ?: 0) + 1

            reelCountData[date] = newCount
            usageStatOverlayManager.reelsScrolledThisSession = newCount

            if(isReelCountToBeDisplayed){
                usageStatOverlayManager.binding?.reelCounter?.apply {
                    visibility = View.VISIBLE
                    text = newCount.toString()
                }
            }else{
                usageStatOverlayManager.binding?.reelCounter?.visibility = View.GONE
            }

            trackAttentionSpan()
        }
    }

    override fun onInterrupt() {
        stopTimeTracking()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenReceiver)
        stopTimeTracking()
    }

    data class AttentionSpanVideoItem(
        val elapsedTime: Float,
        val time: String,
        val type: Int = VIDEO_TYPE_REEL
    )
}
