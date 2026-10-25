package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.PowerManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.R
import com.example.core.system.SafetyWhitelist
import com.example.data.repository.AppBlockerRepository
import com.example.ui.blocker.BlockerActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Persistent foreground service ensuring continuous enforcement and fallback foreground inspection.
 */
class BlockerMonitoringService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var repository: AppBlockerRepository

    @Volatile
    private var lastKnownForegroundPackage: String? = null

    companion object {
        const val CHANNEL_ID = "strict_blocker_service_channel"
        const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            try {
                val intent = Intent(context, BlockerMonitoringService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (t: Throwable) {
                Log.w("BlockerService", "Could not start foreground monitoring service: ${t.message}")
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, BlockerMonitoringService::class.java)
                context.stopService(intent)
            } catch (t: Throwable) {
                Log.w("BlockerService", "Could not stop monitoring service: ${t.message}")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = AppBlockerRepository(applicationContext)

        try {
            createNotificationChannel()
            val notification = buildNotification("Strict protection active")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    0
                }
                ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, fgsType)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            startFallbackMonitoringLoop()
        } catch (t: Throwable) {
            Log.e("BlockerService", "FGS startup failed or restricted by system", t)
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Strict Blocker Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Strict Blocker active in the background"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Strict Blocker Active")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_launcher_fg)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startFallbackMonitoringLoop() {
        serviceScope.launch {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

            var consecutiveFailures = 0
            while (isActive) {
                try {
                    val protectionEnabled = repository.settings.isProtectionEnabledFlow.first()
                    if (protectionEnabled && usageStatsManager != null) {
                        val currentForegroundPkg = getForegroundPackage(usageStatsManager)
                        if (currentForegroundPkg != null && !SafetyWhitelist.isSystemCritical(this@BlockerMonitoringService, currentForegroundPkg)) {
                            val evaluation = repository.evaluateApp(currentForegroundPkg)
                            if (evaluation.isBlocked) {
                                val appLabel = try {
                                    val appInfo = packageManager.getApplicationInfo(currentForegroundPkg, 0)
                                    packageManager.getApplicationLabel(appInfo).toString()
                                } catch (_: Exception) {
                                    currentForegroundPkg
                                }

                                repository.recordBlockEvent(currentForegroundPkg, appLabel, evaluation.reason)

                                val intent = Intent(this@BlockerMonitoringService, BlockerActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                                    putExtra(BlockerActivity.EXTRA_PACKAGE_NAME, currentForegroundPkg)
                                    putExtra(BlockerActivity.EXTRA_APP_NAME, appLabel)
                                    putExtra(BlockerActivity.EXTRA_NEXT_ALLOWED, evaluation.formattedNextAllowedTime)
                                }
                                startActivity(intent)
                            }
                        }
                    }
                    consecutiveFailures = 0
                } catch (t: Exception) {
                    consecutiveFailures++
                    Log.w("BlockerService", "Fallback monitoring pass failed", t)
                    if (consecutiveFailures >= 3) {
                        val manager = getSystemService(NotificationManager::class.java)
                        manager?.notify(
                            NOTIFICATION_ID,
                            buildNotification("Protection fallback needs attention")
                        )
                    }
                }

                val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
                val interactive = powerManager?.isInteractive != false
                val cadenceMs = when {
                    !interactive -> 5_000L
                    consecutiveFailures > 0 -> 2_000L
                    else -> 900L
                }
                delay(cadenceMs)
            }
        }
    }

    private fun getForegroundPackage(usageStatsManager: UsageStatsManager): String? {
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        if (powerManager?.isInteractive == false) return null

        val endTime = System.currentTimeMillis()
        val cachedPackage = lastKnownForegroundPackage
        // A short rolling window detects switches cheaply. Query a full day only once
        // when the service has no cached foreground package (e.g. process restart).
        val lookback = if (cachedPackage == null) {
            24L * 60L * 60L * 1000L
        } else {
            10_000L
        }
        val events = try {
            usageStatsManager.queryEvents(endTime - lookback, endTime)
        } catch (_: SecurityException) {
            return null
        } ?: return cachedPackage

        var mostRecentForegroundPackage: String? = null
        // Android 10 renamed MOVE_TO_FOREGROUND to ACTIVITY_RESUMED. Support both
        // because the app's minSdk is Android 8.0 (API 26).
        val foregroundEventType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            UsageEvents.Event.ACTIVITY_RESUMED
        } else {
            @Suppress("DEPRECATION")
            UsageEvents.Event.MOVE_TO_FOREGROUND
        }

        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == foregroundEventType) {
                mostRecentForegroundPackage = event.packageName
            }
        }

        if (!mostRecentForegroundPackage.isNullOrBlank()) {
            lastKnownForegroundPackage = mostRecentForegroundPackage
        }
        return lastKnownForegroundPackage
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
