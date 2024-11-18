package nethical.digipaws.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import nethical.digipaws.AddCheatHoursActivity
import nethical.digipaws.MainActivity

class SavedPreferencesLoader(private val context: Context) {

    fun loadPinnedApps(): Set<String> {
        val sharedPreferences =
            context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        return sharedPreferences.getStringSet("pinned_apps", emptySet()) ?: emptySet()
    }


    fun loadBlockedApps(): Set<String> {
        val sharedPreferences =
            context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        return sharedPreferences.getStringSet("blocked_apps", emptySet()) ?: emptySet()
    }

    fun loadBlockedKeywords(): Set<String> {
        val sharedPreferences =
            context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        return sharedPreferences.getStringSet("blocked_keywords", emptySet()) ?: emptySet()
    }

    fun savePinned(pinnedApps: Set<String>) {
        val sharedPreferences =
            context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        sharedPreferences.edit().putStringSet("pinned_apps", pinnedApps).apply()
    }


    fun saveBlockedApps(pinnedApps: Set<String>) {
        val sharedPreferences =
            context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        sharedPreferences.edit().putStringSet("blocked_apps", pinnedApps).apply()
    }


    fun saveBlockedKeywords(pinnedApps: Set<String>) {
        val sharedPreferences =
            context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        sharedPreferences.edit().putStringSet("blocked_keywords", pinnedApps).apply()
    }
    fun saveCheatHoursList(cheatHoursList: MutableList<AddCheatHoursActivity.CheatHourItem>) {
        val sharedPreferences = context.getSharedPreferences("cheat_hours", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()

        val json = gson.toJson(cheatHoursList)

        editor.putString("cheatHoursList", json)
        editor.apply()
    }

    fun loadCheatHoursList(): MutableList<AddCheatHoursActivity.CheatHourItem> {
        val sharedPreferences = context.getSharedPreferences("cheat_hours", Context.MODE_PRIVATE)
        val gson = Gson()

        val json = sharedPreferences.getString("cheatHoursList", null)

        if (json.isNullOrEmpty()) return mutableListOf()

        val type = object : TypeToken<MutableList<AddCheatHoursActivity.CheatHourItem>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveAppBlockerWarningInfo(warningData: MainActivity.WarningData) {
        val sharedPreferences = context.getSharedPreferences("warning_data", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()

        val json = gson.toJson(warningData)

        editor.putString("app_blocker", json)
        editor.apply()
    }

    fun loadAppBlockerWarningInfo(): MainActivity.WarningData {
        val sharedPreferences = context.getSharedPreferences("warning_data", Context.MODE_PRIVATE)
        val gson = Gson()

        val json = sharedPreferences.getString("app_blocker", null)

        if (json.isNullOrEmpty()) return MainActivity.WarningData()

        val type = object : TypeToken<MainActivity.WarningData>() {}.type
        return gson.fromJson(json, type)
    }

}