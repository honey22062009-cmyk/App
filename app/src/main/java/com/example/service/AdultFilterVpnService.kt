package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.system.OsConstants
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.core.evaluator.AdultContentEvaluator
import com.example.core.evaluator.DnsPolicy
import com.example.data.repository.AppBlockerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.Inet4Address
import java.nio.ByteBuffer

/**
 * On-device DNS filtering VPN Service for device-wide 18+ adult content blocking.
 *
 * Configures system DNS to use the selected IPv4 resolver(s). If no custom resolver is set,
 * the default upstreams are Cloudflare Family DNS (1.1.1.3 and 1.0.0.3). Custom DNS addresses
 * are user-selected and are not independently verified by this app.
 *
 * Intercepts IPv4 UDP DNS queries on port 53 locally:
 * - If a domain matches the local adult block rules, returns a local NXDOMAIN response.
 * - Otherwise forwards the DNS query to the configured upstream resolver.
 *
 * Privacy note: this service does not decrypt HTTPS or route non-DNS traffic through a proxy.
 * Non-blocked DNS queries are sent to the configured resolver, which can see the queried domain
 * and may log it according to its own policies. Blocked domains may be recorded as local events;
 * this app does not include an analytics or browsing-history upload backend.
 */
class AdultFilterVpnService : VpnService(), Runnable {

    companion object {
        const val ACTION_START = "com.example.service.AdultFilterVpnService.START"
        const val ACTION_STOP = "com.example.service.AdultFilterVpnService.STOP"
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "18_blocker_adult_filter_channel"

        private val _isVpnActive = MutableStateFlow(false)
        val isVpnActive: StateFlow<Boolean> = _isVpnActive.asStateFlow()
        private val _alwaysOn = MutableStateFlow(false)
        val alwaysOn: StateFlow<Boolean> = _alwaysOn.asStateFlow()
        private val _queries = MutableStateFlow(0L)
        val processedQueries: StateFlow<Long> = _queries.asStateFlow()

        fun isVpnConfigured(context: Context): Boolean {
            return prepare(context) == null
        }

        fun start(context: Context) {
            val intent = Intent(context, AdultFilterVpnService::class.java).apply {
                action = ACTION_START
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (t: Throwable) {
                Log.w("AdultFilterVpn", "Could not start VPN foreground service", t)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AdultFilterVpnService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (t: Throwable) {
                Log.w("AdultFilterVpn", "Could not stop VPN service cleanly", t)
            }
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var repository: AppBlockerRepository

    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnThread: Thread? = null
    @Volatile
    private var isRunning = false
    private var udpSocket: DatagramSocket? = null
    private var restartAttempt = 0

    override fun onCreate() {
        super.onCreate()
        repository = AppBlockerRepository(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopVpn()
                return START_NOT_STICKY
            }
            ACTION_START, null -> {
                // Enter foreground immediately to meet Android's foreground-service
                // deadline, then verify persisted user switches off the main thread.
                createNotificationChannel()
                startForeground(NOTIFICATION_ID, buildForegroundNotification())
                serviceScope.launch {
                    try {
                        val enabled = repository.settings.isAdultProtectionEnabledFlow.first() &&
                            repository.settings.isDnsFilterEnabledFlow.first()
                        if (enabled) {
                            startVpn()
                        } else {
                            Log.i("AdultFilterVpn", "DNS filter is disabled in settings; not starting the tunnel")
                            stopVpn()
                        }
                    } catch (e: Exception) {
                        Log.w("AdultFilterVpn", "Could not verify DNS filter settings", e)
                        stopVpn()
                    }
                }
                return START_STICKY
            }
            else -> {
                stopSelf(startId)
                return START_NOT_STICKY
            }
        }
    }

    private fun startVpn() {
        if (isRunning) {
            _alwaysOn.value = Build.VERSION.SDK_INT >= 29 && isAlwaysOn
            return
        }
        isRunning = true

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildForegroundNotification())

        vpnThread = Thread(this, "AdultFilterVpnThread").apply {
            start()
        }
    }

    override fun run() {
        var inStream: FileInputStream? = null
        var outStream: FileOutputStream? = null

        try {
            val configuredDns = runBlocking {
                listOf(
                    repository.settings.customDnsPrimaryFlow.first(),
                    repository.settings.customDnsSecondaryFlow.first()
                ).mapNotNull { raw ->
                    raw.trim().takeIf { value ->
                        runCatching { InetAddress.getByName(value) is Inet4Address }.getOrDefault(false)
                    }
                }.distinct()
            }.ifEmpty { listOf("1.1.1.3", "1.0.0.3") }

            val builder = Builder()
                .setSession("Strict Blocker DNS Protection")
                .setMtu(1500)
                .setBlocking(true)
                .allowFamily(OsConstants.AF_INET6)
                .addAddress("10.18.0.2", 32)

            configuredDns.forEach { dns ->
                builder.addDnsServer(dns)
                builder.addRoute(dns, 32)
            }

            val configIntent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this, 0, configIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.setConfigureIntent(pendingIntent)

            vpnInterface = builder.establish() ?: return
            restartAttempt = 0
            inStream = FileInputStream(vpnInterface!!.fileDescriptor)
            outStream = FileOutputStream(vpnInterface!!.fileDescriptor)

            val socket = DatagramSocket()
            if (!protect(socket)) {
                socket.close()
                throw IOException("Could not protect upstream DNS socket from VPN routing")
            }
            socket.soTimeout = 2500
            udpSocket = socket
            _isVpnActive.value = true
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIFICATION_ID, buildForegroundNotification())
            _alwaysOn.value = Build.VERSION.SDK_INT >= 29 && isAlwaysOn

            val configuredDnsAddresses = configuredDns.map { InetAddress.getByName(it) }
            val defaultUpstreamDns = configuredDnsAddresses.first()
            val buffer = ByteArray(32767)
            val responseBuffer = ByteArray(4096)

            while (isRunning && vpnInterface != null) {
                val length = try {
                    inStream.read(buffer)
                } catch (_: Exception) {
                    break
                }

                if (length <= 0) continue

                // Validate packet/header lengths before indexing or copying. Although
                // packets normally originate from Android's TUN interface, malformed or
                // truncated input must never cause an out-of-bounds read.
                val version = (buffer[0].toInt() and 0xF0) shr 4
                if (version != 4 || length < 28) continue

                val ipHeaderLength = (buffer[0].toInt() and 0x0F) * 4
                if (ipHeaderLength < 20 || ipHeaderLength + 8 > length) continue
                val ipTotalLength =
                    ((buffer[2].toInt() and 0xFF) shl 8) or (buffer[3].toInt() and 0xFF)
                if (ipTotalLength < ipHeaderLength + 8 || ipTotalLength > length) continue

                val protocol = buffer[9].toInt() and 0xFF
                if (protocol != 17) continue // UDP is 17

                val destPort =
                    ((buffer[ipHeaderLength + 2].toInt() and 0xFF) shl 8) or
                        (buffer[ipHeaderLength + 3].toInt() and 0xFF)
                if (destPort != 53) continue // DNS is port 53

                val udpLength =
                    ((buffer[ipHeaderLength + 4].toInt() and 0xFF) shl 8) or
                        (buffer[ipHeaderLength + 5].toInt() and 0xFF)
                if (udpLength < 8 || udpLength > ipTotalLength - ipHeaderLength) continue

                val dnsPayloadOffset = ipHeaderLength + 8
                val dnsPayloadLen = udpLength - 8
                if (dnsPayloadLen < 12 || dnsPayloadOffset + dnsPayloadLen > ipTotalLength) continue

                val dnsPayload = ByteArray(dnsPayloadLen)
                System.arraycopy(buffer, dnsPayloadOffset, dnsPayload, 0, dnsPayloadLen)

                val question = DnsPolicy.question(dnsPayload) ?: continue
                val queriedDomain = question.name
                _queries.value = _queries.value + 1

                // Inspect queried domain with Adult Content Evaluator
                if (queriedDomain.isNotBlank() && (AdultContentEvaluator.isAdultDomain(queriedDomain) ||
                            AdultContentEvaluator.evaluateText(queriedDomain) is AdultContentEvaluator.EvaluationResult.Blocked)) {
                    // Record interception asynchronously
                    serviceScope.launch {
                        repository.recordAdultBlockEvent("DNS Block: $queriedDomain")
                    }

                    // Return NXDOMAIN blocked response
                    val forgedDns = DnsPolicy.answer(dnsPayload, question, code = 3)
                    val responsePacket = buildIpv4UdpPacket(buffer, ipHeaderLength, forgedDns)
                    try {
                        outStream.write(responsePacket)
                    } catch (_: Exception) {}
                    continue
                }

                // Android may direct a query to either configured DNS server. Respect
                // the resolver IP in the original packet rather than sending every query
                // to the primary and silently ignoring the configured secondary.
                val requestedResolver = try {
                    InetAddress.getByAddress(buffer.copyOfRange(16, 20))
                } catch (_: Exception) {
                    defaultUpstreamDns
                }
                val upstreamDns = configuredDnsAddresses.firstOrNull {
                    it.address.contentEquals(requestedResolver.address)
                } ?: defaultUpstreamDns

                // Forward clean DNS queries through the protected socket to avoid VPN loops.
                try {
                    val safeTarget = if (runBlocking { repository.settings.isForceSafeSearchEnabledFlow.first() })
                        DnsPolicy.safeSearchTarget(queriedDomain) else null
                    // Suppress HTTPS/SVCB shortcuts for mapped search hosts; the client falls back to A/AAAA.
                    if (safeTarget != null && question.type !in setOf(1, 28)) {
                        outStream.write(buildIpv4UdpPacket(buffer, ipHeaderLength, DnsPolicy.answer(dnsPayload, question)))
                        continue
                    }
                    val upstreamQuery = safeTarget?.let { DnsPolicy.queryFor(dnsPayload, it, question.type) } ?: dnsPayload
                    val outPacket = DatagramPacket(upstreamQuery, upstreamQuery.size, upstreamDns, 53)
                    socket.send(outPacket)

                    val inPacket = DatagramPacket(responseBuffer, responseBuffer.size)
                    socket.receive(inPacket)
                    if (inPacket.length < 12 || inPacket.port != 53 ||
                        !inPacket.address.address.contentEquals(upstreamDns.address) ||
                        responseBuffer[0] != dnsPayload[0] || responseBuffer[1] != dnsPayload[1]) {
                        continue
                    }

                    val upstreamDnsPayload = ByteArray(inPacket.length)
                    System.arraycopy(responseBuffer, 0, upstreamDnsPayload, 0, inPacket.length)

                    val finalPayload = if (safeTarget != null) {
                        if ((upstreamDnsPayload[3].toInt() and 15) != 0) DnsPolicy.answer(dnsPayload, question, code = 2)
                        else DnsPolicy.answer(dnsPayload, question, target = safeTarget,
                            addresses = DnsPolicy.addresses(upstreamDnsPayload, question.type))
                    } else upstreamDnsPayload
                    val responsePacket = buildIpv4UdpPacket(buffer, ipHeaderLength, finalPayload)
                    outStream.write(responsePacket)
                } catch (_: Exception) {
                    // A timeout must not become an unfiltered response or a silent success.
                    runCatching { outStream.write(buildIpv4UdpPacket(buffer, ipHeaderLength,
                        DnsPolicy.answer(dnsPayload, question, code = 2))) }
                }
            }
        } catch (e: Exception) {
            Log.w("AdultFilterVpn", "VPN loop encountered error: ${e.message}")
        } finally {
            try { inStream?.close() } catch (_: Exception) {}
            try { outStream?.close() } catch (_: Exception) {}
            // stopVpn() sets isRunning=false before closing the tunnel. If this worker
            // exits while still marked running, however, it failed unexpectedly: remove
            // the stale foreground notification and stop the service so it can be started
            // cleanly again on the next user action.
            val unexpectedExit = isRunning
            cleanup()
            if (unexpectedExit) {
                scheduleTunnelRestart()
            }
        }
    }

    private fun buildIpv4UdpPacket(origIpPacket: ByteArray, ihl: Int, dnsPayload: ByteArray): ByteArray {
        val newUdpLength = 8 + dnsPayload.size
        val newTotalLength = ihl + newUdpLength
        val packet = ByteArray(newTotalLength)

        // Copy IP Header
        System.arraycopy(origIpPacket, 0, packet, 0, ihl)

        // Swap Source and Destination IPs
        // Orig Src: origIpPacket[12..15], Orig Dst: origIpPacket[16..19]
        System.arraycopy(origIpPacket, 16, packet, 12, 4) // New Src = Orig Dst
        System.arraycopy(origIpPacket, 12, packet, 16, 4) // New Dst = Orig Src

        // Total Length in IP header
        packet[2] = ((newTotalLength shr 8) and 0xFF).toByte()
        packet[3] = (newTotalLength and 0xFF).toByte()

        // Recalculate IP Header Checksum
        packet[10] = 0
        packet[11] = 0
        val ipChecksum = computeIpChecksum(packet, ihl)
        packet[10] = ((ipChecksum.toInt() shr 8) and 0xFF).toByte()
        packet[11] = (ipChecksum.toInt() and 0xFF).toByte()

        // Swap UDP Ports
        val origSrcPort0 = origIpPacket[ihl]
        val origSrcPort1 = origIpPacket[ihl + 1]
        val origDstPort0 = origIpPacket[ihl + 2]
        val origDstPort1 = origIpPacket[ihl + 3]

        packet[ihl] = origDstPort0
        packet[ihl + 1] = origDstPort1
        packet[ihl + 2] = origSrcPort0
        packet[ihl + 3] = origSrcPort1

        // UDP Length
        packet[ihl + 4] = ((newUdpLength shr 8) and 0xFF).toByte()
        packet[ihl + 5] = (newUdpLength and 0xFF).toByte()

        // UDP Checksum = 0 (optional in IPv4)
        packet[ihl + 6] = 0
        packet[ihl + 7] = 0

        // Copy DNS payload
        System.arraycopy(dnsPayload, 0, packet, ihl + 8, dnsPayload.size)

        return packet
    }

    private fun computeIpChecksum(buffer: ByteArray, headerLength: Int): Short {
        var sum = 0
        var i = 0
        while (i < headerLength) {
            val word = ((buffer[i].toInt() and 0xFF) shl 8) or (buffer[i + 1].toInt() and 0xFF)
            sum += word
            i += 2
        }
        while ((sum shr 16) > 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return sum.inv().toShort()
    }

    private fun scheduleTunnelRestart() {
        serviceScope.launch {
            restartAttempt = (restartAttempt + 1).coerceAtMost(6)
            val delayMs = (1_000L shl (restartAttempt - 1)).coerceAtMost(30_000L)
            kotlinx.coroutines.delay(delayMs)

            try {
                val stillEnabled =
                    repository.settings.isAdultProtectionEnabledFlow.first() &&
                        repository.settings.isDnsFilterEnabledFlow.first()

                if (
                    stillEnabled &&
                    !isRunning &&
                    isVpnConfigured(applicationContext)
                ) {
                    Log.i("AdultFilterVpn", "Retrying DNS tunnel after unexpected exit")
                    startVpn()
                } else if (!stillEnabled) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            } catch (t: Throwable) {
                Log.w("AdultFilterVpn", "VPN restart attempt failed", t)
                if (restartAttempt < 6) {
                    scheduleTunnelRestart()
                }
            }
        }
    }

    private fun stopVpn() {
        restartAttempt = 0
        isRunning = false
        _isVpnActive.value = false
        _alwaysOn.value = false
        vpnThread?.interrupt()
        cleanup()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun cleanup() {
        // Also reset the guard after an unexpected VPN-thread exit. Otherwise the
        // observable state becomes inactive while startVpn() remains permanently blocked.
        isRunning = false
        try {
            udpSocket?.close()
        } catch (_: Exception) {}
        udpSocket = null

        try {
            vpnInterface?.close()
        } catch (_: IOException) {}
        vpnInterface = null
        _isVpnActive.value = false
        _alwaysOn.value = false
    }

    override fun onRevoke() {
        stopVpn()
        super.onRevoke()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
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
            .setContentTitle(if (_isVpnActive.value) "Strict Blocker · DNS filter running" else "Strict Blocker · Connecting DNS filter")
            .setContentText("Filters system DNS. Browser inspection adds URL and private-tab checks.")
            .setSmallIcon(R.drawable.ic_notification_shield)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
