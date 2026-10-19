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
 * Schedules exact transition alarms for schedule cutoffs and openings.
 */
object AlarmScheduler {

    const val ACTION_SCHEDULE_TRANSITION = "com.example.strictblocker.SCHEDULE_TRANSITION"
    private const val REQUEST_CODE = 4040

    fun scheduleNextTransition(context: Context) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = AppBlockerRepository(appContext)
                val nextTransition = repository.findNextGlobalTransition() ?: return@launch

                val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return@launch
                val transitionMillis = nextTransition.transitionDateTime
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()

                val intent = Intent(appContext, ScheduleAlarmReceiver::class.java).apply {
                    action = ACTION_SCHEDULE_TRANSITION
                    putExtra("packageName", nextTransition.packageName)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    appContext,
                    REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            transitionMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            transitionMillis,
                            pendingIntent
                        )
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        transitionMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        transitionMillis,
                        pendingIntent
                    )
                }
            } catch (t: Throwable) {
                Log.w("AlarmScheduler", "Could not schedule transition alarm: ${t.message}")
            }
        }
    }
}
