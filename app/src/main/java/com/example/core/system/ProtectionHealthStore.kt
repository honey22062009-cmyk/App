package com.example.core.system

import android.content.Context

/**
 * Lightweight local heartbeat for the fallback foreground monitor.
 * This is diagnostic state, not proof that Accessibility enforcement is healthy.
 */
object ProtectionHealthStore {
    private const val PREFS_NAME = "protection_health"
    private const val LAST_MONITOR_HEARTBEAT = "last_monitor_heartbeat"

    fun recordMonitorHeartbeat(context: Context, atMillis: Long = System.currentTimeMillis()) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(LAST_MONITOR_HEARTBEAT, atMillis)
            .apply()
    }

    fun getLastMonitorHeartbeat(context: Context): Long? {
        val value = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(LAST_MONITOR_HEARTBEAT, 0L)
        return value.takeIf { it > 0L }
    }
}
