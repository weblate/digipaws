package nethical.digipaws.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import nethical.digipaws.services.DigipawsMainService
import nethical.digipaws.services.UsageTrackingService.AttentionSpanVideoItem
import nethical.digipaws.ui.activity.AddCheatHoursActivity
import nethical.digipaws.ui.activity.MainActivity

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

    fun saveViewBlockerWarningInfo(warningData: MainActivity.WarningData) {
        val sharedPreferences = context.getSharedPreferences("warning_data", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()

        val json = gson.toJson(warningData)

        editor.putString("view_blocker", json)
        editor.apply()
    }

    fun saveCheatHoursForViewBlocker(startTime: Int, endTime: Int, isProceedDisabled: Boolean) {
        val sharedPreferences = context.getSharedPreferences("cheat_hours", Context.MODE_PRIVATE)
        val edit = sharedPreferences.edit()
        edit.putInt("view_blocker_start_time", startTime)
        edit.putInt("view_blocker_end_time", endTime)
        edit.putBoolean("view_blocker_is_proceed_disabled", isProceedDisabled)
        edit.commit()
    }

    fun loadViewBlockerWarningInfo(): MainActivity.WarningData {
        val sharedPreferences = context.getSharedPreferences("warning_data", Context.MODE_PRIVATE)
        val gson = Gson()

        val json = sharedPreferences.getString("view_blocker", null)

        if (json.isNullOrEmpty()) return MainActivity.WarningData()

        val type = object : TypeToken<MainActivity.WarningData>() {}.type
        return gson.fromJson(json, type)
    }


    fun saveUsageHoursAttentionSpanData(attentionSpanListData: MutableMap<String, MutableList<AttentionSpanVideoItem>>) {
        val sharedPreferences =
            context.getSharedPreferences("attention_span_data", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()

        val json = gson.toJson(attentionSpanListData)

        editor.putString("attention_data", json)
        editor.apply()
    }

    fun loadUsageHoursAttentionSpanData(): MutableMap<String, MutableList<AttentionSpanVideoItem>> {
        val sharedPreferences =
            context.getSharedPreferences("attention_span_data", Context.MODE_PRIVATE)
        val gson = Gson()

        val json = sharedPreferences.getString("attention_data", null)

        if (json.isNullOrEmpty()) return mutableMapOf()

        val type =
            object : TypeToken<MutableMap<String, MutableList<AttentionSpanVideoItem>>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveReelsScrolled(reelsData: MutableMap<String, Int>) {
        val sharedPreferences =
            context.getSharedPreferences("attention_span_data", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()

        val json = gson.toJson(reelsData)

        editor.putString("reels_data", json)
        editor.apply()
    }

    fun getReelsScrolled(): MutableMap<String, Int> {
        val sharedPreferences =
            context.getSharedPreferences("attention_span_data", Context.MODE_PRIVATE)
        val gson = Gson()

        val json = sharedPreferences.getString("reels_data", null)

        if (json.isNullOrEmpty()) return mutableMapOf()

        val type =
            object : TypeToken<MutableMap<String, Int>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveFocusModeData(focusModeData: DigipawsMainService.FocusModeData) {
        val sharedPreferences =
            context.getSharedPreferences("focus_mode", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()

        val json = gson.toJson(focusModeData)

        editor.putString("focus_mode", json)
        editor.apply()
    }


    fun getFocusModeData(): DigipawsMainService.FocusModeData {

        val sharedPreferences =
            context.getSharedPreferences("focus_mode", Context.MODE_PRIVATE)
        val gson = Gson()

        val json = sharedPreferences.getString("focus_mode", null)

        if (json.isNullOrEmpty()) return DigipawsMainService.FocusModeData()

        val type =
            object : TypeToken<DigipawsMainService.FocusModeData>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveFocusModeUnblockedApps(appList: List<String>) {
        val sharedPreferences =
            context.getSharedPreferences("focus_mode", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()

        val json = gson.toJson(appList)

        editor.putString("unblocked_apps", json)
        editor.apply()
    }

    fun getFocusModeUnblockedApps(): List<String> {
        val sharedPreferences =
            context.getSharedPreferences("focus_mode", Context.MODE_PRIVATE)
        val gson = Gson()

        val json = sharedPreferences.getString("unblocked_apps", null)

        if (json.isNullOrEmpty()) return listOf()

        val type =
            object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type)
    }


}