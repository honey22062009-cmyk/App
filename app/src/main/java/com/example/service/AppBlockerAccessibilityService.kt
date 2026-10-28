package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityEvent
import com.example.core.evaluator.BrowserContentInspector
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

/**
 * User-enabled foreground app and explicit-search guard.
 * Enforces scheduled application blocking and real-time 18+ adult content protection.
 *
 * Privacy Note: Adult-content inspection is limited to browser windows and visible accessibility
 * URL/search fields. Password fields and unrelated editable inputs are excluded. No browser
 * text is uploaded; blocked-content events are recorded locally.
 */
class AppBlockerAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var repository: AppBlockerRepository

    private var lastBlockedPackage: String? = null
    private var lastBlockTimestamp: Long = 0L
    private var lastInspectionAt = 0L

    private var lastAdultTriggerTimestamp: Long = 0L

    override fun onCreate() {
        super.onCreate()
        repository = AppBlockerRepository(applicationContext)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        BrowserGuardStatus.connected.value = true
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

        val now = android.os.SystemClock.elapsedRealtime()

        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && now - lastInspectionAt < 120L) return
        lastInspectionAt = now
        // Retain only the immutable package/type; do not hold framework events across suspension.
        serviceScope.launch {
            try {
            // 1. Layer: 18+ Adult Content Protection across browsers and webviews
            val adultProtectionEnabled = repository.settings.isAdultProtectionEnabledFlow.first()
            if (adultProtectionEnabled && repository.settings.browserInspectionConsentFlow.first()) {
                if (now - lastAdultTriggerTimestamp > 1200L) {
                    val isAdultBlocked = inspectForAdultContent(packageName)
                    if (isAdultBlocked) return@launch
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
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                // Browser UI nodes can disappear during navigation. A stale node must
                // not crash the protection process. Never log inspected page text.
                android.util.Log.w("StrictBlocker", "Window inspection could not complete", error)
            }
        }
    }

    /** A browser opening, private-mode label or page paragraph can never trigger this guard. */
    private suspend fun inspectForAdultContent(packageName: String): Boolean {
        if (!isBrowserApp(packageName)) return false
        val root = runCatching { rootInActiveWindow }.getOrNull() ?: return false
        if (root.packageName?.toString() != packageName) return false
        BrowserGuardStatus.inspected()
        val result = try { BrowserContentInspector.inspect(root, packageName) }
            finally { @Suppress("DEPRECATION") root.recycle() }
        if (result is AdultContentEvaluator.EvaluationResult.Blocked) {
            // Reserve this trigger before a database write can suspend; coalesce follow-up events.
            lastAdultTriggerTimestamp = android.os.SystemClock.elapsedRealtime()
            triggerAdultBlock(packageName, result.category.label, result.layer)
            return true
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

    private suspend fun triggerAdultBlock(packageName: String, reason: String, layer: String) {
        val appLabel = try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            "Web Browser"
        }

        // Remove offending content from view first. Android does not allow a normal app to force-stop arbitrary apps.
        performGlobalAction(GLOBAL_ACTION_HOME)
        repository.recordAdultBlockEvent("18+ Content Blocked ($reason)")
        // Let HOME finish before presenting the blocker. Starting both in the same instant
        // can race on some OEM builds and leave the browser visible.
        delay(120)

        val cooldownSeconds = repository.settings.adultCooldownSecondsFlow.first()
        BrowserGuardStatus.blocked()

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

    override fun onInterrupt() {
        // Required by AccessibilityService
    }

    override fun onDestroy() {
        super.onDestroy()
        BrowserGuardStatus.connected.value = false
        serviceScope.cancel()
    }
}
