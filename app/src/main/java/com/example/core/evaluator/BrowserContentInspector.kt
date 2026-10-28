package com.example.core.evaluator

import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale

/** Restricts inspection to visible browser chrome/search inputs. Page copy is never scored. */
internal object BrowserContentInspector {
    fun isAddressOrSearchField(node: AccessibilityNodeInfo): Boolean {
        if (node.isPassword || !node.isVisibleToUser) return false
        val id = node.viewIdResourceName.orEmpty().substringAfter(":id/").lowercase(Locale.ROOT)
        val desc = listOf(node.contentDescription, node.hintText).filterNotNull()
            .joinToString(" ").lowercase(Locale.ROOT)
        val urlId = listOf("url_bar", "url_field", "address_bar", "addressbar", "location_bar",
            "toolbar_url", "omnibox", "omnibar", "url_input", "url_edit", "locationbar").any(id::contains)
        val searchId = node.isEditable && listOf("search", "query", "edit_query", "search_box").any(id::contains)
        val described = node.isEditable && listOf("search or", "search the web", "web address", "address bar",
            "search query", "search here", "search with", "search in", "search", "खोजें", "पता").any(desc::contains)
        return urlId || searchId || described
    }

    fun inspect(root: AccessibilityNodeInfo, browserPackage: String): AdultContentEvaluator.EvaluationResult {
        if (root.packageName?.toString() != browserPackage) return AdultContentEvaluator.EvaluationResult.Clean
        val pending = ArrayDeque<AccessibilityNodeInfo>()
        pending.add(root)
        var remaining = 200
        try {
            while (pending.isNotEmpty() && remaining-- > 0) {
                val node = pending.removeFirst()
                try {
                    if (node.isPassword || !node.isVisibleToUser) continue
                    if (node.packageName?.toString()?.let { it != browserPackage } == true) continue
                    if (isAddressOrSearchField(node)) {
                        val result = AdultContentEvaluator.evaluateText(node.text?.toString().orEmpty())
                        if (result is AdultContentEvaluator.EvaluationResult.Blocked) return result
                    }
                    for (i in 0 until node.childCount.coerceAtMost(80)) {
                        runCatching { node.getChild(i) }.getOrNull()?.let(pending::addLast)
                    }
                } finally {
                    if (node !== root) { @Suppress("DEPRECATION") node.recycle() }
                }
            }
        } finally {
            pending.forEach { if (it !== root) { @Suppress("DEPRECATION") it.recycle() } }
        }
        return AdultContentEvaluator.EvaluationResult.Clean
    }
}
