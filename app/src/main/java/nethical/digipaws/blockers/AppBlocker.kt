package nethical.digipaws.blockers

import android.os.SystemClock
import android.util.Log
import nethical.digipaws.ui.activity.TimedActionActivity
import nethical.digipaws.utils.TimeTools
import java.util.Calendar

class AppBlocker:BaseBlocker() {
    private var cooldownAppsList:MutableMap<String,Long> = mutableMapOf()

    var cheatMinutes: MutableMap<String, List<Pair<Int, Int>>> = mutableMapOf()

    var blockedAppsList = hashSetOf("")


    fun doesAppNeedToBeBlocked(packageName: String): AppBlockerResult? {

        if(cooldownAppsList.containsKey(packageName)){
            if (cooldownAppsList[packageName]!! < SystemClock.uptimeMillis()){
                removeCooldownFrom(packageName)
            }
            return null
        }

        if (isWithinUsageMinutes(packageName)) {
            return AppBlockerResult(isBlocked = false)
        }

        if (blockedAppsList.contains(packageName)) {
            return AppBlockerResult(
                isBlocked = true
            )
        }
        return null
    }
    fun putCooldownTo(packageName: String, endTime: Long) {
        cooldownAppsList.put(packageName,endTime)
        Log.d("cooldownAppsList",cooldownAppsList.toString())
    }
    private fun removeCooldownFrom(packageName: String){
        cooldownAppsList.remove(packageName)
    }

    private fun isWithinUsageMinutes(packageName: String): Boolean {
        val currentTime = Calendar.getInstance()
        val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
        val currentMinute = currentTime.get(Calendar.MINUTE)

        val currentMinutes = TimeTools.convertToMinutesFromMidnight(currentHour, currentMinute)
        cheatMinutes[packageName]?.forEach { (startMinutes, endMinutes) ->
            if ((startMinutes <= endMinutes && currentMinutes in startMinutes until endMinutes) || (startMinutes > endMinutes && (currentMinutes >= startMinutes || currentMinutes < endMinutes))) {
                return true
            }

        }
        return false
    }
    fun refreshCheatMinutesData(cheatList: List<TimedActionActivity.AutoTimedActionItem>) {
        cheatMinutes.clear()
        cheatList.forEach { item ->
            val startTime = item.startTimeInMins
            val endTime = item.endTimeInMins
            val packageNames: ArrayList<String> = item.packages

            packageNames.forEach { packageName ->
                Log.d("cheat adding for", packageName)

                if (cheatMinutes.containsKey(packageName)) {
                    val cheatHourTimeData: List<Pair<Int, Int>>? = cheatMinutes[packageName]
                    val cheatHourNewTimeData: MutableList<Pair<Int, Int>> =
                        cheatHourTimeData!!.toMutableList()

                    cheatHourNewTimeData.add(Pair(startTime, endTime))
                    cheatMinutes[packageName] = cheatHourNewTimeData
                } else {
                    cheatMinutes[packageName] = listOf(Pair(startTime, endTime))
                }
            }
        }

    }

    data class AppBlockerResult(
        val isBlocked: Boolean = false
    )

}