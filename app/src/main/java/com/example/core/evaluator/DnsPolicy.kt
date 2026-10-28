package com.example.core.evaluator

import java.io.ByteArrayOutputStream
import java.util.Locale

/** DNS wire operations kept separate so routing and SafeSearch can be regression tested. */
object DnsPolicy {
    data class Question(val name: String, val type: Int, val end: Int)

    fun question(packet: ByteArray): Question? = runCatching {
        require(packet.size >= 17 && u16(packet, 4) == 1 && (packet[2].toInt() and 0x80) == 0)
        var offset = 12
        val labels = mutableListOf<String>()
        while (true) {
            val length = packet[offset++].toInt() and 255
            if (length == 0) break
            require(length in 1..63 && offset + length <= packet.size)
            labels += String(packet, offset, length, Charsets.US_ASCII)
            offset += length
        }
        require(offset + 4 <= packet.size && labels.isNotEmpty() && u16(packet, offset + 2) == 1)
        val name = labels.joinToString(".").lowercase(Locale.ROOT)
        require(name.length <= 253)
        Question(name, u16(packet, offset), offset + 4)
    }.getOrNull()

    fun safeSearchTarget(host: String): String? {
        val domain = host.lowercase(Locale.ROOT).trimEnd('.')
        if (Regex("(?:www\\.|images\\.)?google\\.(?:com|[a-z]{2}|com\\.[a-z]{2}|co\\.[a-z]{2}|cat)").matches(domain))
            return "forcesafesearch.google.com"
        if (domain in setOf("bing.com", "www.bing.com", "edgeservices.bing.com")) return "strict.bing.com"
        return null
    }

    fun queryFor(original: ByteArray, target: String, type: Int): ByteArray = ByteArrayOutputStream().apply {
        write(original.copyOfRange(0, 2)); write(byteArrayOf(1, 0, 0, 1, 0, 0, 0, 0, 0, 0))
        write(nameBytes(target)); writeU16(type); writeU16(1)
    }.toByteArray()

    /** Extract only A/AAAA answers. Compression pointers never escape their source packet. */
    fun addresses(response: ByteArray, type: Int): List<ByteArray> = runCatching {
        require(response.size >= 12 && (response[3].toInt() and 15) == 0)
        var offset = 12
        repeat(u16(response, 4)) { offset = skipName(response, offset) + 4; require(offset <= response.size) }
        val result = mutableListOf<ByteArray>()
        repeat(u16(response, 6).coerceAtMost(128)) {
            offset = skipName(response, offset)
            val recordType = u16(response, offset)
            val recordClass = u16(response, offset + 2)
            val length = u16(response, offset + 8)
            offset += 10
            require(offset + length <= response.size)
            if (recordType == type && recordClass == 1 && ((type == 1 && length == 4) || (type == 28 && length == 16)))
                result += response.copyOfRange(offset, offset + length)
            offset += length
        }
        result
    }.getOrDefault(emptyList())

    fun answer(query: ByteArray, question: Question, code: Int = 0, target: String? = null, addresses: List<ByteArray> = emptyList()): ByteArray {
        val cname = target?.let(::nameBytes)
        val answers = if (cname == null) 0 else 1 + addresses.size
        return ByteArrayOutputStream().apply {
            write(query.copyOfRange(0, 2)); write(0x81); write(0x80 or (code and 15))
            writeU16(1); writeU16(answers); writeU16(0); writeU16(0)
            write(query.copyOfRange(12, question.end))
            if (cname != null) {
                write(byteArrayOf(0xc0.toByte(), 0x0c)); writeU16(5); writeU16(1)
                write(byteArrayOf(0, 0, 0, 60)); writeU16(cname.size); write(cname)
                addresses.forEach { address ->
                    write(cname); writeU16(question.type); writeU16(1)
                    write(byteArrayOf(0, 0, 0, 60)); writeU16(address.size); write(address)
                }
            }
        }.toByteArray()
    }

    private fun skipName(packet: ByteArray, start: Int): Int {
        var offset = start
        repeat(128) {
            require(offset < packet.size)
            val length = packet[offset++].toInt() and 255
            if (length == 0) return offset
            if (length and 0xc0 == 0xc0) { require(offset < packet.size); return offset + 1 }
            require(length <= 63 && offset + length <= packet.size)
            offset += length
        }
        error("Invalid DNS name")
    }
    private fun nameBytes(name: String): ByteArray = ByteArrayOutputStream().apply {
        name.split('.').forEach { label -> write(label.length); write(label.toByteArray(Charsets.US_ASCII)) }; write(0)
    }.toByteArray()
    private fun ByteArrayOutputStream.writeU16(value: Int) { write((value ushr 8) and 255); write(value and 255) }
    private fun u16(bytes: ByteArray, at: Int): Int {
        require(at >= 0 && at + 1 < bytes.size)
        return ((bytes[at].toInt() and 255) shl 8) or (bytes[at + 1].toInt() and 255)
    }
}
