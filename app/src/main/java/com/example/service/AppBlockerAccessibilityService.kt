package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.core.evaluator.AdultContentEvaluator
import com.example.core.system.AlarmScheduler
import com.example.core.system.SafetyWhitelist
import com.example.data.repository.AppBlockerRepository
import com.example.ui.blocker.BlockerActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * High-performance, zero-latency accessibility service.
 * Enforces scheduled application blocking and real-time 18+ adult content protection.
 *
 * Privacy Note: Adult-content inspection is limited to browser windows and visible accessibility
 * text. Password fields and unrelated editable inputs are excluded from text inspection. No page
 * text is uploaded; blocked-content events are recorded locally.
 */
class AppBlockerAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var repository: AppBlockerRepository

    private var lastBlockedPackage: String? = null
    private var lastBlockTimestamp: Long = 0L
    private var lastInspectionAt = 0L

    private var lastAdultTriggerTimestamp: Long = 0L
    private var adultBlockedPackage: String? = null
    private var adultBlockedUntil: Long = 0L
    private var lastAdultReassertTimestamp: Long = 0L

    override fun onCreate() {
        super.onCreate()
        repository = AppBlockerRepository(applicationContext)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Enabling Accessibility is a strong signal that the user wants enforcement
        // active now. Rebuild the secondary protection paths immediately.
        AlarmScheduler.scheduleNextTransition(this)
        BlockerMonitoringService.start(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            eventType != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED &&
            eventType != AccessibilityEvent.TYPE_VIEW_FOCUSED &&
            eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            return
        }

        val packageName = event.packageName?.toString() ?: return

        // Skip system critical apps, launchers, and blocker itself
        if (SafetyWhitelist.isSystemCritical(this, packageName)) return

        val now = System.currentTimeMillis()

        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && now - lastInspectionAt < 120L) return
        lastInspectionAt = now
        // The framework recycles the original event after this callback. Own the asynchronous copy.
        @Suppress("DEPRECATION")
        val ownedEvent = AccessibilityEvent.obtain(event)
        serviceScope.launch {
            try {
            // If explicit content was just detected in this app, keep the offending app
            // out of view for the configured cooldown even if its next accessibility event
            // no longer exposes the original text.
            if (packageName == adultBlockedPackage && now < adultBlockedUntil) {
                if (now - lastAdultReassertTimestamp > 700L) {
                    lastAdultReassertTimestamp = now
                    reassertAdultBlock(packageName)
                }
                return@launch
            }

            // 1. Layer: 18+ Adult Content Protection across browsers and webviews
            val adultProtectionEnabled = repository.settings.isAdultProtectionEnabledFlow.first()
            if (adultProtectionEnabled) {
                if (now - lastAdultTriggerTimestamp > 400L) {
                    val isAdultBlocked = inspectForAdultContent(packageName, ownedEvent)
                    if (isAdultBlocked) {
                        lastAdultTriggerTimestamp = now
                        return@launch
                    }
                }
            }

            // 2. Layer: Scheduled Application Blocking
            if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                // Throttle repeated triggers within 400ms for the exact same package
                if (packageName == lastBlockedPackage && (now - lastBlockTimestamp) < 400L) {
                    return@launch
                }

                val protectionEnabled = repository.settings.isProtectionEnabledFlow.first()
                if (!protectionEnabled) return@launch

                val evaluation = repository.evaluateApp(packageName)
                if (evaluation.isBlocked) {
                    lastBlockedPackage = packageName
                    lastBlockTimestamp = now

                    val appLabel = try {
                        val appInfo = packageManager.getApplicationInfo(packageName, 0)
                        packageManager.getApplicationLabel(appInfo).toString()
                    } catch (_: Exception) {
                        packageName
                    }

                    // Record block event
                    repository.recordBlockEvent(packageName, appLabel, evaluation.reason)

                    // Remove the blocked app from view first. If a modern Android
                    // background-activity rule suppresses the custom blocker Activity,
                    // the enforcement still succeeds by returning the user Home.
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    delay(90)

                    val intent = Intent(this@AppBlockerAccessibilityService, BlockerActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                        putExtra(BlockerActivity.EXTRA_PACKAGE_NAME, packageName)
                        putExtra(BlockerActivity.EXTRA_APP_NAME, appLabel)
                        putExtra(BlockerActivity.EXTRA_NEXT_ALLOWED, evaluation.formattedNextAllowedTime)
                        putExtra(BlockerActivity.EXTRA_BLOCK_TYPE, BlockerActivity.BLOCK_TYPE_SCHEDULE)
                    }
                    runCatching { startActivity(intent) }
                }
            }
            } finally {
                @Suppress("DEPRECATION")
                ownedEvent.recycle()
            }
        }
    }

    /**
     * Inspects browser address bar / active window nodes to detect explicit adult content or incognito tabs.
     * Returns true if adult content was detected and blocked.
     */
    private suspend fun inspectForAdultContent(packageName: String, event: AccessibilityEvent): Boolean {
        // Do not inspect accessibility text from messaging, banking, or non-browser apps.
        // This guard runs before reading event.text or contentDescription.
        val isBrowser = isBrowserApp(packageName)
        if (!isBrowser) return false

        val blockIncognito = repository.settings.isBlockIncognitoEnabledFlow.first()
        if (blockIncognito && AdultContentEvaluator.isPrivateOnlyBrowser(packageName)) {
            triggerAdultBlock(packageName, "Private-only browser", "Private browsing guard")
            return true
        }

        // Accessibility text-change events can originate from login, comment, or chat
        // fields inside a browser. Do not evaluate their contents as URL/search text.
        val eventSource = try { event.source } catch (_: Exception) { null }
        val skipImmediateText = eventSource != null &&
            (eventSource.isPassword ||
                (eventSource.isEditable && !isLikelyAddressSearchField(eventSource)))

        if (!skipImmediateText) {
            // Immediate browser event text is checked conservatively; strict URL checks
            // are reserved for fields with address/search metadata below.
            event.text?.forEach { charSeq ->
                val text = charSeq.toString()
                if (text.isNotBlank()) {
                    val eval = AdultContentEvaluator.evaluateVisibleContext(text)
                    if (eval is AdultContentEvaluator.EvaluationResult.Blocked) {
                        if (eval.category != AdultContentEvaluator.BlockCategory.INCOGNITO_RESTRICTED || blockIncognito) {
                            triggerAdultBlock(packageName, eval.category.label, eval.layer)
                            return true
                        }
                    }
                }
            }

            val eventDesc = event.contentDescription?.toString()
            if (!eventDesc.isNullOrBlank()) {
                val eval = AdultContentEvaluator.evaluateVisibleContext(eventDesc)
                if (eval is AdultContentEvaluator.EvaluationResult.Blocked) {
                    if (eval.category != AdultContentEvaluator.BlockCategory.INCOGNITO_RESTRICTED || blockIncognito) {
                        triggerAdultBlock(packageName, eval.category.label, eval.layer)
                        return true
                    }
                }
            }
        }

        val rootNode = try {
            rootInActiveWindow ?: event.source
        } catch (_: Exception) {
            null
        } ?: return false

        if (rootNode.packageName?.toString() != packageName) return false

        // Known browser address bar and omnibox resource IDs
        val addressBarIds = listOf(
            "com.android.chrome:id/url_bar",
            "com.sec.android.app.sbrowser:id/location_bar_edit_text",
            "org.mozilla.firefox:id/mozac_browser_toolbar_url_view",
            "com.microsoft.emmx:id/url_bar",
            "com.brave.browser:id/url_bar",
            "com.opera.browser:id/url_field",
            "com.duckduckgo.mobile.android:id/omnibar_text_input"
        )

        // 2. Check known URL input nodes only for browsers
        if (isBrowser) for (id in addressBarIds) {
            val nodes = rootNode.findAccessibilityNodeInfosByViewId(id)
            if (!nodes.isNullOrEmpty()) {
                for (node in nodes) {
                    val text = node.text?.toString() ?: ""
                    if (text.isNotBlank()) {
                        val eval = AdultContentEvaluator.evaluateText(text)
                        if (eval is AdultContentEvaluator.EvaluationResult.Blocked) {
                            if (eval.category == AdultContentEvaluator.BlockCategory.INCOGNITO_RESTRICTED && !blockIncognito) {
                                continue
                            }
                            triggerAdultBlock(packageName, eval.category.label, eval.layer)
                            return true
                        }
                    }
                }
            }
        }

        // 2b. Unknown/new browsers may use different resource IDs. Inspect editable
        // fields as URL/search candidates so they cannot bypass protection just because
        // their package or toolbar ID is not in our curated list.
        if (isBrowser) {
            val editableNodes = ArrayDeque<AccessibilityNodeInfo>()
            editableNodes.add(rootNode)
            var inspectedEditableCandidates = 0
            while (editableNodes.isNotEmpty() && inspectedEditableCandidates < 160) {
                val node = editableNodes.removeFirst()
                inspectedEditableCandidates++
                if (blockIncognito && !node.isPassword && !node.isEditable &&
                    (AdultContentEvaluator.isActivePrivateIndicator(node.text?.toString().orEmpty(), node.isSelected || node.isChecked) ||
                     AdultContentEvaluator.isActivePrivateIndicator(node.contentDescription?.toString().orEmpty(), node.isSelected || node.isChecked))) {
                    triggerAdultBlock(packageName, "Private browsing window", "Private browsing guard")
                    return true
                }
                val eval = evaluateEditableBrowserNode(node)
                if (eval is AdultContentEvaluator.EvaluationResult.Blocked) {
                    if (eval.category != AdultContentEvaluator.BlockCategory.INCOGNITO_RESTRICTED || blockIncognito) {
                        triggerAdultBlock(packageName, eval.category.label, eval.layer)
                        return true
                    }
                }
                for (i in 0 until node.childCount) {
                    try { node.getChild(i)?.let(editableNodes::addLast) } catch (_: Exception) { }
                }
            }
        }

        // 3. Scan the active hierarchy more deeply. Some modern browsers and WebViews
        // expose the address bar/page text only after focus, scroll or text-change events.
        val windowTextBuilder = StringBuilder()
        collectNodeText(rootNode, windowTextBuilder, maxDepth = 12, maxNodes = intArrayOf(240))

        // If multiple interactive windows exist (browser sheets, custom tabs, WebViews),
        // inspect those too instead of assuming rootInActiveWindow contains everything.
        try {
            windows.forEach { window ->
                val root = window.root ?: return@forEach
                if (root != rootNode && root.packageName?.toString() == packageName) {
                    collectNodeText(root, windowTextBuilder, maxDepth = 10, maxNodes = intArrayOf(120))
                }
            }
        } catch (_: Exception) {
            // Best-effort only; the active root above is still inspected.
        }

        val combinedText = windowTextBuilder.toString()
        if (combinedText.isNotBlank()) {
            // Full page/window text uses conservative context scoring. Strict keyword/domain
            // matching is reserved for the actual address/search field.
            val eval = AdultContentEvaluator.evaluateVisibleContext(combinedText)
            if (eval is AdultContentEvaluator.EvaluationResult.Blocked) {
                if (eval.category == AdultContentEvaluator.BlockCategory.INCOGNITO_RESTRICTED && !blockIncognito) {
                    return false
                }
                triggerAdultBlock(packageName, eval.category.label, eval.layer)
                return true
            }
        }

        return false
    }

    private fun isBrowserApp(packageName: String): Boolean {
        if (AdultContentEvaluator.isBrowserPackage(packageName)) return true

        // Detect installed third-party browsers by capability instead of relying only on
        // a package-name list. This closes the easy "install another browser" bypass.
        return try {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")).addCategory(Intent.CATEGORY_BROWSABLE)
            packageManager.queryIntentActivities(webIntent, PackageManager.MATCH_ALL)
                .any { it.activityInfo?.packageName == packageName }
        } catch (_: Exception) {
            false
        }
    }

    private fun evaluateEditableBrowserNode(node: AccessibilityNodeInfo?): AdultContentEvaluator.EvaluationResult {
        if (node == null || node.isPassword) {
            return AdultContentEvaluator.EvaluationResult.Clean
        }

        if (!isLikelyAddressSearchField(node)) return AdultContentEvaluator.EvaluationResult.Clean

        val value = node.text?.toString().orEmpty()
        if (value.isBlank()) return AdultContentEvaluator.EvaluationResult.Clean
        return AdultContentEvaluator.evaluateText(value)
    }

    private fun isLikelyAddressSearchField(node: AccessibilityNodeInfo): Boolean {
        val metadata = listOf(
            node.viewIdResourceName,
            node.contentDescription?.toString(),
            node.hintText?.toString()
        ).filterNotNull().joinToString(" ").lowercase(Locale.ROOT)
        return listOf("url", "address", "omnibox", "omnibar", "location_bar", "location bar", "search", "website", "url_field")
            .any(metadata::contains)
    }

    private fun collectNodeText(node: AccessibilityNodeInfo?, out: StringBuilder, maxDepth: Int, maxNodes: IntArray) {
        if (node == null || maxDepth <= 0 || maxNodes[0] <= 0) return
        maxNodes[0]--

        // Never read or recurse into password fields while scanning visible browser context.
        if (node.isPassword) return

        // Address/search inputs are evaluated separately. Do not include other editable
        // values such as login names or chat drafts in the page-context text scan.
        if (!node.isEditable) {
            val text = node.text?.toString()
            if (!text.isNullOrBlank()) out.append(" ").append(text)
            val desc = node.contentDescription?.toString()
            if (!desc.isNullOrBlank()) out.append(" ").append(desc)
        }

        for (i in 0 until node.childCount) {
            if (maxNodes[0] <= 0) break
            val child = try { node.getChild(i) } catch (_: Exception) { null }
            collectNodeText(child, out, maxDepth - 1, maxNodes)
        }
    }

    private suspend fun triggerAdultBlock(packageName: String, reason: String, layer: String) {
        val appLabel = try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            "Web Browser"
        }

        // Record adult block event in database and increment count
        repository.recordAdultBlockEvent("18+ Content Blocked ($reason)")

        // Remove offending content from view first. Android does not allow a normal app to force-stop arbitrary apps.
        performGlobalAction(GLOBAL_ACTION_HOME)
        // Let HOME finish before presenting the blocker. Starting both in the same instant
        // can race on some OEM builds and leave the browser visible.
        delay(120)

        val cooldownSeconds = repository.settings.adultCooldownSecondsFlow.first()
        adultBlockedPackage = packageName
        adultBlockedUntil = System.currentTimeMillis() + cooldownSeconds * 1000L

        // Launch clean neutral 18+ Block Screen (single top to prevent stacked duplicates)
        val intent = Intent(this, BlockerActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(BlockerActivity.EXTRA_PACKAGE_NAME, packageName)
            putExtra(BlockerActivity.EXTRA_APP_NAME, appLabel)
            putExtra(BlockerActivity.EXTRA_BLOCK_TYPE, BlockerActivity.BLOCK_TYPE_ADULT)
            putExtra(BlockerActivity.EXTRA_ADULT_LAYER, layer)
            putExtra(BlockerActivity.EXTRA_ADULT_REASON, reason)
            putExtra(BlockerActivity.EXTRA_ADULT_COOLDOWN_SECONDS, cooldownSeconds)
        }
        runCatching { startActivity(intent) }
    }

    private suspend fun reassertAdultBlock(packageName: String) {
        val remainingSeconds = ((adultBlockedUntil - System.currentTimeMillis()) / 1000L)
            .coerceAtLeast(1L)
            .toInt()
        val appLabel = try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            "Restricted app"
        }

        performGlobalAction(GLOBAL_ACTION_HOME)
        delay(100)
        val intent = Intent(this, BlockerActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(BlockerActivity.EXTRA_PACKAGE_NAME, packageName)
            putExtra(BlockerActivity.EXTRA_APP_NAME, appLabel)
            putExtra(BlockerActivity.EXTRA_BLOCK_TYPE, BlockerActivity.BLOCK_TYPE_ADULT)
            putExtra(BlockerActivity.EXTRA_ADULT_LAYER, "Protection cooldown")
            putExtra(BlockerActivity.EXTRA_ADULT_REASON, "Restricted content was recently detected")
            putExtra(BlockerActivity.EXTRA_ADULT_COOLDOWN_SECONDS, remainingSeconds)
        }
        runCatching { startActivity(intent) }
    }

    override fun onInterrupt() {
        // Required by AccessibilityService
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
