package com.example.core.evaluator

import org.junit.Assert.*
import org.junit.Test

class DnsPolicyTest {
    private fun query(host: String, type: Int = 1) = DnsPolicy.queryFor(byteArrayOf(0x12, 0x34), host, type)

    @Test fun safeSearchMapsSearchHostsWithoutMatchingLookalikes() {
        for (host in listOf("google.com", "www.google.co.in", "www.google.co.uk", "images.google.com", "www.google.de"))
            assertEquals("forcesafesearch.google.com", DnsPolicy.safeSearchTarget(host))
        assertEquals("strict.bing.com", DnsPolicy.safeSearchTarget("www.bing.com"))
        assertEquals("strict.bing.com", DnsPolicy.safeSearchTarget("edgeservices.bing.com"))
        for (host in listOf("google.com.evil.com", "notgoogle.com", "bing.com.evil.org", "accounts.google.com", "example.com"))
            assertNull(DnsPolicy.safeSearchTarget(host))
    }

    @Test fun cnameAnswerPreservesOriginalQuestionAndTransaction() {
        val original = query("www.google.co.in")
        val question = DnsPolicy.question(original)!!
        val response = DnsPolicy.answer(original, question, target = "forcesafesearch.google.com",
            addresses = listOf(byteArrayOf(216.toByte(), 239.toByte(), 38, 120)))
        assertEquals(0x12, response[0].toInt()); assertEquals(0x34, response[1].toInt())
        assertEquals(2, response[7].toInt())
        assertArrayEquals(original.copyOfRange(12, question.end), response.copyOfRange(12, question.end))
        assertArrayEquals(byteArrayOf(216.toByte(), 239.toByte(), 38, 120), DnsPolicy.addresses(response, 1).single())
    }

    @Test fun ipv6AnswerUsesAAAARecords() {
        val original = query("www.bing.com", 28)
        val ip = ByteArray(16) { it.toByte() }
        val response = DnsPolicy.answer(original, DnsPolicy.question(original)!!, target = "strict.bing.com", addresses = listOf(ip))
        assertArrayEquals(ip, DnsPolicy.addresses(response, 28).single())
        assertTrue(DnsPolicy.addresses(response, 1).isEmpty())
    }

    @Test fun blockedResponseIsNxdomainWithoutForgedAddresses() {
        val original = query("pornhub.com")
        val response = DnsPolicy.answer(original, DnsPolicy.question(original)!!, code = 3)
        assertEquals(3, response[3].toInt() and 15)
        assertEquals(0, response[7].toInt())
        assertTrue(DnsPolicy.addresses(response, 1).isEmpty())
    }

    @Test fun malformedTruncatedAndCompressedQueriesNeverCrash() {
        val valid = query("example.com")
        for (length in 0 until valid.size) assertNull(DnsPolicy.question(valid.copyOf(length)))
        val compressed = valid.copyOf().apply { this[12] = 0xc0.toByte() }
        assertNull(DnsPolicy.question(compressed))
        assertTrue(DnsPolicy.addresses(byteArrayOf(1, 2, 3), 1).isEmpty())
    }

    @Test fun operaVariantsAndActivePrivateWindowsAreRecognized() {
        for (pkg in listOf("com.opera.gx", "com.opera.mini.native.beta", "com.opera.touch", "com.opera.future", "com.chrome.canary", "org.mozilla.firefox_beta"))
            assertTrue(pkg, AdultContentEvaluator.isBrowserPackage(pkg))
        assertTrue(AdultContentEvaluator.isActivePrivateIndicator("You've gone incognito"))
        assertTrue(AdultContentEvaluator.isActivePrivateIndicator("Private", selected = true))
        assertFalse(AdultContentEvaluator.isActivePrivateIndicator("New incognito tab"))
        assertFalse(AdultContentEvaluator.isActivePrivateIndicator("Private", selected = false))
        assertTrue(AdultContentEvaluator.isPrivateOnlyBrowser("org.mozilla.focus"))
        assertFalse(AdultContentEvaluator.isPrivateOnlyBrowser("com.android.chrome"))
    }
}
