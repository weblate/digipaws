package nethical.digipaws.blockers

import android.os.SystemClock
import android.util.Log
import nethical.digipaws.ui.activity.AddCheatHoursActivity
import nethical.digipaws.utils.TimeTools
import java.util.Calendar

class AppBlocker:BaseBlocker() {
    private var cooldownAppsList:MutableMap<String,Long> = mutableMapOf()

    var cheatMinutes: MutableMap<String, List<Pair<Int, Int>>> = mutableMapOf()

    var blockedAppsList = hashSetOf("")

    var proceedBtnInfo: MutableMap<String, Boolean> =
        mutableMapOf() // stores info about proceed button visibility


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
                isBlocked = true,
                isProceedHidden = proceedBtnInfo.getOrDefault(packageName, defaultValue = false)
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
            if (currentMinutes in startMinutes until endMinutes) {
                return true
            }
        }

        return false
    }

    fun refreshCheatMinutesData(cheatList: List<AddCheatHoursActivity.CheatHourItem>) {
        cheatMinutes.clear()
        proceedBtnInfo.clear()
        cheatList.forEach { item ->
            val startTime = item.startTime
            val endTime = item.endTime
            val packageNames: ArrayList<String> = item.packages

            packageNames.forEach { packageName ->
                Log.d("cheat adding for", packageName)

                proceedBtnInfo[packageName] = item.isProceedHidden
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
        val isBlocked: Boolean = false,
        val isProceedHidden: Boolean = false
    )

}