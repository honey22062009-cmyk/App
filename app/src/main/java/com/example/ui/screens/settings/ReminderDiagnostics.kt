package com.example.ui.screens.settings

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.app.NotificationManagerCompat
import com.example.core.system.PermissionHelper
import com.example.core.system.TrackerReminderScheduler
import com.example.receiver.TrackerReminderReceiver

@Composable
internal fun ReminderDiagnostics() {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var revision by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    val requestNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        revision++; TrackerReminderScheduler.refresh(context)
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) { revision++; TrackerReminderScheduler.refresh(context) }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val allowed = remember(revision) {
        PermissionHelper.isNotificationPermissionGranted(context) && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    val exact = remember(revision) { PermissionHelper.canScheduleExactAlarms(context) }
    val blockedChannel = remember(revision) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        listOf(TrackerReminderReceiver.CHANNEL_DAILY, TrackerReminderReceiver.CHANNEL_SLEEP)
            .any { manager.getNotificationChannel(it)?.importance == NotificationManager.IMPORTANCE_NONE }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if (allowed && exact && !blockedChannel) "Ready for exact reminders" else "Reminder setup needs attention",
            style = MaterialTheme.typography.titleMedium)
        if (!allowed) Button(onClick = {
            if (Build.VERSION.SDK_INT >= 33 && !PermissionHelper.isNotificationPermissionGranted(context))
                requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            else context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
        }) { Text("Allow notifications") }
        if (blockedChannel) TextButton(onClick = {
            context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
        }) { Text("Enable reminder notification channels") }
        if (!exact) Button(onClick = { PermissionHelper.openExactAlarmSettings(context) }) { Text("Allow exact timing") }
        OutlinedButton(onClick = {
            message = if (TrackerReminderScheduler.scheduleTest(context))
                "Test scheduled for one minute from now. You can close the app."
            else "Allow exact timing first, then try again."
        }, enabled = allowed && exact && !blockedChannel, modifier = Modifier.fillMaxWidth()) { Text("Test a reminder in 1 minute") }
        message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        Text("Samsung: keep Strict Blocker out of Sleeping apps. Force-stopping an app prevents Android from delivering its alarms until it is opened again.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
