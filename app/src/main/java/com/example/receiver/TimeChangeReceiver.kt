package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.core.system.AlarmScheduler
import com.example.core.system.TrackerReminderScheduler

/**
 * Recalculates future schedule boundaries and transition alarms when:
 * - Timezone changes (TIMEZONE_CHANGED)
 * - Device clock is manually adjusted (TIME_SET)
 * - Date rolls over or changes (DATE_CHANGED)
 */
class TimeChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_DATE_CHANGED -> {
                AlarmScheduler.scheduleNextTransition(context)
        TrackerReminderScheduler.refresh(context)
            }
        }
    }
}
