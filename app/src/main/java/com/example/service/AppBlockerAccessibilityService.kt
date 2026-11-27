package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.example.StrictBlockerApplication
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
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * User-enabled foreground app and browser-chrome guard.
 *
 * Browser inspection is local. Password fields and arbitrary page paragraphs are
 * excluded; only address/search fields and a strict top-chrome host fallback are read.
 */
class AppBlockerAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var repository: AppBlockerRepository

    private var lastBlockedPackage: String? = null
    private var lastBlockTimestamp: Long = 0L
    private var lastAdultTriggerTimestamp: Long = 0L
    private val lastContentChangedAt = ConcurrentHashMap<String, Long>()
    private val browserCapabilityCache = ConcurrentHashMap<String, Boolean>()

    private val packageChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            browserCapabilityCache.clear()
            SafetyWhitelist.invalidateCache()
        }
    }

    private var packageReceiverRegistered = false

    override fun onCreate() {
        super.onCreate()
        repository = (applicationContext as StrictBlockerApplication).container.repository

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(packageChangeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                registerReceiver(packageChangeReceiver, filter)
            }
            packageReceiverRegistered = true
        } catch (_: Exception) {
            packageReceiverRegistered = false
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        BrowserGuardStatus.connected.value = true
        serviceScope.launch(Dispatchers.Default) {
            SafetyWhitelist.preload(this@AppBlockerAccessibilityService)
        }
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
            eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED
        ) return

        val packageName = event.packageName?.toString() ?: return
        val isPlayStore = packageName == PLAY_STORE_PACKAGE
        val isSystemCritical = SafetyWhitelist.isSystemCritical(this, packageName)

        // Play Store stays exempt from app blocking, but its own search field can
        // still be inspected. Other system-critical packages are returned immediately.
        if (isSystemCritical && !isPlayStore) return

        val now = SystemClock.elapsedRealtime()
        if (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            val previous = lastContentChangedAt[packageName] ?: 0L
            if (now - previous < CONTENT_CHANGED_DEBOUNCE_MS) return
            lastContentChangedAt[packageName] = now
        }

        // Window title is only used later if it is itself a strict host candidate.
        val windowTitle = event.text?.joinToString(" ")?.take(256)
            ?: event.contentDescription?.toString()?.take(256)

        serviceScope.launch {
            try {
                val adultProtectionEnabled = repository.settings.isAdultProtectionEnabledFlow.first()
                val inspectionConsent = repository.settings.browserInspectionConsentFlow.first()
                if (adultProtectionEnabled && inspectionConsent &&
                    now - lastAdultTriggerTimestamp > ADULT_TRIGGER_COOLDOWN_MS
                ) {
                    val isAdultBlocked = inspectForAdultContent(
                        packageName = packageName,
                        windowTitle = windowTitle,
                        allowGenericHostScan = !isPlayStore
                    )
                    if (isAdultBlocked) return@launch
                }

                // The Play Store is never evaluated by the schedule blocker.
                if (isSystemCritical || eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return@launch

                if (packageName == lastBlockedPackage &&
                    now - lastBlockTimestamp < SCHEDULE_TRIGGER_DEBOUNCE_MS
                ) return@launch

                if (!repository.settings.isProtectionEnabledFlow.first()) return@launch
                val evaluation = repository.evaluateApp(packageName)
                if (!evaluation.isBlocked) return@launch

                lastBlockedPackage = packageName
                lastBlockTimestamp = now
                val appLabel = appLabel(packageName)
                repository.recordBlockEvent(packageName, appLabel, evaluation.reason)

                val intent = Intent(this@AppBlockerAccessibilityService, BlockerActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra(BlockerActivity.EXTRA_PACKAGE_NAME, packageName)
                    putExtra(BlockerActivity.EXTRA_APP_NAME, appLabel)
                    putExtra(BlockerActivity.EXTRA_NEXT_ALLOWED, evaluation.formattedNextAllowedTime)
                    putExtra(BlockerActivity.EXTRA_BLOCK_TYPE, BlockerActivity.BLOCK_TYPE_SCHEDULE)
                }
                // Launch the shield first to avoid the visible HOME flash. HOME is a
                // fallback only when Android rejects the direct activity launch.
                runCatching { startActivity(intent) }
                    .onFailure { performGlobalAction(GLOBAL_ACTION_HOME) }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.w("StrictBlocker", "Browser/app inspection could not complete", error)
            }
        }
    }

    private suspend fun inspectForAdultContent(
        packageName: String,
        windowTitle: String?,
        allowGenericHostScan: Boolean
    ): Boolean {
        val isStoreSearch = packageName == PLAY_STORE_PACKAGE
        val isBrowser = if (isStoreSearch) true else withContext(Dispatchers.Default) {
            isBrowserApp(packageName)
        }
        if (!isBrowser) return false

        val root = runCatching { rootInActiveWindow }.getOrNull() ?: return false
        if (root.packageName?.toString() != packageName) {
            @Suppress("DEPRECATION")
            root.recycle()
            return false
        }

        BrowserGuardStatus.inspected()
        val blockPrivateMode = !isStoreSearch &&
            repository.settings.blockIncognitoEnabledFlow.first()
        val result = try {
            withContext(Dispatchers.Default) {
                if (blockPrivateMode && BrowserContentInspector.detectPrivateMode(root, packageName)) {
                    AdultContentEvaluator.EvaluationResult.Blocked(
                        AdultContentEvaluator.BlockCategory.INCOGNITO_RESTRICTED,
                        "Private browsing guard"
                    )
                } else {
                    BrowserContentInspector.inspect(
                        root = root,
                        browserPackage = packageName,
                        windowTitle = windowTitle,
                        allowGenericHostScan = allowGenericHostScan
                    )
                }
            }
        } finally {
            @Suppress("DEPRECATION")
            root.recycle()
        }

        if (result is AdultContentEvaluator.EvaluationResult.Blocked) {
            lastAdultTriggerTimestamp = SystemClock.elapsedRealtime()
            triggerAdultBlock(packageName, result.category.label, result.layer)
            return true
        }
        return false
    }

    private fun isBrowserApp(packageName: String): Boolean {
        if (AdultContentEvaluator.isBrowserPackage(packageName)) return true
        return browserCapabilityCache.getOrPut(packageName) {
            try {
                val webIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://example.invalid/")
                ).addCategory(Intent.CATEGORY_BROWSABLE)
                packageManager.queryIntentActivities(webIntent, PackageManager.MATCH_ALL)
                    .any { it.activityInfo?.packageName == packageName }
            } catch (_: Exception) {
                false
            }
        }
    }

    private suspend fun triggerAdultBlock(packageName: String, reason: String, layer: String) {
        val appLabel = appLabel(packageName)
        repository.recordAdultBlockEvent("18+ Content Blocked ($reason)")
        val cooldownSeconds = repository.settings.adultCooldownSecondsFlow.first()
        BrowserGuardStatus.blocked()

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
            .onFailure { performGlobalAction(GLOBAL_ACTION_HOME) }
    }

    private fun appLabel(packageName: String): String =
        try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            "Web Browser"
        }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        super.onDestroy()
        BrowserGuardStatus.connected.value = false
        if (packageReceiverRegistered) runCatching { unregisterReceiver(packageChangeReceiver) }
        serviceScope.cancel()
    }

    private companion object {
        const val PLAY_STORE_PACKAGE = "com.android.vending"
        const val CONTENT_CHANGED_DEBOUNCE_MS = 400L
        const val SCHEDULE_TRIGGER_DEBOUNCE_MS = 400L
        const val ADULT_TRIGGER_COOLDOWN_MS = 1_200L
    }
}
