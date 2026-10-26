package com.example.core.system

import android.app.AlarmManager
import android.app.PendingIntent
import android.os.Build
import android.content.Context
import android.content.Intent
import com.example.data.preferences.SettingsPreferences
import com.example.receiver.TrackerReminderReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

object TrackerReminderScheduler {
    const val ACTION_DAILY_CHECK_IN = "com.example.strictblocker.DAILY_CHECK_IN_REMINDER"
    const val ACTION_SLEEP = "com.example.strictblocker.SLEEP_REMINDER"

    private const val REQUEST_DAILY = 6101
    private const val REQUEST_SLEEP = 6102

    fun refresh(context: Context) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            val settings = SettingsPreferences(appContext)
            scheduleOne(
                context = appContext,
                action = ACTION_DAILY_CHECK_IN,
                requestCode = REQUEST_DAILY,
                enabled = settings.dailyReminderEnabledFlow.first(),
                hour = settings.dailyReminderHourFlow.first(),
                minute = settings.dailyReminderMinuteFlow.first()
            )
            scheduleOne(
                context = appContext,
                action = ACTION_SLEEP,
                requestCode = REQUEST_SLEEP,
                enabled = settings.sleepReminderEnabledFlow.first(),
                hour = settings.sleepReminderHourFlow.first(),
                minute = settings.sleepReminderMinuteFlow.first()
            )
        }
    }

    private fun scheduleOne(
        context: Context,
        action: String,
        requestCode: Int,
        enabled: Boolean,
        hour: Int,
        minute: Int
    ) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, TrackerReminderReceiver::class.java).apply { this.action = action }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (!enabled) {
            manager.cancel(pendingIntent)
            return
        }

        val now = ZonedDateTime.now()
        var next = now.withHour(hour.coerceIn(0, 23))
            .withMinute(minute.coerceIn(0, 59))
            .withSecond(0)
            .withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)

        val triggerAt = next.toInstant().toEpochMilli()
        val canUseExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            manager.canScheduleExactAlarms()
        try {
            if (canUseExact) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } catch (_: SecurityException) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }
}
