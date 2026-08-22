package com.elytelabs.inappflow

import android.content.Context
import android.content.SharedPreferences
import java.util.Date

/**
 * SharedPreferences storage for tracking app installation date,
 * launch count, remind intervals, and opt-out preferences.
 */
object ReviewPrefs {

    private const val PREFS_FILE_NAME = "android_rate_prefs"
    private const val PREFS_INSTALL_DATE = "app_install_date"
    private const val PREFS_LAUNCH_COUNT = "app_launch_count"
    private const val PREFS_REMIND_INTERVAL = "app_rating_remind_interval"
    private const val PREFS_OPT_OUT = "app_rating_opt_out"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)
    }

    private fun getPreferencesEditor(context: Context): SharedPreferences.Editor {
        return getPreferences(context).edit()
    }

    fun setRemindIntervalDate(context: Context) {
        val editor = getPreferencesEditor(context)
        editor.putLong(PREFS_REMIND_INTERVAL, Date().time)
        editor.apply()
    }

    fun getRemindInterval(context: Context): Long {
        return getPreferences(context).getLong(PREFS_REMIND_INTERVAL, 0)
    }

    fun setInstallDate(context: Context) {
        val editor = getPreferencesEditor(context)
        editor.putLong(PREFS_INSTALL_DATE, Date().time)
        editor.apply()
    }

    fun getInstallDate(context: Context): Long {
        return getPreferences(context).getLong(PREFS_INSTALL_DATE, 0)
    }

    fun setLaunchCount(context: Context, launchTimes: Int) {
        val editor = getPreferencesEditor(context)
        editor.putInt(PREFS_LAUNCH_COUNT, launchTimes)
        editor.apply()
    }

    fun getLaunchCount(context: Context): Int {
        return getPreferences(context).getInt(PREFS_LAUNCH_COUNT, 0)
    }

    fun isFirstLaunch(context: Context): Boolean {
        return getPreferences(context).getLong(PREFS_INSTALL_DATE, 0) == 0L
    }

    fun setOptOut(context: Context, optOut: Boolean) {
        val editor = getPreferencesEditor(context)
        editor.putBoolean(PREFS_OPT_OUT, optOut)
        editor.apply()
    }

    fun isOptedOut(context: Context): Boolean {
        return getPreferences(context).getBoolean(PREFS_OPT_OUT, false)
    }

    /**
     * Resets all tracking preferences back to default values.
     */
    fun reset(context: Context) {
        val editor = getPreferencesEditor(context)
        editor.clear()
        editor.apply()
    }
}
