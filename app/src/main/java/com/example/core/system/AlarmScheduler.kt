package com.example.core.system

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.repository.AppBlockerRepository
import com.example.receiver.ScheduleAlarmReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.ZoneId

/**
 * Schedules the next opening or closing boundary for an enabled schedule.
 */
object AlarmScheduler {

    const val ACTION_SCHEDULE_TRANSITION = "com.example.strictblocker.SCHEDULE_TRANSITION"
    private const val REQUEST_CODE = 4040
    private const val TAG = "AlarmScheduler"

    fun scheduleNextTransition(context: Context) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                    ?: return@launch

                val intent = Intent(appContext, ScheduleAlarmReceiver::class.java).apply {
                    action = ACTION_SCHEDULE_TRANSITION
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    appContext,
                    REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val nextTransition = AppBlockerRepository(appContext).findNextGlobalTransition()
                if (nextTransition == null) {
                    // Cancel a previously scheduled alarm if its app/window was removed
                    // or all schedule periods were disabled.
                    alarmManager.cancel(pendingIntent)
                    pendingIntent.cancel()
                    return@launch
                }

                val transitionMillis = nextTransition.transitionDateTime
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()

                // The evaluator should always provide a future time. Guard against an
                // unexpectedly stale value rather than attempting to install a past alarm.
                if (transitionMillis <= System.currentTimeMillis()) {
                    Log.w(TAG, "Recalculating a stale transition time")
                    alarmManager.cancel(pendingIntent)
                    pendingIntent.cancel()
                    // Re-query from the current time instead of leaving protection without
                    // a future boundary alarm if a time change or scheduling race made this stale.
                    scheduleNextTransition(appContext)
                    return@launch
                }

                intent.putExtra("packageName", nextTransition.packageName)
                // Recreate the pending intent with the target package as an extra.
                val transitionIntent = PendingIntent.getBroadcast(
                    appContext,
                    REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val exactAlarmsAllowed = try {
                        alarmManager.canScheduleExactAlarms()
                    } catch (_: SecurityException) {
                        false
                    }
                    if (exactAlarmsAllowed) {
                        try {
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                transitionMillis,
                                transitionIntent
                            )
                        } catch (_: SecurityException) {
                            // The special access may be revoked between checking it and
                            // scheduling. Preserve the boundary with an inexact fallback.
                            alarmManager.setAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                transitionMillis,
                                transitionIntent
                            )
                        }
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            transitionMillis,
                            transitionIntent
                        )
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        transitionMillis,
                        transitionIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        transitionMillis,
                        transitionIntent
                    )
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Could not schedule transition alarm: ${t.message}", t)
            }
        }
    }
}
