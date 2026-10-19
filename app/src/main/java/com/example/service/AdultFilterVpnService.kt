package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * On-device DNS filtering VPN Service for device-wide 18+ adult content blocking.
 *
 * Configures the system network stack to resolve domain names through verified,
 * privacy-first adult-blocking DNS resolvers (CleanBrowsing Family, Cloudflare Families 1.1.1.3).
 *
 * Privacy Note: Does NOT inspect or decrypt HTTPS traffic, does NOT tunnel traffic
 * to any remote proxy, and maintains zero browsing logs or telemetry.
 */
class AdultFilterVpnService : VpnService(), Runnable {

    companion object {
        const val ACTION_START = "com.example.service.AdultFilterVpnService.START"
        const val ACTION_STOP = "com.example.service.AdultFilterVpnService.STOP"
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "18_blocker_adult_filter_channel"

        private val _isVpnActive = MutableStateFlow(false)
        val isVpnActive: StateFlow<Boolean> = _isVpnActive.asStateFlow()

        fun isVpnConfigured(context: Context): Boolean {
            return prepare(context) == null
        }

        fun start(context: Context) {
            val intent = Intent(context, AdultFilterVpnService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AdultFilterVpnService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnThread: Thread? = null
    @Volatile
    private var isRunning = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopVpn()
                return START_NOT_STICKY
            }
            else -> {
                startVpn()
                return START_STICKY
            }
        }
    }

    private fun startVpn() {
        if (isRunning) return
        isRunning = true
        _isVpnActive.value = true

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildForegroundNotification())

        vpnThread = Thread(this, "AdultFilterVpnThread").apply {
            start()
        }
    }

    override fun run() {
        try {
            val builder = Builder()
                .setSession("18 Blocker Adult Shield")
                .setMtu(1500)
                .addAddress("10.18.0.2", 32)
                // Cloudflare 1.1.1.3 (Malware + Adult Content Blocking)
                .addDnsServer("1.1.1.3")
                .addDnsServer("1.0.0.3")
                // CleanBrowsing Family Filter (Strict Adult & SafeSearch DNS)
                .addDnsServer("185.228.168.168")
                .addDnsServer("185.228.169.168")
                // AdGuard Family Protection DNS
                .addDnsServer("94.140.14.15")

            // Add DNS server IP routing so DNS queries route through the filtered resolvers
            builder.addRoute("1.1.1.3", 32)
            builder.addRoute("1.0.0.3", 32)
            builder.addRoute("185.228.168.168", 32)
            builder.addRoute("185.228.169.168", 32)
            builder.addRoute("94.140.14.15", 32)

            // Configure pending intent on notification tap
            val configIntent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this, 0, configIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.setConfigureIntent(pendingIntent)

            vpnInterface = builder.establish()

            // Keep the thread active while VPN is running
            while (isRunning && vpnInterface != null) {
                try {
                    Thread.sleep(1000)
                } catch (_: InterruptedException) {
                    break
                }
            }
        } catch (e: Exception) {
            // Handle VPN establishment errors gracefully
        } finally {
            cleanup()
        }
    }

    private fun stopVpn() {
        isRunning = false
        _isVpnActive.value = false
        vpnThread?.interrupt()
        cleanup()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun cleanup() {
        try {
            vpnInterface?.close()
        } catch (_: IOException) {}
        vpnInterface = null
        _isVpnActive.value = false
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVpn()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "18+ Adult Content Protection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Status of on-device family DNS adult content filtering"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("18 Blocker: Adult Protection Active")
            .setContentText("Device-wide family DNS filter is protecting browsers and apps.")
            .setSmallIcon(R.drawable.app_icon_18_blocker_1792376066440)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
