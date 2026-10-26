package com.example.receiver

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.core.system.TrackerReminderScheduler

class TrackerReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val type = when (action) {
            TrackerReminderScheduler.ACTION_DAILY_CHECK_IN -> "daily"
            TrackerReminderScheduler.ACTION_SLEEP -> "sleep"
            else -> return
        }

        createChannels(context)
        if (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        ) {
            postNotification(context, type)
        }

        // One-shot alarms are intentionally re-scheduled after firing so there is
        // never an aggressive retry loop.
        TrackerReminderScheduler.refresh(context)
    }

    private fun postNotification(context: Context, type: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val isSleep = type == "sleep"
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_TRACKER_ACTION, type)
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            if (isSleep) 6202 else 6201,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            context,
            if (isSleep) CHANNEL_SLEEP else CHANNEL_DAILY
        )
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(if (isSleep) "Sleep log" else "Daily check-in")
            .setContentText(
                if (isSleep) "How much sleep did you get last night?"
                else "Take a moment to check in with today's progress."
            )
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        manager.notify(if (isSleep) 6302 else 6301, notification)
    }

    private fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DAILY,
                "Daily check-in",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Optional daily progress reminders"
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SLEEP,
                "Sleep reminder",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Optional reminder to manually log sleep"
            }
        )
    }

    companion object {
        private const val CHANNEL_DAILY = "daily_check_in"
        private const val CHANNEL_SLEEP = "sleep_reminder"
    }
}
