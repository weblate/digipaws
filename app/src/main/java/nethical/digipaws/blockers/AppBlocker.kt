package nethical.digipaws.blockers

import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlin.math.log

class AppBlocker:BaseBlocker() {
    private var cooldownAppsList:MutableMap<String,Long> = mutableMapOf()
    private val blockedAppsList = hashSetOf(
        "com.instagram.android"
    )

    fun doesAppNeedToBeBlocked(packageName: String): Boolean{

        if(cooldownAppsList.containsKey(packageName)){
            if (cooldownAppsList[packageName]!! < SystemClock.uptimeMillis()){
                Log.d("checkResult for $packageName", cooldownAppsList[packageName].toString() + " < " + SystemClock.uptimeMillis())
                removeCooldownFrom(packageName)
            }
                return false
        }
        return blockedAppsList.contains(packageName)
    }
    fun putCooldownTo(packageName: String, endTime: Long) {
        cooldownAppsList.put(packageName,endTime)
        Log.d("cooldownAppsList",cooldownAppsList.toString())
    }
    private fun removeCooldownFrom(packageName: String){
        cooldownAppsList.remove(packageName)
    }
}