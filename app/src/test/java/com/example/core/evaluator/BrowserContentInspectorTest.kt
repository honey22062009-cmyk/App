package com.example.core.evaluator

import android.app.Application
import android.view.accessibility.AccessibilityNodeInfo
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class BrowserContentInspectorTest {
    @Suppress("DEPRECATION")
    private fun field(id: String?, text: String, editable: Boolean = false, visible: Boolean = true, password: Boolean = false) =
        AccessibilityNodeInfo.obtain().apply {
            packageName = "com.android.chrome"; viewIdResourceName = id; this.text = text
            isEditable = editable; isVisibleToUser = visible; isPassword = password
        }
    @Test fun readsCollapsedAndEditableAddressFields() {
        for (editable in listOf(true, false)) {
            val node = field("com.android.chrome:id/url_bar", "https://example.com/?q=porn", editable)
            assertTrue(BrowserContentInspector.inspect(node, "com.android.chrome") is AdultContentEvaluator.EvaluationResult.Blocked)
        }
    }
    @Test fun ignoresPageCopyMenusAndPrivateModeLabels() {
        for (text in listOf("porn sex sexual", "You've gone incognito", "New private tab"))
            assertEquals(AdultContentEvaluator.EvaluationResult.Clean,
                BrowserContentInspector.inspect(field("com.android.chrome:id/page_title", text), "com.android.chrome"))
    }
    @Test fun doesNotReadPasswordsOtherFormsHiddenFieldsOrOtherPackages() {
        val cases = listOf(field("chrome:id/password", "porn", true, password = true),
            field("chrome:id/comment", "porn", true), field("chrome:id/url_bar", "porn", visible = false),
            field("chrome:id/search_box", "porn", editable = false))
        for (node in cases) assertEquals(AdultContentEvaluator.EvaluationResult.Clean, BrowserContentInspector.inspect(node, "com.android.chrome"))
        assertEquals(AdultContentEvaluator.EvaluationResult.Clean,
            BrowserContentInspector.inspect(field("chrome:id/url_bar", "porn"), "com.opera.browser"))
    }
    @Test fun visibleSearchInputsAreGuardedAndNormalBrowsingIsAllowed() {
        val search = field("com.opera.gx:id/search_box", "sex", editable = true)
        assertTrue(BrowserContentInspector.inspect(search, "com.android.chrome") is AdultContentEvaluator.EvaluationResult.Blocked)
        val clean = field("chrome:id/url_bar", "https://www.google.com/search?q=physics")
        assertEquals(AdultContentEvaluator.EvaluationResult.Clean, BrowserContentInspector.inspect(clean, "com.android.chrome"))
    }
}
