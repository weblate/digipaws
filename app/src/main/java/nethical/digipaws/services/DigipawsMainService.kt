package nethical.digipaws.services

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.view.accessibility.AccessibilityEvent

class DigipawsMainService : BaseBlockingService() {
    companion object {
        const val INTENT_ACTION_REFRESH_FOCUS_MODE = "nethical.digipaws.refresh.focus_mode"
    }

    private var focusModeData = FocusModeData()
    private var allowedAppList: HashSet<String> = hashSetOf()

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        super.onAccessibilityEvent(event)
        if (focusModeData.isTurnedOn) {
            if (!allowedAppList.contains(event?.packageName)) {
                pressHome()
            }

            if (focusModeData.endTime < System.currentTimeMillis()) {
                focusModeData.isTurnedOn = false
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onServiceConnected() {
        super.onServiceConnected()

        val filter = IntentFilter().apply {
            addAction(INTENT_ACTION_REFRESH_FOCUS_MODE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(refreshReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(refreshReceiver, filter)
        }
        setupFocusMode()

    }

    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            if (intent != null && intent.action == INTENT_ACTION_REFRESH_FOCUS_MODE) {
                setupFocusMode()

            }
        }
    }

    fun setupFocusMode() {
        allowedAppList = savedPreferencesLoader.getFocusModeUnblockedApps().toHashSet()
        allowedAppList.add(packageName)
        getDefaultLauncherPackageName()?.let { allowedAppList.add(it) }
        focusModeData = savedPreferencesLoader.getFocusModeData()
    }

    private fun getDefaultLauncherPackageName(): String? {
        val packageManager: PackageManager = packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }

        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName
    }

    data class FocusModeData(
        var isTurnedOn: Boolean = false,
        val endTime: Long = -1
    )

}