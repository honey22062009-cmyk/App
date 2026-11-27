package com.example.core.evaluator

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale

/**
 * Reads only browser chrome and editable address/search controls. It never scores
 * arbitrary page paragraphs. Node text is processed locally and is not logged.
 */
internal object BrowserContentInspector {
    private const val MAX_NODES = 240
    private const val MAX_CHILDREN_PER_NODE = 64
    private const val CHROME_HEIGHT_FRACTION = 0.20f

    // Resource-ID fragments observed across common browser families. These are
    // hints, not claims that a particular ID exists on every installed version.
    private val knownAddressIdFragments = listOf(
        "url_bar", "url_field", "address_bar", "addressbar", "location_bar",
        "toolbar_url", "omnibox", "omnibar", "url_input", "url_edit", "locationbar"
    )
    private val searchIdFragments = listOf("search", "query", "edit_query", "search_box")
    private val searchDescriptions = listOf(
        "search or", "search the web", "web address", "address bar", "search query",
        "search here", "search with", "search in", "search", "खोजें", "पता"
    )

    private val privateChromeLabels = setOf(
        "incognito", "incognito mode", "incognito tab", "incognito tabs",
        "you are in incognito", "you're in incognito", "you have gone incognito",
        "you've gone incognito", "private browsing", "private browsing mode",
        "private tab", "private tabs", "private mode", "you are browsing privately",
        "secret mode", "गुप्त मोड", "निजी टैब", "निजी ब्राउज़िंग"
    )

    fun isAddressOrSearchField(node: AccessibilityNodeInfo): Boolean {
        if (node.isPassword || !node.isVisibleToUser) return false
        val id = node.viewIdResourceName.orEmpty().substringAfter(":id/").lowercase(Locale.ROOT)
        val description = listOfNotNull(node.contentDescription?.toString(), node.hintText?.toString())
            .joinToString(" ").lowercase(Locale.ROOT)
        val knownUrlId = knownAddressIdFragments.any(id::contains)
        val genericSearchId = node.isEditable && searchIdFragments.any(id::contains)
        val describedAddress = node.isEditable && searchDescriptions.any(description::contains)
        return knownUrlId || genericSearchId || describedAddress
    }

    /**
     * Detects private mode only from non-editable, visible browser chrome in the
     * upper 20% of the active window. Typed search text is intentionally excluded.
     */
    fun detectPrivateMode(root: AccessibilityNodeInfo, browserPackage: String): Boolean {
        if (root.packageName?.toString() != browserPackage) return false
        val rootBounds = Rect()
        root.getBoundsInScreen(rootBounds)
        if (rootBounds.isEmpty) return false
        val chromeBottom = rootBounds.top + (rootBounds.height() * CHROME_HEIGHT_FRACTION).toInt()

        val pending = ArrayDeque<AccessibilityNodeInfo>()
        pending.add(root)
        var remaining = MAX_NODES
        try {
            while (pending.isNotEmpty() && remaining-- > 0) {
                val node = pending.removeFirst()
                try {
                    if (!node.isVisibleToUser || node.isPassword) continue
                    if (node.packageName?.toString()?.let { it != browserPackage } == true) continue

                    if (!node.isEditable && isInBrowserChrome(node, rootBounds, chromeBottom)) {
                        val label = listOfNotNull(
                            node.text?.toString(),
                            node.contentDescription?.toString()
                        ).map(::normalizeChromeLabel)
                        if (label.any(::isPrivateModeLabel)) return true
                    }
                    enqueueChildren(node, pending)
                } finally {
                    if (node !== root) {
                        @Suppress("DEPRECATION")
                        node.recycle()
                    }
                }
            }
        } finally {
            recyclePending(pending, root)
        }
        return false
    }

    fun inspect(
        root: AccessibilityNodeInfo,
        browserPackage: String,
        windowTitle: String? = null,
        allowGenericHostScan: Boolean = true
    ): AdultContentEvaluator.EvaluationResult {
        if (root.packageName?.toString() != browserPackage) return AdultContentEvaluator.EvaluationResult.Clean

        val rootBounds = Rect()
        root.getBoundsInScreen(rootBounds)
        val chromeBottom = if (rootBounds.isEmpty) Int.MIN_VALUE
            else rootBounds.top + (rootBounds.height() * CHROME_HEIGHT_FRACTION).toInt()

        var genericHostCandidate: String? = null
        val pending = ArrayDeque<AccessibilityNodeInfo>()
        pending.add(root)
        var remaining = MAX_NODES
        try {
            while (pending.isNotEmpty() && remaining-- > 0) {
                val node = pending.removeFirst()
                try {
                    if (!node.isVisibleToUser || node.isPassword) continue
                    if (node.packageName?.toString()?.let { it != browserPackage } == true) continue

                    // Layer 1: known address/search resource IDs and accessible hints.
                    if (isAddressOrSearchField(node)) {
                        val value = node.text?.toString().orEmpty()
                        val result = AdultContentEvaluator.evaluateText(value)
                        if (result is AdultContentEvaluator.EvaluationResult.Blocked) return result
                    }

                    // Layer 2: top 20% of the browser window, strict URL/host parser only.
                    if (allowGenericHostScan && genericHostCandidate == null &&
                        !node.isEditable && !rootBounds.isEmpty &&
                        isInBrowserChrome(node, rootBounds, chromeBottom)
                    ) {
                        val candidates = listOfNotNull(
                            node.text?.toString(),
                            node.contentDescription?.toString()
                        )
                        genericHostCandidate = candidates.firstNotNullOfOrNull(::parseDomainCandidate)
                    }
                    enqueueChildren(node, pending)
                } finally {
                    if (node !== root) {
                        @Suppress("DEPRECATION")
                        node.recycle()
                    }
                }
            }
        } finally {
            recyclePending(pending, root)
        }

        if (genericHostCandidate != null) {
            val result = AdultContentEvaluator.evaluateText(genericHostCandidate)
            if (result is AdultContentEvaluator.EvaluationResult.Blocked) return result
        }

        // Layer 3: window title as a last resort, but only when it is itself a host.
        val titleHost = windowTitle?.let(::parseDomainCandidate)
        if (titleHost != null) {
            return AdultContentEvaluator.evaluateText(titleHost)
        }
        return AdultContentEvaluator.EvaluationResult.Clean
    }

    private fun isInBrowserChrome(node: AccessibilityNodeInfo, root: Rect, chromeBottom: Int): Boolean {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (bounds.isEmpty) return false
        return bounds.top >= root.top - 2 &&
            bounds.top <= chromeBottom &&
            bounds.bottom <= chromeBottom + root.height() / 30
    }

    private fun parseDomainCandidate(raw: String): String? {
        val value = AdultContentEvaluator.normalizeForMatching(raw).trim().trimEnd('.', ',')
        if (value.isBlank() || value.length > 300 || value.any(Char::isWhitespace)) return null
        if (value.startsWith("http://") || value.startsWith("https://")) {
            val candidate = AdultContentEvaluator.extractDomain(value)
            return candidate.takeIf(::looksLikeHost)
        }
        val candidate = value.substringBefore('/').substringBefore('?').substringBefore('#')
        return candidate.takeIf(::looksLikeHost)
    }

    private fun looksLikeHost(value: String): Boolean {
        if (value.length !in 4..253 || value.startsWith(".") || value.endsWith(".")) return false
        if (!value.contains('.')) return false
        return value.split('.').all { label ->
            label.isNotEmpty() && label.length <= 63 &&
                label.first().isLetterOrDigit() && label.last().isLetterOrDigit() &&
                label.all { it.isLetterOrDigit() || it == '-' }
        } && value.substringAfterLast('.').length >= 2
    }

    private fun normalizeChromeLabel(value: String): String =
        value.lowercase(Locale.ROOT).replace(Regex("\\s+"), " ").trim().trimEnd(':', '·', '•')

    private fun isPrivateModeLabel(label: String): Boolean =
        label in privateChromeLabels ||
            label.startsWith("you're in incognito") ||
            label.startsWith("you are in incognito") ||
            label.startsWith("private browsing is on")

    private fun enqueueChildren(node: AccessibilityNodeInfo, pending: ArrayDeque<AccessibilityNodeInfo>) {
        for (index in 0 until node.childCount.coerceAtMost(MAX_CHILDREN_PER_NODE)) {
            runCatching { node.getChild(index) }.getOrNull()?.let(pending::addLast)
        }
    }

    private fun recyclePending(pending: ArrayDeque<AccessibilityNodeInfo>, root: AccessibilityNodeInfo) {
        pending.forEach { node ->
            if (node !== root) {
                @Suppress("DEPRECATION")
                node.recycle()
            }
        }
        pending.clear()
    }
}
