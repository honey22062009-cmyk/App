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
    fun testEvaluateIncognitoTitle() {
        val result = AdultContentEvaluator.evaluateText("You've gone Incognito - Chrome")
        assertTrue(result is AdultContentEvaluator.EvaluationResult.Blocked)
        val blocked = result as AdultContentEvaluator.EvaluationResult.Blocked
        assertEquals(AdultContentEvaluator.BlockCategory.INCOGNITO_RESTRICTED, blocked.category)
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
