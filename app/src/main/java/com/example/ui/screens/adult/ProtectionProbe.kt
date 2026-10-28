package com.example.ui.screens.adult

import com.example.core.evaluator.DnsPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.random.Random

/** Sends DNS-only probes through the active VPN route. Never opens an adult page. */
internal object ProtectionProbe {
    suspend fun check(resolver: String): String = withContext(Dispatchers.IO) {
        runCatching {
            DatagramSocket().use { socket ->
                socket.soTimeout = 3500
                val server = InetAddress.getByName(resolver.ifBlank { "1.1.1.3" })
                fun query(host: String): ByteArray {
                    val original = ByteArray(12).apply { Random.nextBytes(this); this[2] = 1; this[3] = 0 }
                    val data = DnsPolicy.queryFor(original, host, 1)
                    socket.send(DatagramPacket(data, data.size, server, 53))
                    val buffer = ByteArray(4096)
                    val response = DatagramPacket(buffer, buffer.size)
                    socket.receive(response)
                    require(response.address == server && response.length >= 12 && buffer[0] == data[0] && buffer[1] == data[1])
                    return buffer.copyOf(response.length)
                }
                val clean = query("example.com")
                require((clean[3].toInt() and 15) == 0 && DnsPolicy.addresses(clean, 1).isNotEmpty()) { "Normal DNS lookup failed." }
                val blocked = query("pornhub.com")
                require((blocked[3].toInt() and 15) == 3) { "Restricted-domain probe was not blocked." }
                val safe = query("www.google.com")
                require(String(safe, Charsets.ISO_8859_1).contains("forcesafesearch")) { "Google SafeSearch mapping was not observed." }
                "Passed: normal DNS works, the restricted-domain query is blocked, and Google SafeSearch is mapped."
            }
        }.getOrElse { "DNS check failed: ${it.message ?: "No response. Check your connection and reconnect the filter."}" }
    }
}
