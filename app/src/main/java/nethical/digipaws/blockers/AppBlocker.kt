package nethical.digipaws.blockers

import android.os.SystemClock
import android.util.Log
import nethical.digipaws.AddCheatHoursActivity
import nethical.digipaws.utils.Tools
import java.util.Calendar

class AppBlocker:BaseBlocker() {
    private var cooldownAppsList:MutableMap<String,Long> = mutableMapOf()

    var cheatMinutes: MutableMap<String, List<Pair<Int, Int>>> = mutableMapOf()

    var blockedAppsList = hashSetOf("")

    fun doesAppNeedToBeBlocked(packageName: String): Boolean{

        if(cooldownAppsList.containsKey(packageName)){
            if (cooldownAppsList[packageName]!! < SystemClock.uptimeMillis()){
                removeCooldownFrom(packageName)
            }
                return false
        }

        if (isWithinUsageMinutes(packageName)) {
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

    private fun isWithinUsageMinutes(packageName: String): Boolean {
        val currentTime = Calendar.getInstance()
        val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
        val currentMinute = currentTime.get(Calendar.MINUTE)

        val currentMinutes = Tools.convertToMinutesFromMidnight(currentHour, currentMinute)
        cheatMinutes[packageName]?.forEach { (startMinutes, endMinutes) ->
            if (currentMinutes in startMinutes until endMinutes) {
                return true
            }
        }

        return false
    }

    fun refreshCheatMinutesData(cheatList: List<AddCheatHoursActivity.CheatHourItem>) {
        cheatList.forEach { item ->
            val startTime = item.startTime
            val endTime = item.endTime
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

}