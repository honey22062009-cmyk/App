package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.core.evaluator.AdultContentEvaluator
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

/**
 * High-performance, zero-latency accessibility service.
 * Enforces scheduled application blocking and real-time 18+ adult content protection.
 *
 * Privacy Note: Inspects only browser address bar nodes and explicit URL strings.
 * Never logs, records, or transmits passwords, personal chats, banking info, or private messages.
 */
class AppBlockerAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var repository: AppBlockerRepository

    private var lastBlockedPackage: String? = null
    private var lastBlockTimestamp: Long = 0L

    private var lastAdultTriggerTimestamp: Long = 0L

    override fun onCreate() {
        super.onCreate()
        repository = AppBlockerRepository(applicationContext)
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

        serviceScope.launch {
            // 1. Layer: 18+ Adult Content Protection across browsers and webviews
            val adultProtectionEnabled = repository.settings.isAdultProtectionEnabledFlow.first()
            if (adultProtectionEnabled) {
                if (now - lastAdultTriggerTimestamp > 400L) {
                    val isAdultBlocked = inspectForAdultContent(packageName, event)
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

                    // Launch Fullscreen Shield Activity
                    val intent = Intent(this@AppBlockerAccessibilityService, BlockerActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                        putExtra(BlockerActivity.EXTRA_PACKAGE_NAME, packageName)
                        putExtra(BlockerActivity.EXTRA_APP_NAME, appLabel)
                        putExtra(BlockerActivity.EXTRA_NEXT_ALLOWED, evaluation.formattedNextAllowedTime)
                        putExtra(BlockerActivity.EXTRA_BLOCK_TYPE, BlockerActivity.BLOCK_TYPE_SCHEDULE)
                    }
                    startActivity(intent)
                }
            }
        }
    }

    /**
     * Inspects browser address bar / active window nodes to detect explicit adult content or incognito tabs.
     * Returns true if adult content was detected and blocked.
     */
    private suspend fun inspectForAdultContent(packageName: String, event: AccessibilityEvent): Boolean {
        val isBrowser = AdultContentEvaluator.isBrowserPackage(packageName)
        val blockIncognito = repository.settings.isBlockIncognitoEnabledFlow.first()

        // 1. Check immediate event text and content descriptions (fastest 0ms detection)
        event.text?.forEach { charSeq ->
            val text = charSeq.toString()
            if (text.isNotBlank()) {
                val eval = if (isBrowser) AdultContentEvaluator.evaluateText(text) else AdultContentEvaluator.evaluateVisibleContext(text)
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
            val eval = if (isBrowser) AdultContentEvaluator.evaluateText(eventDesc) else AdultContentEvaluator.evaluateVisibleContext(eventDesc)
            if (eval is AdultContentEvaluator.EvaluationResult.Blocked) {
                if (eval.category != AdultContentEvaluator.BlockCategory.INCOGNITO_RESTRICTED || blockIncognito) {
                    triggerAdultBlock(packageName, eval.category.label, eval.layer)
                    return true
                }
            }
        }

        val rootNode = try {
            rootInActiveWindow ?: event.source
        } catch (_: Exception) {
            null
        } ?: return false

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

        // 3. Scan the active hierarchy more deeply. Some modern browsers and WebViews
        // expose the address bar/page text only after focus, scroll or text-change events.
        val windowTextBuilder = StringBuilder()
        collectNodeText(rootNode, windowTextBuilder, maxDepth = 12, maxNodes = intArrayOf(240))

        // If multiple interactive windows exist (browser sheets, custom tabs, WebViews),
        // inspect those too instead of assuming rootInActiveWindow contains everything.
        try {
            windows.forEach { window ->
                val root = window.root ?: return@forEach
                if (root != rootNode) {
                    collectNodeText(root, windowTextBuilder, maxDepth = 10, maxNodes = intArrayOf(120))
                }
            }
        } catch (_: Exception) {
            // Best-effort only; the active root above is still inspected.
        }

        val combinedText = windowTextBuilder.toString()
        if (combinedText.isNotBlank()) {
            val eval = if (isBrowser) {
                AdultContentEvaluator.evaluateText(combinedText)
            } else {
                AdultContentEvaluator.evaluateVisibleContext(combinedText)
            }
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

    private fun collectNodeText(node: AccessibilityNodeInfo?, out: StringBuilder, maxDepth: Int, maxNodes: IntArray) {
        if (node == null || maxDepth <= 0 || maxNodes[0] <= 0) return
        maxNodes[0]--

        val text = node.text?.toString()
        if (!text.isNullOrBlank()) {
            out.append(" ").append(text)
        }
        val desc = node.contentDescription?.toString()
        if (!desc.isNullOrBlank()) {
            out.append(" ").append(desc)
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
        startActivity(intent)
    }

    override fun onInterrupt() {
        // Required by AccessibilityService
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
