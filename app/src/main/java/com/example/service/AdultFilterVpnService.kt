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
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.core.evaluator.AdultContentEvaluator
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
 * Configures the system network stack to resolve domain names through verified,
 * privacy-first adult-blocking DNS resolvers (CleanBrowsing Family, Cloudflare Families 1.1.1.3).
 *
 * Intercepts DNS queries on port 53 locally:
 * - If domain is on the adult blocklist, responds with NXDOMAIN / 0.0.0.0 locally.
 * - Otherwise forwards through Cloudflare Families (1.1.1.3) protected upstream.
 *
 * Privacy Note: Does NOT inspect or decrypt HTTPS traffic, does NOT tunnel non-DNS traffic
 * to any remote proxy. Explicitly blocked DNS domains may be recorded as local block events; no browsing data is uploaded by this service.
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

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var repository: AppBlockerRepository

    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnThread: Thread? = null
    @Volatile
    private var isRunning = false
    private var udpSocket: DatagramSocket? = null

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
            inStream = FileInputStream(vpnInterface!!.fileDescriptor)
            outStream = FileOutputStream(vpnInterface!!.fileDescriptor)

            val socket = DatagramSocket()
            if (!protect(socket)) {
                socket.close()
                throw IOException("Could not protect upstream DNS socket from VPN routing")
            }
            socket.soTimeout = 2500
            udpSocket = socket

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

                val queriedDomain = extractDomainFromDnsQuery(dnsPayload)

                // Inspect queried domain with Adult Content Evaluator
                if (queriedDomain.isNotBlank() && (AdultContentEvaluator.isAdultDomain(queriedDomain) ||
                            AdultContentEvaluator.evaluateText(queriedDomain) is AdultContentEvaluator.EvaluationResult.Blocked)) {
                    // Record interception asynchronously
                    serviceScope.launch {
                        repository.recordAdultBlockEvent("DNS Block: $queriedDomain")
                    }

                    // Return NXDOMAIN blocked response
                    val forgedDns = buildNxDomainResponse(dnsPayload)
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
                    val outPacket = DatagramPacket(dnsPayload, dnsPayloadLen, upstreamDns, 53)
                    socket.send(outPacket)

                    val inPacket = DatagramPacket(responseBuffer, responseBuffer.size)
                    socket.receive(inPacket)
                    if (inPacket.port != 53 || !inPacket.address.address.contentEquals(upstreamDns.address)) {
                        continue
                    }

                    val upstreamDnsPayload = ByteArray(inPacket.length)
                    System.arraycopy(responseBuffer, 0, upstreamDnsPayload, 0, inPacket.length)

                    val responsePacket = buildIpv4UdpPacket(buffer, ipHeaderLength, upstreamDnsPayload)
                    outStream.write(responsePacket)
                } catch (_: Exception) {
                    // Upstream timeout or drop; fail gracefully.
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
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun extractDomainFromDnsQuery(payload: ByteArray): String {
        var idx = 12 // Skip DNS 12-byte header
        val sb = StringBuilder()
        while (idx < payload.size) {
            val len = payload[idx].toInt() and 0xFF
            if (len == 0) break
            if ((len and 0xC0) == 0xC0) break // Compression offset
            idx++
            if (idx + len > payload.size) break
            if (sb.isNotEmpty()) sb.append('.')
            sb.append(String(payload, idx, len, Charsets.US_ASCII))
            idx += len
        }
        return sb.toString().trim()
    }

    private fun buildNxDomainResponse(queryPayload: ByteArray): ByteArray {
        val response = queryPayload.copyOf()
        if (response.size >= 4) {
            // Flags: Response flag (QR=1) + RCODE=3 (NXDOMAIN)
            response[2] = (response[2].toInt() or 0x81).toByte()
            response[3] = (response[3].toInt() or 0x83).toByte()
        }
        return response
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

    private fun stopVpn() {
        isRunning = false
        _isVpnActive.value = false
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
            .setContentTitle("18 Blocker: Adult Protection Active")
            .setContentText("Device-wide family DNS filter is protecting browsers and apps.")
            .setSmallIcon(R.drawable.ic_launcher_fg)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
