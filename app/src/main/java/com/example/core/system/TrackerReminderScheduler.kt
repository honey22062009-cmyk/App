package com.example.core.system

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.preferences.SettingsPreferences
import com.example.receiver.TrackerReminderReceiver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.ZonedDateTime

object TrackerReminderScheduler {
    const val ACTION_DAILY_CHECK_IN = "com.example.strictblocker.DAILY_CHECK_IN_REMINDER"
    const val ACTION_SLEEP = "com.example.strictblocker.SLEEP_REMINDER"
    const val ACTION_TEST = "com.example.strictblocker.TEST_REMINDER"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    fun refresh(context: Context): Job = scope.launch { refreshNow(context.applicationContext) }

    suspend fun refreshNow(context: Context) = mutex.withLock {
        val settings = SettingsPreferences(context)
        scheduleOne(context, ACTION_DAILY_CHECK_IN, 6101, settings.dailyReminderEnabledFlow.first(),
            nextOccurrence(settings.dailyReminderHourFlow.first(), settings.dailyReminderMinuteFlow.first()).toInstant().toEpochMilli())
        scheduleOne(context, ACTION_SLEEP, 6102, settings.sleepReminderEnabledFlow.first(),
            nextOccurrence(settings.sleepReminderHourFlow.first(), settings.sleepReminderMinuteFlow.first()).toInstant().toEpochMilli())
    }

    fun nextOccurrence(hour: Int, minute: Int, now: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime {
        var next = now.withHour(hour.coerceIn(0, 23)).withMinute(minute.coerceIn(0, 59)).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return next
    }

    /** Uses the same AlarmManager/receiver/channel path as the daily reminder. */
    fun scheduleTest(context: Context): Boolean {
        if (!PermissionHelper.canScheduleExactAlarms(context)) return false
        return scheduleOne(context, ACTION_TEST, 6103, true, System.currentTimeMillis() + 60_000L)
    }

    private fun scheduleOne(context: Context, action: String, code: Int, enabled: Boolean, triggerAt: Long): Boolean {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        val pending = PendingIntent.getBroadcast(context, code,
            Intent(context, TrackerReminderReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if (!enabled) { manager.cancel(pending); return true }
        return try {
            if (Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms()) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
            true
        } catch (_: SecurityException) {
            // Permission may be revoked between the capability check and scheduling.
            runCatching { manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending) }.isSuccess
        }
    }
}
