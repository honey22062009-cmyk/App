package com.example.core.evaluator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdultContentEvaluatorTest {

    @Test
    fun testDirectAdultDomainsBlocked() {
        assertTrue(AdultContentEvaluator.isAdultDomain("pornhub.com"))
        assertTrue(AdultContentEvaluator.isAdultDomain("xvideos.com"))
        assertTrue(AdultContentEvaluator.isAdultDomain("www.xnxx.com"))
        assertTrue(AdultContentEvaluator.isAdultDomain("m.xhamster.com"))
        assertTrue(AdultContentEvaluator.isAdultDomain("video.pornhub.com"))
        assertTrue(AdultContentEvaluator.isAdultDomain("dmm.co.jp"))
        assertTrue(AdultContentEvaluator.isAdultDomain("sub.dmm.co.jp"))
    }

    @Test
    fun testLegitimateDomainsAllowed() {
        assertFalse(AdultContentEvaluator.isAdultDomain("google.com"))
        assertFalse(AdultContentEvaluator.isAdultDomain("github.com"))
        assertFalse(AdultContentEvaluator.isAdultDomain("wikipedia.org"))
        assertFalse(AdultContentEvaluator.isAdultDomain("nytimes.com"))
    }

    @Test
    fun testEvaluateUrlWithAdultDomain() {
        val result = AdultContentEvaluator.evaluateText("https://www.pornhub.com/view_video.php?viewkey=123")
        assertTrue(result is AdultContentEvaluator.EvaluationResult.Blocked)
        val blocked = result as AdultContentEvaluator.EvaluationResult.Blocked
        assertEquals(AdultContentEvaluator.BlockCategory.ADULT_DOMAIN, blocked.category)
    }

    @Test
    fun testEvaluateExplicitSearchQuery() {
        val result = AdultContentEvaluator.evaluateText("https://www.google.com/search?q=free+porn+hd")
        assertTrue(result is AdultContentEvaluator.EvaluationResult.Blocked)
        val blocked = result as AdultContentEvaluator.EvaluationResult.Blocked
        assertEquals(AdultContentEvaluator.BlockCategory.EXPLICIT_SEARCH, blocked.category)
    }

    @Test
    fun openingPrivateWindowsAndOrdinaryBrowserChromeIsAllowed() {
        for (text in listOf("You've gone Incognito - Chrome", "New private tab", "Search or enter address", "about:blank"))
            assertEquals(text, AdultContentEvaluator.EvaluationResult.Clean, AdultContentEvaluator.evaluateText(text))
    }

    @Test fun explicitSearchWordsAndEncodedQueriesAreInterrupted() {
        for (text in listOf("sex", "sexual videos", "porn", "free_porn", "https://google.com/search?q=sex",
            "https://bing.com/search?q=%2570%256f%2572%256e", "https://example.com/?q=porn", "सेक्स"))
            assertTrue(text, AdultContentEvaluator.evaluateText(text) is AdultContentEvaluator.EvaluationResult.Blocked)
    }

    @Test fun wordBoundariesAndDomainSuffixesDoNotBlockOrdinarySearches() {
        for (text in listOf("Sussex university", "Essex weather", "Middlesex", "https://example.com", "https://notpornhub.com"))
            assertEquals(text, AdultContentEvaluator.EvaluationResult.Clean, AdultContentEvaluator.evaluateText(text))
    }

    @Test fun operaVariantsAndOtherBrowserPackagesAreRecognized() {
        for (pkg in listOf("com.opera.gx", "com.opera.mini.native.beta", "com.opera.touch", "com.opera.future", "com.chrome.canary", "org.mozilla.firefox_beta"))
            assertTrue(pkg, AdultContentEvaluator.isBrowserPackage(pkg))
    }

    @Test
    fun testEvaluateCleanWebpageAllowed() {
        val result = AdultContentEvaluator.evaluateText("https://developer.android.com/jetpack/compose")
        assertEquals(AdultContentEvaluator.EvaluationResult.Clean, result)
    }

    @Test
    fun testBrowserPackageRecognition() {
        assertTrue(AdultContentEvaluator.isBrowserPackage("com.android.chrome"))
        assertTrue(AdultContentEvaluator.isBrowserPackage("org.mozilla.firefox"))
        assertTrue(AdultContentEvaluator.isBrowserPackage("com.sec.android.app.sbrowser"))
        assertTrue(AdultContentEvaluator.isBrowserPackage("com.duckduckgo.mobile.android"))
        assertFalse(AdultContentEvaluator.isBrowserPackage("com.google.android.calculator"))
    }
}
