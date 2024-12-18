package nethical.digipaws.services

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale

class DigipawsMainService : BaseBlockingService() {
    companion object {
        const val INTENT_ACTION_REFRESH_FOCUS_MODE = "nethical.digipaws.refresh.focus_mode"
        const val INTENT_ACTION_REFRESH_ANTI_UNINSTALL = "nethical.digipaws.refresh.anti_uninstall"

    }

    private var focusModeData = FocusModeData()
    private var blockedAppList: HashSet<String> = hashSetOf()
    private var launcherPackage = "nethical.digipaws"

    private var isAntiUninstallOn = true


    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        super.onAccessibilityEvent(event)
        if (focusModeData.isTurnedOn) {
            if (blockedAppList.contains(event?.packageName) && launcherPackage != event?.packageName) {
                pressHome()
            }

            if (focusModeData.endTime < System.currentTimeMillis()) {
                focusModeData.isTurnedOn = false
                savedPreferencesLoader.saveFocusModeData(focusModeData)
            }
        }

        if (isAntiUninstallOn) {
            Log.d("package name", event?.packageName.toString())
            if (event?.packageName == "com.android.settings") {
                traverseNodesForKeywords(rootInActiveWindow)
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onServiceConnected() {
        super.onServiceConnected()

        val filter = IntentFilter().apply {
            addAction(INTENT_ACTION_REFRESH_FOCUS_MODE)
            addAction(INTENT_ACTION_REFRESH_ANTI_UNINSTALL)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(refreshReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(refreshReceiver, filter)
        }
        setupFocusMode()
        setupAntiUninstall()
    }

    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            if (intent != null) {
                when (intent.action) {
                    INTENT_ACTION_REFRESH_FOCUS_MODE -> setupFocusMode()
                    INTENT_ACTION_REFRESH_ANTI_UNINSTALL -> setupAntiUninstall()
                }
            }
        }
    }

    fun setupFocusMode() {
        blockedAppList = savedPreferencesLoader.getFocusModeBlockedApps().toHashSet()
        getDefaultLauncherPackageName()?.let { launcherPackage = it }
        focusModeData = savedPreferencesLoader.getFocusModeData()
    }

    fun setupAntiUninstall() {
        val info = getSharedPreferences("anti_uninstall", Context.MODE_PRIVATE)
        isAntiUninstallOn = info.getBoolean("is_anti_uninstall_on", false)
    }

    private fun getDefaultLauncherPackageName(): String? {
        val packageManager: PackageManager = packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }

        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName
    }


    private fun traverseNodesForKeywords(
        node: AccessibilityNodeInfo?
    ) {
        if (node == null) {
            return
        }
        if (node.className != null && node.className == "android.widget.TextView") {
            val nodeText = node.text
            if (nodeText != null) {
                val editTextContent = nodeText.toString().lowercase(Locale.getDefault())
                if (editTextContent.lowercase(Locale.getDefault()).contains("digipaws")) {
                    pressHome()
                }
            }
        }

        for (i in 0 until node.childCount) {
            val childNode = node.getChild(i)
            traverseNodesForKeywords(childNode)
        }
    }

    data class FocusModeData(
        var isTurnedOn: Boolean = false,
        val endTime: Long = -1
    )

}